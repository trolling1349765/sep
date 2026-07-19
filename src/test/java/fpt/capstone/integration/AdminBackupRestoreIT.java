package fpt.capstone.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import fpt.capstone.entity.Backup;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.enums.BackupStatus;
import fpt.capstone.repository.BackupRepository;
import fpt.capstone.repository.SponsorRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end backup/restore cycle against the shared Testcontainers MySQL.
 *
 * Ordered on purpose: the empty-state assertions must run before the first
 * backup ever recorded, and the restore round-trip reuses the backup created
 * earlier in the class. Restore rewrites the business/catalogue tables
 * (DELETE + reinsert of the snapshot), so this class only seeds its own rows
 * (uniq() sponsors) and never relies on business data from other IT classes —
 * auth tables (users/roles/permissions/system_log) are excluded from restore
 * by design and stay untouched.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AdminBackupRestoreIT extends AbstractIntegrationTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> EXCLUDED = Set.of(
            "users", "roles", "rights", "permissions", "system_log", "refresh_tokens", "backups");

    @Autowired
    private BackupRepository backupRepository;
    @Autowired
    private SponsorRepository sponsorRepository;

    // State handed from the backup test to the restore/guard tests (class runs ordered).
    private static int backupId;
    private static String backupCode;
    private static String sponsorName;

    private Cookie adminToken() throws Exception {
        return cookieOf(registerUserWithRole("Admin"), "access_token");
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    @Test
    @Order(1)
    void emptyState_beforeAnyBackup_latestNullAndHistoryEmpty() throws Exception {
        Cookie admin = adminToken();

        mockMvc.perform(get("/admin/backups/overview").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latest").isEmpty())
                .andExpect(jsonPath("$.data.schedule.enabled").value(false))   // test profile
                .andExpect(jsonPath("$.data.schedule.nextRunAt").isEmpty())
                .andExpect(jsonPath("$.data.schedule.cron").exists());

        mockMvc.perform(get("/admin/backups").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/admin/backups").cookie(admin).param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @Order(2)
    void createFullBackup_writesRealFileWithChecksumAndAuditPair() throws Exception {
        Cookie admin = adminToken();
        sponsorName = "Sponsor-" + uniq();
        sponsorRepository.save(Sponsor.builder().name(sponsorName).contactInfo("0123").build());
        long logsBefore = systemLogRepository.count();

        MvcResult result = mockMvc.perform(post("/admin/backups").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"FULL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.type").value("FULL"))
                .andExpect(jsonPath("$.data.filePath").doesNotExist())
                .andExpect(jsonPath("$.data.checksumSha256").isNotEmpty())
                .andExpect(jsonPath("$.data.createdByName").value("IT User"))
                .andExpect(jsonPath("$.data.completedAt").exists())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        backupId = JsonPath.read(body, "$.data.id");
        backupCode = JsonPath.read(body, "$.data.code");
        assertTrue(Pattern.matches("BK-\\d{4}-\\d{3}", backupCode), backupCode);

        Backup record = backupRepository.findById(backupId).orElseThrow();
        Path file = Path.of(record.getFilePath());
        assertTrue(Files.exists(file));
        assertEquals(Files.size(file), record.getSizeBytes());
        assertEquals(sha256(file), record.getChecksumSha256());

        JsonNode root = JSON.readTree(file.toFile());
        JsonNode data = root.get("data");
        // absolute exclusions never leak into the file
        for (String excluded : EXCLUDED) {
            assertFalse(data.has(excluded), excluded + " must not be dumped");
        }
        // FULL scope: business + catalogue = 22 tables, meta counts match data
        assertEquals(22, data.size());
        assertEquals(22, (int) record.getTableCount());
        JsonNode metaTables = root.get("meta").get("tables");
        long metaRows = 0;
        for (String table : (Iterable<String>) data::fieldNames) {
            assertEquals(metaTables.get(table).asInt(), data.get(table).size(), table);
            metaRows += data.get(table).size();
        }
        assertEquals(metaRows, (long) record.getRowCount());
        // the seeded sponsor is inside the dump
        assertTrue(data.get("sponsors").toString().contains(sponsorName));

        // BACKUP_CREATE + BACKUP_COMPLETE audit pair
        assertEquals(logsBefore + 2, systemLogRepository.count());
        assertTrue(systemLogRepository.findAll().stream()
                .anyMatch(l -> "BACKUP_CREATE".equals(l.getAction()) && backupCode.equals(l.getEntityId())));
        assertTrue(systemLogRepository.findAll().stream()
                .anyMatch(l -> "BACKUP_COMPLETE".equals(l.getAction()) && backupCode.equals(l.getEntityId())));

        // overview now reports this backup as latest
        mockMvc.perform(get("/admin/backups/overview").cookie(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.latest.code").value(backupCode));
    }

    @Test
    @Order(3)
    void restore_bringsBusinessDataBackToSnapshot_authTablesUntouched() throws Exception {
        Cookie admin = adminToken();
        // mutate after the snapshot: drop the backed-up sponsor, add a new one
        Sponsor original = sponsorRepository.findAll().stream()
                .filter(s -> sponsorName.equals(s.getName())).findFirst().orElseThrow();
        sponsorRepository.delete(original);
        sponsorRepository.save(Sponsor.builder().name("AfterBackup-" + uniq()).contactInfo("x").build());
        long usersBefore = userRepository.count();
        long logsBefore = systemLogRepository.count();

        mockMvc.perform(post("/admin/restore").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backupId\":" + backupId + ",\"confirm\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.backupCode").value(backupCode))
                .andExpect(jsonPath("$.data.restoredTables").value(22))
                .andExpect(jsonPath("$.data.restoredRows").isNumber())
                .andExpect(jsonPath("$.data.durationMs").isNumber());

        // snapshot restored: original sponsor back, post-backup sponsor gone
        assertTrue(sponsorRepository.findAll().stream().anyMatch(s -> sponsorName.equals(s.getName())));
        assertTrue(sponsorRepository.findAll().stream()
                .noneMatch(s -> s.getName() != null && s.getName().startsWith("AfterBackup-")));

        // excluded tables untouched; audit gained exactly START + COMPLETE
        assertEquals(usersBefore, userRepository.count());
        assertEquals(logsBefore + 2, systemLogRepository.count());
        assertTrue(systemLogRepository.findAll().stream()
                .anyMatch(l -> "RESTORE_START".equals(l.getAction()) && backupCode.equals(l.getEntityId())));
        assertTrue(systemLogRepository.findAll().stream()
                .anyMatch(l -> "RESTORE_COMPLETE".equals(l.getAction()) && backupCode.equals(l.getEntityId())));

        // the admin session survives the restore (auth tables untouched)
        mockMvc.perform(get("/admin/backups/overview").cookie(admin))
                .andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void restore_guardsRejectBadRequests() throws Exception {
        Cookie admin = adminToken();

        mockMvc.perform(post("/admin/restore").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backupId\":" + backupId + ",\"confirm\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10032));   // CONFIRM_REQUIRED

        mockMvc.perform(post("/admin/restore").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backupId\":99999999,\"confirm\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(10029));   // BACKUP_NOT_FOUND
    }

    @Test
    @Order(5)
    void restore_tamperedFile_marksCorruptedThenNotRestorable() throws Exception {
        Cookie admin = adminToken();
        Backup record = backupRepository.findById(backupId).orElseThrow();
        Files.writeString(Path.of(record.getFilePath()), "x", StandardOpenOption.APPEND);

        mockMvc.perform(post("/admin/restore").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backupId\":" + backupId + ",\"confirm\":true}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10031));   // BACKUP_CORRUPTED
        assertEquals(BackupStatus.CORRUPTED,
                backupRepository.findById(backupId).orElseThrow().getStatus());

        // no longer restorable (status guard fires before the checksum)
        mockMvc.perform(post("/admin/restore").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backupId\":" + backupId + ",\"confirm\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10030));   // BACKUP_NOT_RESTORABLE

        // and it disappeared from the COMPLETED dropdown feed
        mockMvc.perform(get("/admin/backups").cookie(admin).param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    @Order(6)
    void backupList_filtersAndBusinessScope() throws Exception {
        Cookie admin = adminToken();

        // BUSINESS backup: only the 11 business tables, no catalogue, no excluded
        MvcResult result = mockMvc.perform(post("/admin/backups").cookie(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"BUSINESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andReturn();
        int businessId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        Backup record = backupRepository.findById(businessId).orElseThrow();
        JsonNode data = JSON.readTree(Path.of(record.getFilePath()).toFile()).get("data");
        assertEquals(11, data.size());
        assertFalse(data.has("policies"));
        assertFalse(data.has("users"));
        // per-year sequence increments
        assertTrue(record.getCode().compareTo(backupCode) > 0);

        mockMvc.perform(get("/admin/backups").cookie(admin).param("type", "BUSINESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].code").value(record.getCode()));
        mockMvc.perform(get("/admin/backups").cookie(admin).param("status", "GARBAGE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10010));
    }

    @Test
    @Order(7)
    void rbac_backupAndRestoreRequireTheirRights() throws Exception {
        Cookie citizen = cookieOf(registerUser(uniqueEmail(), uniquePhone()), "access_token");
        long logsBefore = systemLogRepository.count();

        mockMvc.perform(get("/admin/backups/overview").cookie(citizen))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/backups").cookie(citizen)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"FULL\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/restore").cookie(citizen)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backupId\":1,\"confirm\":true}"))
                .andExpect(status().isForbidden());
        // each 403 recorded as ILLEGAL_REQUEST
        assertEquals(logsBefore + 3, systemLogRepository.count());

        // Head has AUDIT_LOG_VIEW but not BACKUP_MANAGE/RESTORE_MANAGE
        Cookie head = cookieOf(registerUserWithRole("Head"), "access_token");
        mockMvc.perform(get("/admin/backups").cookie(head))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/admin/restore").cookie(head)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backupId\":1,\"confirm\":true}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/backups"))
                .andExpect(status().isUnauthorized());
    }
}
