package fpt.capstone.config;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stable machine-readable role codes exposed to the FE ("Mã vai trò").
 * Role `name` remains the internal logical key (findByName, JWT claim, permission seed) —
 * these codes are display/contract identifiers only and must stay in sync with seedRoles.
 */
public final class RoleCodes {

    public static final String CITIZEN = "CITIZEN";
    public static final String RECEPTION_OFFICER = "RECEPTION_OFFICER";
    public static final String APPRAISAL_OFFICER = "APPRAISAL_OFFICER";
    public static final String HEAD_OF_DIVISION = "HEAD_OF_DIVISION";
    public static final String COMMUNE_LEADER = "COMMUNE_LEADER";
    public static final String RECORDS_CLERK = "RECORDS_CLERK";
    public static final String MANAGEMENT_OFFICER = "MANAGEMENT_OFFICER";
    public static final String SYSTEM_ADMINISTRATOR = "SYSTEM_ADMINISTRATOR";

    /** Role name -> code, in seed order (Citizen=1 ... Admin=8). */
    public static final Map<String, String> NAME_TO_CODE;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Citizen", CITIZEN);
        map.put("Reception", RECEPTION_OFFICER);
        map.put("Appraisal", APPRAISAL_OFFICER);
        map.put("Head", HEAD_OF_DIVISION);
        map.put("Leader", COMMUNE_LEADER);
        map.put("Records", RECORDS_CLERK);
        map.put("Management", MANAGEMENT_OFFICER);
        map.put("Admin", SYSTEM_ADMINISTRATOR);
        NAME_TO_CODE = Collections.unmodifiableMap(map);
    }

    private RoleCodes() {
    }
}
