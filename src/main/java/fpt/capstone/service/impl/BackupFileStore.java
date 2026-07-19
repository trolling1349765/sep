package fpt.capstone.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import fpt.capstone.enums.BackupType;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes and reads backup JSON files under ${app.backup.dir}.
 *
 * File layout: { "meta": { code, type, appVersion, createdAt, tables: {name: rowCount} },
 *                "data": { table: [ {column: value} ] } }.
 *
 * Values are normalized to JSON-native types on write (temporals become
 * strings) so that on restore the parsed values can be handed straight to
 * PreparedStatement.setObject — MySQL coerces the strings back into
 * DATETIME/DATE columns. The whitelist is deliberate: an unexpected JDBC type
 * (e.g. a future binary column) fails the backup loudly instead of writing a
 * file that cannot be restored.
 *
 * Uses a dedicated Jackson 2 ObjectMapper (not the Spring bean): the project
 * pins com.fasterxml jackson-databind alongside Boot 4's Jackson 3, so
 * injecting "the" ObjectMapper is ambiguous.
 */
@Component
public class BackupFileStore {

    // datetime(6) precision — a seconds-only pattern would silently truncate.
    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);

    @Value("${app.backup.dir:uploads/backups}")
    private String backupDir;

    @Getter
    private Path backupPath;

    @PostConstruct
    public void init() {
        backupPath = Paths.get(backupDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(backupPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create backup directory: " + backupPath, e);
        }
    }

    public record WrittenFile(Path path, long sizeBytes, String checksumSha256) {
    }

    public record ParsedBackup(Map<String, Object> meta, Map<String, List<Map<String, Object>>> data) {
    }

    /** Serializes and writes the backup, computing SHA-256 in the same pass. */
    public WrittenFile write(String code, BackupType type, String appVersion,
            LocalDateTime createdAt, Map<String, List<Map<String, Object>>> data) {
        Map<String, Object> tables = new LinkedHashMap<>();
        data.forEach((table, rows) -> tables.put(table, rows.size()));

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("code", code);
        meta.put("type", type.name());
        meta.put("appVersion", appVersion);
        meta.put("createdAt", createdAt.format(DATETIME));
        meta.put("tables", tables);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("meta", meta);
        root.put("data", data);

        Path file = backupPath.resolve(code + ".json");
        try {
            MessageDigest md = sha256Digest();
            try (OutputStream out = new DigestOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(file)), md)) {
                MAPPER.writeValue(out, root);
            }
            return new WrittenFile(file, Files.size(file), java.util.HexFormat.of().formatHex(md.digest()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write backup file " + file, e);
        }
    }

    public ParsedBackup read(Path file) {
        try {
            Map<String, Object> root = MAPPER.readValue(file.toFile(),
                    new TypeReference<LinkedHashMap<String, Object>>() {
                    });
            @SuppressWarnings("unchecked")
            Map<String, Object> meta = (Map<String, Object>) root.get("meta");
            @SuppressWarnings("unchecked")
            Map<String, List<Map<String, Object>>> data =
                    (Map<String, List<Map<String, Object>>>) root.get("data");
            if (meta == null || data == null) {
                throw new IllegalStateException("Backup file is missing meta/data sections: " + file);
            }
            return new ParsedBackup(meta, data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read backup file " + file, e);
        }
    }

    public String sha256(Path file) {
        try {
            MessageDigest md = sha256Digest();
            try (InputStream in = new DigestInputStream(
                    new BufferedInputStream(Files.newInputStream(file)), md)) {
                in.transferTo(OutputStream.nullOutputStream());
            }
            return java.util.HexFormat.of().formatHex(md.digest());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to checksum backup file " + file, e);
        }
    }

    /** Maps JDBC result values to JSON-native, restore-safe values. */
    public static Object normalize(Object value) {
        return switch (value) {
            case null -> null;
            case String s -> s;
            case Boolean b -> b;
            case Integer i -> i;
            case Long l -> l;
            case Short s -> s;
            case Byte b -> b;
            case BigDecimal d -> d;
            case BigInteger bi -> bi;
            case Double d -> d;
            case Float f -> f;
            case java.sql.Timestamp ts -> ts.toLocalDateTime().format(DATETIME);
            case LocalDateTime ldt -> ldt.format(DATETIME);
            case java.sql.Date d -> d.toLocalDate().toString();
            case LocalDate ld -> ld.toString();
            case java.sql.Time t -> t.toString();
            case byte[] ignored -> throw new IllegalStateException(
                    "Binary columns are not supported by backup");
            default -> throw new IllegalStateException(
                    "Unsupported JDBC type in backup: " + value.getClass().getName());
        };
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
