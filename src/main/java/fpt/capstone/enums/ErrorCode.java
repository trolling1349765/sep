package fpt.capstone.enums;

import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@NoArgsConstructor
public enum ErrorCode {
    SUCCESS(1000, "Success"),
    USER_EXISTED(1001, "User already exists"),
    EMAIL_EXISTED(1002, "Email already exists"),
    USERNAME_EXISTED(1003, "Username already exists"),
    PASSWORD_INVALID(1004, "Password must be in range 8 and 20 digits"),
    OCR_TEXT_INVALID(1005, "OCR text must be clearly"),
    USERNAME_NOT_EXISTED(1006, "User not exists"),
    UNAUTHENTICATED(1007, "Unauthenticated"),
    APPLICATION_NOT_FOUND(1008, "Application not found"),
    FILE_IO_ERROR(1009, "Cannot access file service"),
    ARGUMENT_INVALID(10010, "Argument out of range"),
    BENIFIT_HISTORY_NOT_FOUND(10011, "Benifit history not found"),
    ADDITIONAL_DOCUMENT_NOT_FOUND(10012, "Additional document not found"),
    BENIFICIARY_NOT_FOUND(10013, "Benificiary not found"),
    ROLE_NOT_FOUND(10014, "Role not found"),
    RIGHT_NOT_FOUND(10015, "Right not found"),
    SYSTEM_RIGHT_REQUIRED(10016, "System rights cannot be removed from any role"),
    ADMIN_ROLE_LOCKED(10017, "Permissions of the Admin role cannot be modified"),
    RIGHT_CODE_EXISTS(10018, "Right code already exists"),
    INVALID_RIGHT_CODE(10019, "Right code does not match the naming convention"),
    ACCESS_DENIED(10020, "Access denied"),
    CANNOT_CHANGE_OWN_ROLE(10021, "Cannot change the role of your own account"),
    ACCOUNT_BANNED(10022, "Your account has been banned. Please contact the administrator."),
    PHONE_EXISTED(10023, "Phone number already exists"),
    CITIZEN_ROLE_RESTRICTED(10024, "Citizen role cannot be assigned or changed via admin APIs"),
    CANNOT_DEACTIVATE_SELF(10025, "Cannot change the status of your own account"),
    INVALID_STATUS(10026, "Invalid or unchanged account status"),
    ACCOUNT_INACTIVE(10027, "Your account has been deactivated. Please contact the administrator."),
    LOG_NOT_FOUND(10028, "System log record not found"),
    BACKUP_NOT_FOUND(10029, "Backup not found"),
    BACKUP_NOT_RESTORABLE(10030, "Only completed backups can be restored"),
    BACKUP_CORRUPTED(10031, "Backup file is missing or corrupted"),
    CONFIRM_REQUIRED(10032, "Confirmation is required for this operation"),
    BACKUP_IN_PROGRESS(10033, "Another backup is already running"),
    INVALID_PAGE(10034, "page number is out of range"),
    ;

    private int code;
    private String message;

    ErrorCode(int code, String message) {
        this.message = message;
        this.code = code;
    }
}
