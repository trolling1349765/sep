package fpt.capstone.exceprion.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum ErrorCode {
    USER_EXISTED(1001, "User already exists"),
    EMAIL_EXISTED(1002, "Email already exists"),
    USERNAME_EXISTED(1003, "Username already exists"),
    PASSWORD_INVALID(1004, "Password must be in range 8 and 20 digits"),
    ;

    private int code;
    private String message;
}
