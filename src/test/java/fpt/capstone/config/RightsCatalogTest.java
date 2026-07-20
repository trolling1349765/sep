package fpt.capstone.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Invariants of the RBAC seed data.
 * These fail fast when the catalogue or base matrix is edited inconsistently.
 */
class RightsCatalogTest {

    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$");
    private static final Set<String> SYSTEM_RIGHTS = Set.of(
            "PROFILE_VIEW", "PROFILE_UPDATE", "PASSWORD_CHANGE", "NOTIFICATION_VIEW");

    @Nested
    @DisplayName("Rights catalogue")
    class Catalogue {

        @Test
        void rights_shouldContainExactly104Entries() {
            assertEquals(104, RightsCatalog.RIGHTS.length);
        }

        @Test
        void rights_shouldSpanExactly30Modules() {
            Set<String> modules = Arrays.stream(RightsCatalog.RIGHTS)
                    .map(row -> row[1])
                    .collect(Collectors.toSet());
            assertEquals(30, modules.size());
        }

        @Test
        void rights_codesShouldBeUniqueAndMatchNamingConvention() {
            Set<String> seen = new HashSet<>();
            for (String[] row : RightsCatalog.RIGHTS) {
                assertTrue(seen.add(row[0]), "Duplicate right code: " + row[0]);
                assertTrue(CODE_PATTERN.matcher(row[0]).matches(),
                        "Code violates convention: " + row[0]);
            }
        }

        @Test
        void rights_shouldFlagExactlyTheFourSystemRights() {
            Set<String> flagged = Arrays.stream(RightsCatalog.RIGHTS)
                    .filter(row -> "1".equals(row[4]))
                    .map(row -> row[0])
                    .collect(Collectors.toSet());
            assertEquals(SYSTEM_RIGHTS, flagged);
        }

        @Test
        void rights_everyRowShouldHaveModuleAndDisplayName() {
            for (String[] row : RightsCatalog.RIGHTS) {
                assertEquals(5, row.length, "Row of wrong arity: " + Arrays.toString(row));
                assertFalse(row[1].isBlank(), "Blank module for " + row[0]);
                assertFalse(row[2].isBlank(), "Blank module name for " + row[0]);
                assertFalse(row[3].isBlank(), "Blank display name for " + row[0]);
            }
        }

        @Test
        void rights_modulesShouldBeContiguousBlocks() {
            // seedRights derives sortOrder from row position per module; a module
            // split into separate blocks would signal a misplaced row
            Map<String, Integer> lastIndex = new HashMap<>();
            String previousModule = null;
            for (int i = 0; i < RightsCatalog.RIGHTS.length; i++) {
                String module = RightsCatalog.RIGHTS[i][1];
                if (!module.equals(previousModule)) {
                    assertFalse(lastIndex.containsKey(module),
                            "Module appears in two separate blocks: " + module);
                }
                lastIndex.put(module, i);
                previousModule = module;
            }
        }
    }

    @Nested
    @DisplayName("Base permission matrix")
    class BaseMatrix {

        @Test
        void matrix_shouldGrantTheSpecifiedCountPerRole() {
            Map<String, Integer> expected = Map.of(
                    "Citizen", 19,
                    "Reception", 30,
                    "Appraisal", 33,
                    "Head", 47,
                    "Leader", 37,
                    "Records", 27,
                    "Management", 53,
                    "Admin", 18);
            assertEquals(expected.keySet(), RightsCatalog.BASE_MATRIX.keySet());
            expected.forEach((role, count) -> assertEquals(count.intValue(),
                    RightsCatalog.BASE_MATRIX.get(role).length,
                    "Wrong base-grant count for role " + role));
        }

        @Test
        void matrix_shouldOnlyReferenceCataloguedCodesWithoutDuplicates() {
            Set<String> catalogued = Arrays.stream(RightsCatalog.RIGHTS)
                    .map(row -> row[0])
                    .collect(Collectors.toSet());
            RightsCatalog.BASE_MATRIX.forEach((role, codes) -> {
                Set<String> unique = new HashSet<>(List.of(codes));
                assertEquals(codes.length, unique.size(), "Duplicate grant in role " + role);
                unique.forEach(code -> assertTrue(catalogued.contains(code),
                        "Role " + role + " references unknown code " + code));
            });
        }

        @Test
        void matrix_everyRoleShouldKeepAllSystemRights() {
            RightsCatalog.BASE_MATRIX.forEach((role, codes) -> assertTrue(Set.of(codes).containsAll(SYSTEM_RIGHTS),
                    "Role " + role + " is missing a system right"));
        }

        @Test
        void matrix_adminShouldHoldNoBusinessRights() {
            Set<String> adminModules = Set.of("TAI_KHOAN", "THONG_BAO", "QUAN_TRI", "NGUOI_DUNG",
                    "VAI_TRO", "PHAN_QUYEN", "QUY_TRINH", "TICH_HOP", "NHAT_KY", "SAO_LUU", "KHOI_PHUC");
            Map<String, String> moduleByCode = Arrays.stream(RightsCatalog.RIGHTS)
                    .collect(Collectors.toMap(row -> row[0], row -> row[1]));
            for (String code : RightsCatalog.BASE_MATRIX.get("Admin")) {
                assertTrue(adminModules.contains(moduleByCode.get(code)),
                        "Admin holds a business right: " + code);
            }
        }
    }
}
