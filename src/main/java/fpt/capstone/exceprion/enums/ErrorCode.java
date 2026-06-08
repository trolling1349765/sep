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
    ;

    private int code;
    private String message;
}
