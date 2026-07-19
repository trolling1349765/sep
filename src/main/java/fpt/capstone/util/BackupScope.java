package fpt.capstone.util;

import fpt.capstone.enums.BackupType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The single source of truth for which DB tables backup/restore touches.
 * Table names are the REAL schema names (several differ from their entity
 * class names — singular forms, "eligibility_criterias"...). Order matters:
 * dumps and restores iterate these lists deterministically.
 *
 * EXCLUDED_TABLES (auth/RBAC/audit + the backups ledger itself) must NEVER
 * appear in a backup file nor be touched by a restore — a restore must not be
 * able to lock everyone out or rewrite the audit trail.
 */
public final class BackupScope {

    private BackupScope() {
    }

    /** Frequently-changing business data — the BUSINESS backup scope. */
    public static final List<String> BUSINESS_TABLES = List.of(
            "applications",
            "additional_document",
            "decision_document",
            "benificiaries",
            "benefit_history",
            "relatives",
            "wounded_soldiers",
            "support_requests",
            "support_replies",
            "support_request_attachments",
            "notifications");

    /** Catalogue/content tables, additionally included in FULL backups. */
    public static final List<String> CATALOGUE_TABLES = List.of(
            "policies",
            "chapters",
            "articles",
            "form_type",
            "benefit_rule",
            "eligibility_criterias",
            "sponsors",
            "donations",
            "goods_inventories",
            "goods_distributions",
            "distribution_record");

    /** Never backed up, never restored. */
    public static final Set<String> EXCLUDED_TABLES = Set.of(
            "users", "roles", "rights", "permissions",
            "system_log", "refresh_tokens", "backups");

    public static List<String> tablesFor(BackupType type) {
        if (type == BackupType.BUSINESS) {
            return BUSINESS_TABLES;
        }
        List<String> all = new ArrayList<>(BUSINESS_TABLES);
        all.addAll(CATALOGUE_TABLES);
        return List.copyOf(all);
    }

    /** Every table a backup file may legally contain (BUSINESS ∪ CATALOGUE). */
    public static Set<String> restorableTables() {
        Set<String> all = new HashSet<>(BUSINESS_TABLES);
        all.addAll(CATALOGUE_TABLES);
        return all;
    }
}
