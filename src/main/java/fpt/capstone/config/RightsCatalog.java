package fpt.capstone.config;

import java.util.Map;

/**
 * Canonical RBAC seed data:
 * - RIGHTS: 102 rights across 30 modules. Rows are {code, module, moduleName,
 * name, isSystem}.
 * sortOrder is derived from the row's position within its module.
 * - BASE_MATRIX: initial right codes per role. Only applied when a role has
 * zero
 * permissions in the DB — admin-edited grants are never overwritten by the
 * seed.
 *
 * Codes are immutable identifiers; display names may be edited later via the
 * admin API,
 * this catalog only supplies initial values.
 */
public final class RightsCatalog {

        public static final String[][] RIGHTS = {
                        // TAI_KHOAN — Tài khoản cá nhân (3)
                        { "PROFILE_VIEW", "TAI_KHOAN", "Tài khoản cá nhân", "profile Xem", "1" },
                        { "PROFILE_UPDATE", "TAI_KHOAN", "Tài khoản cá nhân", "profile Cập nhật", "1" },
                        { "PASSWORD_CHANGE", "TAI_KHOAN", "Tài khoản cá nhân", "password change", "1" },
                        // CHINH_SACH — Chính sách (6)
                        { "POLICY_VIEW", "CHINH_SACH", "Chính sách", "policy Xem", "0" },
                        { "POLICY_CREATE", "CHINH_SACH", "Chính sách", "policy Tạo", "0" },
                        { "POLICY_UPDATE", "CHINH_SACH", "Chính sách", "policy Cập nhật", "0" },
                        { "POLICY_DEACTIVATE", "CHINH_SACH", "Chính sách", "policy Ngừng hoạt động", "0" },
                        { "POLICY_PUBLISH", "CHINH_SACH", "Chính sách", "policy publish", "0" },
                        { "POLICY_HISTORY_VIEW", "CHINH_SACH", "Chính sách", "policy history Xem", "0" },
                        // QUYEN_LOI — Chính sách và quyền lợi (7)
                        { "BENEFIT_RULE_VIEW", "QUYEN_LOI", "Chính sách và quyền lợi", "benefit rule Xem", "0" },
                        { "BENEFIT_RULE_CONFIGURE", "QUYEN_LOI", "Chính sách và quyền lợi", "benefit rule Cấu hình",
                                        "0" },
                        { "BENEFIT_RULE_DEPRECATE", "QUYEN_LOI", "Chính sách và quyền lợi", "benefit rule deprecate",
                                        "0" },
                        { "BENEFIT_CALCULATE", "QUYEN_LOI", "Chính sách và quyền lợi", "benefit calculate", "0" },
                        { "BENEFIT_OVERRIDE", "QUYEN_LOI", "Chính sách và quyền lợi", "benefit override", "0" },
                        { "BENEFIT_TERMINATE", "QUYEN_LOI", "Chính sách và quyền lợi", "benefit terminate", "0" },
                        { "BENEFIT_HISTORY_VIEW", "QUYEN_LOI", "Chính sách và quyền lợi", "benefit history Xem", "0" },
                        // DIEU_KIEN — Điều kiện hưởng (2)
                        { "ELIGIBILITY_CHECK", "DIEU_KIEN", "Điều kiện hưởng", "eligibility check", "0" },
                        { "ELIGIBILITY_EVALUATE", "DIEU_KIEN", "Điều kiện hưởng", "eligibility evaluate", "0" },
                        // HO_SO — Hồ sơ (24)
                        { "APPLICATION_VIEW_OWN", "HO_SO", "Hồ sơ", "application Xem own", "0" },
                        { "APPLICATION_CREATE", "HO_SO", "Hồ sơ", "application Tạo", "0" },
                        { "APPLICATION_UPDATE_OWN_DRAFT", "HO_SO", "Hồ sơ", "application Cập nhật own draft", "0" },
                        { "APPLICATION_SUPPLEMENT_OWN", "HO_SO", "Hồ sơ", "application supplement own", "0" },
                        { "APPLICATION_VIEW", "HO_SO", "Hồ sơ", "application Xem", "0" },
                        { "APPLICATION_INTAKE", "HO_SO", "Hồ sơ", "application intake", "0" },
                        { "APPLICATION_INTAKE_VIEW", "HO_SO", "Hồ sơ", "application intake Xem", "0" },
                        { "APPLICATION_INTAKE_CREATE", "HO_SO", "Hồ sơ", "application intake Tạo", "0" },
                        { "APPLICATION_INTAKE_UPDATE", "HO_SO", "Hồ sơ", "application intake Cập nhật", "0" },
                        { "APPLICATION_INTAKE_CANCEL", "HO_SO", "Hồ sơ", "application intake cancel", "0" },
                        { "APPLICATION_REQUEST_SUPPLEMENT_AT_INTAKE", "HO_SO", "Hồ sơ",
                                        "application request supplement at intake", "0" },
                        { "APPLICATION_TRANSFER_TO_APPRAISAL", "HO_SO", "Hồ sơ", "application transfer to appraisal",
                                        "0" },
                        { "APPLICATION_APPRAISE", "HO_SO", "Hồ sơ", "application appraise", "0" },
                        { "APPLICATION_REQUEST_SUPPLEMENT_AT_APPRAISAL", "HO_SO", "Hồ sơ",
                                        "application request supplement at appraisal", "0" },
                        { "APPLICATION_REJECT_AT_APPRAISAL", "HO_SO", "Hồ sơ", "application Từ chối at appraisal",
                                        "0" },
                        { "APPLICATION_SUBMIT_TO_HEAD", "HO_SO", "Hồ sơ", "application submit to head", "0" },
                        { "APPLICATION_REVIEW_AS_HEAD", "HO_SO", "Hồ sơ", "application review as head", "0" },
                        { "APPLICATION_REJECT_AS_HEAD", "HO_SO", "Hồ sơ", "application Từ chối as head", "0" },
                        { "APPLICATION_SUBMIT_TO_COMMUNE_LEADER", "HO_SO", "Hồ sơ",
                                        "application submit to commune leader", "0" },
                        { "APPLICATION_FINAL_REVIEW", "HO_SO", "Hồ sơ", "application final review", "0" },
                        { "APPLICATION_FINAL_APPROVE", "HO_SO", "Hồ sơ", "application final Phê duyệt", "0" },
                        { "APPLICATION_REJECT_AS_FINAL", "HO_SO", "Hồ sơ", "application Từ chối as final", "0" },
                        { "APPLICATION_RETURN_FOR_REREVIEW", "HO_SO", "Hồ sơ", "application return for rereview", "0" },
                        { "APPLICATION_ARCHIVE", "HO_SO", "Hồ sơ", "application archive", "0" },
                        // TAI_LIEU — Tài liệu (3)
                        { "DOCUMENT_MANAGE", "TAI_LIEU", "Tài liệu", "document Quản lý", "0" },
                        { "DOCUMENT_OCR", "TAI_LIEU", "Tài liệu", "document ocr", "0" },
                        { "OCR_VERIFY", "TAI_LIEU", "Tài liệu", "ocr verify", "0" },
                        // TIEP_NHAN — Tiếp nhận (1)
                        { "RECEIPT_EXPORT", "TIEP_NHAN", "Tiếp nhận", "receipt Xuất dữ liệu", "0" },
                        // THAM_DINH — Thẩm định (1)
                        { "APPRAISAL_HISTORY_VIEW", "THAM_DINH", "Thẩm định", "appraisal history Xem", "0" },
                        // QUYET_DINH — Quyết định (3)
                        { "DECISION_DIGITALLY_SIGN", "QUYET_DINH", "Quyết định", "decision digitally sign", "0" },
                        { "DECISION_ISSUE", "QUYET_DINH", "Quyết định", "decision issue", "0" },
                        { "RESULT_PUBLISH", "QUYET_DINH", "Quyết định", "result publish", "0" },
                        // VAN_BAN — Văn bản chính thức (6)
                        { "OFFICIAL_DOCUMENT_VIEW", "VAN_BAN", "Văn bản chính thức", "official document Xem", "0" },
                        { "OFFICIAL_DOCUMENT_MANAGE", "VAN_BAN", "Văn bản chính thức", "official document Quản lý",
                                        "0" },
                        { "OFFICIAL_DOCUMENT_CREATE", "VAN_BAN", "Văn bản chính thức", "official document Tạo", "0" },
                        { "OFFICIAL_DOCUMENT_UPDATE_DRAFT", "VAN_BAN", "Văn bản chính thức",
                                        "official document Cập nhật draft", "0" },
                        { "OFFICIAL_DOCUMENT_CORRECT", "VAN_BAN", "Văn bản chính thức", "official document correct",
                                        "0" },
                        { "OFFICIAL_DOCUMENT_RETRACT", "VAN_BAN", "Văn bản chính thức", "official document retract",
                                        "0" },
                        // LUU_TRU — Lưu trữ (1)
                        { "ARCHIVE_VIEW", "LUU_TRU", "Lưu trữ", "archive Xem", "0" },
                        // DOI_TUONG — Đối tượng hưởng (5)
                        { "BENEFICIARY_VIEW", "DOI_TUONG", "Đối tượng hưởng", "beneficiary Xem", "0" },
                        { "BENEFICIARY_CREATE", "DOI_TUONG", "Đối tượng hưởng", "beneficiary Tạo", "0" },
                        { "BENEFICIARY_UPDATE", "DOI_TUONG", "Đối tượng hưởng", "beneficiary Cập nhật", "0" },
                        { "BENEFICIARY_STATUS_UPDATE", "DOI_TUONG", "Đối tượng hưởng", "beneficiary status Cập nhật",
                                        "0" },
                        { "BENEFICIARY_DIGITIZE", "DOI_TUONG", "Đối tượng hưởng", "beneficiary digitize", "0" },
                        // NHA_TAI_TRO — Nhà tài trợ (2)
                        { "SPONSOR_VIEW", "NHA_TAI_TRO", "Nhà tài trợ", "sponsor Xem", "0" },
                        { "SPONSOR_MANAGE", "NHA_TAI_TRO", "Nhà tài trợ", "sponsor Quản lý", "0" },
                        // KINH_PHI — Kinh phí (4)
                        { "FUNDING_VIEW", "KINH_PHI", "Kinh phí", "funding Xem", "0" },
                        { "FUNDING_MANAGE", "KINH_PHI", "Kinh phí", "funding Quản lý", "0" },
                        { "FUNDING_RECORD_CREATE", "KINH_PHI", "Kinh phí", "funding record Tạo", "0" },
                        { "FUNDING_ALLOCATE", "KINH_PHI", "Kinh phí", "funding allocate", "0" },
                        // VAT_PHAM — Vật phẩm và hỗ trợ (3)
                        { "SUPPORT_ITEM_VIEW", "VAT_PHAM", "Vật phẩm và hỗ trợ", "support item Xem", "0" },
                        { "SUPPORT_ITEM_RECEIVE", "VAT_PHAM", "Vật phẩm và hỗ trợ", "support item receive", "0" },
                        { "SUPPORT_REQUEST_CREATE", "VAT_PHAM", "Vật phẩm và hỗ trợ", "support request Tạo", "0" },
                        // KHO — Kho (5)
                        { "INVENTORY_MANAGE", "KHO", "Kho", "inventory Quản lý", "0" },
                        { "INBOUND_RECEIPT_CREATE", "KHO", "Kho", "inbound receipt Tạo", "0" },
                        { "INBOUND_RECEIPT_POST", "KHO", "Kho", "inbound receipt post", "0" },
                        { "INVENTORY_VIEW", "KHO", "Kho", "inventory Xem", "0" },
                        { "INVENTORY_ADJUST", "KHO", "Kho", "inventory adjust", "0" },
                        // CAP_PHAT — Cấp phát (4)
                        { "ITEM_ALLOCATION_PLAN_CREATE", "CAP_PHAT", "Cấp phát", "item allocation plan Tạo", "0" },
                        { "ITEM_DISTRIBUTE", "CAP_PHAT", "Cấp phát", "item distribute", "0" },
                        { "DISTRIBUTION_CONFIRM", "CAP_PHAT", "Cấp phát", "distribution confirm", "0" },
                        { "DISTRIBUTION_HISTORY_VIEW", "CAP_PHAT", "Cấp phát", "distribution history Xem", "0" },
                        // BAO_CAO — Báo cáo (5)
                        { "REPORT_VIEW", "BAO_CAO", "Báo cáo", "report Xem", "0" },
                        { "REPORT_CREATE", "BAO_CAO", "Báo cáo", "report Tạo", "0" },
                        { "REPORT_UPDATE_DRAFT", "BAO_CAO", "Báo cáo", "report Cập nhật draft", "0" },
                        { "REPORT_DELETE_DRAFT", "BAO_CAO", "Báo cáo", "report delete draft", "0" },
                        { "REPORT_EXPORT", "BAO_CAO", "Báo cáo", "report Xuất dữ liệu", "0" },
                        // THONG_BAO — Thông báo (1)
                        { "NOTIFICATION_VIEW", "THONG_BAO", "Thông báo", "notification Xem", "1" },
                        // TRO_LY — Trợ lý chính sách (1)
                        { "CHATBOT_USE", "TRO_LY", "Trợ lý chính sách", "chatbot use", "0" },
                        // PHAN_ANH — Phản ánh (1)
                        { "FEEDBACK_CREATE", "PHAN_ANH", "Phản ánh", "feedback Tạo", "0" },
                        // QUAN_TRI — Quản trị hệ thống (2)
                        { "SYSTEM_MONITOR_VIEW", "QUAN_TRI", "Quản trị hệ thống", "system monitor Xem", "0" },
                        { "SYSTEM_CONFIGURE", "QUAN_TRI", "Quản trị hệ thống", "system Cấu hình", "0" },
                        // NGUOI_DUNG — Người dùng (5)
                        { "USER_MANAGE", "NGUOI_DUNG", "Người dùng", "user Quản lý", "0" },
                        { "USER_VIEW", "NGUOI_DUNG", "Người dùng", "user Xem", "0" },
                        { "USER_CREATE", "NGUOI_DUNG", "Người dùng", "user Tạo", "0" },
                        { "USER_UPDATE", "NGUOI_DUNG", "Người dùng", "user Cập nhật", "0" },
                        { "USER_DEACTIVATE", "NGUOI_DUNG", "Người dùng", "user Ngừng hoạt động", "0" },
                        // VAI_TRO — Vai trò (1)
                        { "ROLE_MANAGE", "VAI_TRO", "Vai trò", "role Quản lý", "0" },
                        // PHAN_QUYEN — Phân quyền (1)
                        { "PERMISSION_MANAGE", "PHAN_QUYEN", "Phân quyền", "permission Quản lý", "0" },
                        // QUY_TRINH — Quy trình (1)
                        { "WORKFLOW_MANAGE", "QUY_TRINH", "Quy trình", "workflow Quản lý", "0" },
                        // TICH_HOP — Tích hợp (1)
                        { "INTEGRATION_CONFIGURE", "TICH_HOP", "Tích hợp", "integration Cấu hình", "0" },
                        // NHAT_KY — Nhật ký (1)
                        { "AUDIT_LOG_VIEW", "NHAT_KY", "Nhật ký", "audit log Xem", "0" },
                        // SAO_LUU — Sao lưu (1)
                        { "BACKUP_MANAGE", "SAO_LUU", "Sao lưu", "backup Quản lý", "0" },
                        // KHOI_PHUC — Khôi phục (1)
                        { "RESTORE_MANAGE", "KHOI_PHUC", "Khôi phục", "restore Quản lý", "0" },
        };

        // Base grant matrix: Citizen 19, Reception 30, Appraisal 29, Head 47,
        // Leader 37, Records 27, Management 53, Admin 18 (pure administration).
        public static final Map<String, String[]> BASE_MATRIX = Map.of(
                        "Citizen", new String[] {
                                        "PROFILE_VIEW", "PROFILE_UPDATE", "PASSWORD_CHANGE",
                                        "POLICY_VIEW",
                                        "BENEFIT_HISTORY_VIEW",
                                        "ELIGIBILITY_CHECK",
                                        "APPLICATION_VIEW_OWN", "APPLICATION_CREATE", "APPLICATION_UPDATE_OWN_DRAFT",
                                        "APPLICATION_SUPPLEMENT_OWN",
                                        "OFFICIAL_DOCUMENT_VIEW",
                                        "ARCHIVE_VIEW",
                                        "BENEFICIARY_VIEW",
                                        "SUPPORT_REQUEST_CREATE",
                                        "DISTRIBUTION_HISTORY_VIEW",
                                        "REPORT_VIEW",
                                        "NOTIFICATION_VIEW",
                                        "CHATBOT_USE",
                                        "FEEDBACK_CREATE",
                        },
                        "Reception", new String[] {
                                        "PROFILE_VIEW", "PROFILE_UPDATE", "PASSWORD_CHANGE",
                                        "POLICY_VIEW", "POLICY_HISTORY_VIEW",
                                        "BENEFIT_RULE_VIEW",
                                        "ELIGIBILITY_CHECK",
                                        "APPLICATION_CREATE", "APPLICATION_VIEW",
                                        "APPLICATION_INTAKE", "APPLICATION_INTAKE_VIEW", "APPLICATION_INTAKE_CREATE",
                                        "APPLICATION_INTAKE_UPDATE", "APPLICATION_INTAKE_CANCEL",
                                        "APPLICATION_REQUEST_SUPPLEMENT_AT_INTAKE", "APPLICATION_TRANSFER_TO_APPRAISAL",
                                        "DOCUMENT_MANAGE", "DOCUMENT_OCR", "OCR_VERIFY",
                                        "RECEIPT_EXPORT",
                                        "APPRAISAL_HISTORY_VIEW",
                                        "OFFICIAL_DOCUMENT_VIEW",
                                        "ARCHIVE_VIEW",
                                        "BENEFICIARY_VIEW",
                                        "REPORT_VIEW", "REPORT_CREATE", "REPORT_UPDATE_DRAFT", "REPORT_DELETE_DRAFT",
                                        "REPORT_EXPORT",
                                        "NOTIFICATION_VIEW",
                        },
                        "Appraisal", new String[] {
                                        "PROFILE_VIEW", "PROFILE_UPDATE", "PASSWORD_CHANGE",
                                        "POLICY_VIEW", "POLICY_HISTORY_VIEW",
                                        "BENEFIT_RULE_VIEW", "BENEFIT_CALCULATE", "BENEFIT_HISTORY_VIEW",
                                        "ELIGIBILITY_CHECK", "ELIGIBILITY_EVALUATE",
                                        "APPLICATION_VIEW", "APPLICATION_INTAKE_VIEW",
                                        "APPLICATION_APPRAISE", "APPLICATION_REQUEST_SUPPLEMENT_AT_APPRAISAL",
                                        "APPLICATION_REJECT_AT_APPRAISAL", "APPLICATION_SUBMIT_TO_HEAD",
                                        "DOCUMENT_MANAGE", "DOCUMENT_OCR", "OCR_VERIFY",
                                        "APPRAISAL_HISTORY_VIEW",
                                        "OFFICIAL_DOCUMENT_VIEW",
                                        "ARCHIVE_VIEW",
                                        "BENEFICIARY_VIEW",
                                        "REPORT_VIEW", "REPORT_CREATE", "REPORT_UPDATE_DRAFT", "REPORT_DELETE_DRAFT",
                                        "REPORT_EXPORT",
                                        "NOTIFICATION_VIEW",
                        },
                        "Head", new String[] {
                                        "PROFILE_VIEW", "PROFILE_UPDATE", "PASSWORD_CHANGE",
                                        "POLICY_VIEW", "POLICY_CREATE", "POLICY_UPDATE", "POLICY_DEACTIVATE",
                                        "POLICY_PUBLISH",
                                        "POLICY_HISTORY_VIEW",
                                        "BENEFIT_RULE_VIEW", "BENEFIT_RULE_CONFIGURE", "BENEFIT_RULE_DEPRECATE",
                                        "BENEFIT_CALCULATE", "BENEFIT_OVERRIDE", "BENEFIT_TERMINATE",
                                        "BENEFIT_HISTORY_VIEW",
                                        "ELIGIBILITY_CHECK", "ELIGIBILITY_EVALUATE",
                                        "APPLICATION_VIEW", "APPLICATION_INTAKE_VIEW",
                                        "APPLICATION_REVIEW_AS_HEAD", "APPLICATION_REJECT_AS_HEAD",
                                        "APPLICATION_SUBMIT_TO_COMMUNE_LEADER", "APPLICATION_RETURN_FOR_REREVIEW",
                                        "DOCUMENT_MANAGE",
                                        "APPRAISAL_HISTORY_VIEW",
                                        "OFFICIAL_DOCUMENT_VIEW",
                                        "ARCHIVE_VIEW",
                                        "BENEFICIARY_VIEW", "BENEFICIARY_STATUS_UPDATE",
                                        "SPONSOR_VIEW",
                                        "FUNDING_VIEW", "FUNDING_MANAGE", "FUNDING_ALLOCATE",
                                        "SUPPORT_ITEM_VIEW",
                                        "INBOUND_RECEIPT_POST", "INVENTORY_VIEW",
                                        "ITEM_ALLOCATION_PLAN_CREATE", "DISTRIBUTION_HISTORY_VIEW",
                                        "REPORT_VIEW", "REPORT_CREATE", "REPORT_UPDATE_DRAFT", "REPORT_DELETE_DRAFT",
                                        "REPORT_EXPORT",
                                        "NOTIFICATION_VIEW",
                                        "USER_VIEW",
                                        "AUDIT_LOG_VIEW",
                        },
                        "Leader", new String[] {
                                        "PROFILE_VIEW", "PROFILE_UPDATE", "PASSWORD_CHANGE",
                                        "POLICY_VIEW", "POLICY_HISTORY_VIEW",
                                        "BENEFIT_RULE_VIEW", "BENEFIT_OVERRIDE", "BENEFIT_TERMINATE",
                                        "BENEFIT_HISTORY_VIEW",
                                        "ELIGIBILITY_CHECK",
                                        "APPLICATION_VIEW",
                                        "APPLICATION_FINAL_REVIEW", "APPLICATION_FINAL_APPROVE",
                                        "APPLICATION_REJECT_AS_FINAL",
                                        "APPLICATION_RETURN_FOR_REREVIEW",
                                        "DOCUMENT_MANAGE",
                                        "APPRAISAL_HISTORY_VIEW",
                                        "DECISION_DIGITALLY_SIGN",
                                        "OFFICIAL_DOCUMENT_VIEW", "OFFICIAL_DOCUMENT_CORRECT",
                                        "OFFICIAL_DOCUMENT_RETRACT",
                                        "ARCHIVE_VIEW",
                                        "BENEFICIARY_VIEW", "BENEFICIARY_STATUS_UPDATE",
                                        "SPONSOR_VIEW",
                                        "FUNDING_VIEW", "FUNDING_ALLOCATE",
                                        "SUPPORT_ITEM_VIEW",
                                        "INVENTORY_VIEW",
                                        "DISTRIBUTION_HISTORY_VIEW",
                                        "REPORT_VIEW", "REPORT_CREATE", "REPORT_UPDATE_DRAFT", "REPORT_DELETE_DRAFT",
                                        "REPORT_EXPORT",
                                        "NOTIFICATION_VIEW",
                                        "AUDIT_LOG_VIEW",
                        },
                        "Records", new String[] {
                                        "PROFILE_VIEW", "PROFILE_UPDATE", "PASSWORD_CHANGE",
                                        "POLICY_VIEW", "POLICY_PUBLISH", "POLICY_HISTORY_VIEW",
                                        "BENEFIT_RULE_VIEW", "BENEFIT_HISTORY_VIEW",
                                        "APPLICATION_VIEW", "APPLICATION_ARCHIVE",
                                        "DOCUMENT_MANAGE",
                                        "APPRAISAL_HISTORY_VIEW",
                                        "DECISION_ISSUE", "RESULT_PUBLISH",
                                        "OFFICIAL_DOCUMENT_VIEW", "OFFICIAL_DOCUMENT_MANAGE",
                                        "OFFICIAL_DOCUMENT_CREATE",
                                        "OFFICIAL_DOCUMENT_UPDATE_DRAFT", "OFFICIAL_DOCUMENT_CORRECT",
                                        "OFFICIAL_DOCUMENT_RETRACT",
                                        "ARCHIVE_VIEW",
                                        "REPORT_VIEW", "REPORT_CREATE", "REPORT_UPDATE_DRAFT", "REPORT_DELETE_DRAFT",
                                        "REPORT_EXPORT",
                                        "NOTIFICATION_VIEW",
                        },
                        "Management", new String[] {
                                        "PROFILE_VIEW", "PROFILE_UPDATE", "PASSWORD_CHANGE",
                                        "POLICY_VIEW", "POLICY_CREATE", "POLICY_UPDATE", "POLICY_DEACTIVATE",
                                        "POLICY_PUBLISH",
                                        "POLICY_HISTORY_VIEW",
                                        "BENEFIT_RULE_VIEW", "BENEFIT_CALCULATE", "BENEFIT_TERMINATE",
                                        "BENEFIT_HISTORY_VIEW",
                                        "ELIGIBILITY_CHECK", "ELIGIBILITY_EVALUATE",
                                        "APPLICATION_VIEW", "APPLICATION_ARCHIVE",
                                        "DOCUMENT_MANAGE", "DOCUMENT_OCR", "OCR_VERIFY",
                                        "APPRAISAL_HISTORY_VIEW",
                                        "OFFICIAL_DOCUMENT_VIEW", "OFFICIAL_DOCUMENT_MANAGE",
                                        "ARCHIVE_VIEW",
                                        "BENEFICIARY_VIEW", "BENEFICIARY_CREATE", "BENEFICIARY_UPDATE",
                                        "BENEFICIARY_STATUS_UPDATE", "BENEFICIARY_DIGITIZE",
                                        "SPONSOR_VIEW", "SPONSOR_MANAGE",
                                        "FUNDING_VIEW", "FUNDING_MANAGE", "FUNDING_RECORD_CREATE", "FUNDING_ALLOCATE",
                                        "SUPPORT_ITEM_VIEW", "SUPPORT_ITEM_RECEIVE", "SUPPORT_REQUEST_CREATE",
                                        "INVENTORY_MANAGE", "INBOUND_RECEIPT_CREATE", "INBOUND_RECEIPT_POST",
                                        "INVENTORY_VIEW", "INVENTORY_ADJUST",
                                        "ITEM_ALLOCATION_PLAN_CREATE", "ITEM_DISTRIBUTE", "DISTRIBUTION_CONFIRM",
                                        "DISTRIBUTION_HISTORY_VIEW",
                                        "REPORT_VIEW", "REPORT_CREATE", "REPORT_UPDATE_DRAFT", "REPORT_DELETE_DRAFT",
                                        "REPORT_EXPORT",
                                        "NOTIFICATION_VIEW",
                        },
                        "Admin", new String[] {
                                        "PROFILE_VIEW", "PROFILE_UPDATE", "PASSWORD_CHANGE", "NOTIFICATION_VIEW",
                                        "SYSTEM_MONITOR_VIEW", "SYSTEM_CONFIGURE",
                                        "USER_MANAGE", "USER_VIEW", "USER_CREATE", "USER_UPDATE", "USER_DEACTIVATE",
                                        "ROLE_MANAGE", "PERMISSION_MANAGE", "WORKFLOW_MANAGE", "INTEGRATION_CONFIGURE",
                                        "AUDIT_LOG_VIEW", "BACKUP_MANAGE", "RESTORE_MANAGE",
                        });

        private RightsCatalog() {
        }
}
