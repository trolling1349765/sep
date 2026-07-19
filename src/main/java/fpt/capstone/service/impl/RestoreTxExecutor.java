package fpt.capstone.service.impl;

import fpt.capstone.util.BackupScope;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The transactional core of a restore. Separate bean so the @Transactional
 * proxy is guaranteed (no self-invocation): inside the transaction,
 * JpaTransactionManager exposes its JDBC connection to spring-jdbc via
 * DataSourceUtils, so the FK-checks toggle, every DELETE and every INSERT run
 * on the SAME pinned connection and roll back together.
 */
@Service
@RequiredArgsConstructor
public class RestoreTxExecutor {

    private static final int BATCH_SIZE = 500;

    private final JdbcTemplate jdbcTemplate;

    public record RestoreStats(int restoredTables, long restoredRows) {
    }

    @Transactional
    public RestoreStats execute(Map<String, List<Map<String, Object>>> data) {
        // Caller validated against BackupScope already; re-check as defense in depth.
        for (String table : data.keySet()) {
            if (!BackupScope.restorableTables().contains(table)) {
                throw new IllegalStateException("Table outside backup scope: " + table);
            }
        }
        // Session-scoped variable: MUST be reset before the connection returns
        // to the pool, even on rollback — hence the finally inside the tx method.
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");
        try {
            long rows = 0;
            // Deterministic scope order; DELETE (not TRUNCATE — TRUNCATE
            // implicit-commits in MySQL and would break atomicity).
            for (String table : orderedTables(data)) {
                jdbcTemplate.update("DELETE FROM `" + table + "`");
                rows += insertAll(table, data.get(table));
            }
            return new RestoreStats(data.size(), rows);
        } finally {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");
        }
    }

    // Tables in BackupScope declaration order, restricted to those in the file.
    private static List<String> orderedTables(Map<String, List<Map<String, Object>>> data) {
        List<String> ordered = new ArrayList<>(BackupScope.BUSINESS_TABLES);
        ordered.addAll(BackupScope.CATALOGUE_TABLES);
        return ordered.stream().filter(data::containsKey).collect(Collectors.toList());
    }

    private long insertAll(String table, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        // Jackson parses rows into LinkedHashMap, so the first row's key order
        // is the dump's column order and applies to every row of the table.
        List<String> columns = new ArrayList<>(rows.get(0).keySet());
        String columnSql = columns.stream()
                .map(c -> "`" + c.replace("`", "``") + "`")
                .collect(Collectors.joining(","));
        String sql = "INSERT INTO `" + table + "` (" + columnSql + ") VALUES ("
                + String.join(",", Collections.nCopies(columns.size(), "?")) + ")";

        jdbcTemplate.batchUpdate(sql, rows, BATCH_SIZE, (ps, row) -> {
            int i = 1;
            for (String column : columns) {
                // JSON-native values (String/Number/Boolean/null); MySQL
                // coerces datetime strings back into DATETIME columns.
                ps.setObject(i++, row.get(column));
            }
        });
        return rows.size();
    }
}
