package fpt.capstone.exceprion.enums;

import lombok.AllArgsConstructor;
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
    OCR_TEXT_INVALID(1005, "OCR text must be clearly");
    ;

    private int code;
    private String message;

    ErrorCode(int code, String message) {
        this.message = message;
        this.code = code;
    }
}
