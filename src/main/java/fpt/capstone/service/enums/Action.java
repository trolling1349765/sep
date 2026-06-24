package fpt.capstone.service.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum Action {

    USER_LOGIN("USER_LOGIN"),
    USER_LOGOUT("USER_LOGOUT"),
    PASSWORD_RESET("PASSWORD_RESET"),
    CHANGE_PASSWORD("CHANGE_PASSWORD"),
    ILLEGAL_REQUEST("ILLEGAL_REQUEST"),
    CREATE_APPLICATION("CREATE_APPLICATION"),
    CREATE_USER("CREATE_USER"),
    DELETE_USER("DELETE_USER"),
    CHANGE_ROLE("CHANGE_ROLE"),
    CRREATE_ROLE("CRREATE_ROLE"),
    CREATE_RIGHT("CREATE_RIGHT"),
    ;

    private String action;
}
