package fpt.capstone.integration;

import com.jayway.jsonpath.JsonPath;
import fpt.capstone.config.DataInitializer;
import fpt.capstone.config.RightsCatalog;
import fpt.capstone.entity.Right;
import fpt.capstone.entity.Role;
import jakarta.servlet.http.Cookie;
import net.minidev.json.JSONArray;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RbacSeedIT extends AbstractIntegrationTest {

    @Autowired
    private DataInitializer dataInitializer;

    private static Set<String> catalogueCodes() {
        return Arrays.stream(RightsCatalog.RIGHTS).map(row -> row[0]).collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("AC5 - catalogue after first seed")
    class Catalogue {

        @Test
        void adminRightsEndpoint_shouldExpose102CatalogueRightsIn30Modules() throws Exception {
            MvcResult admin = registerUserWithRole("Admin");
            Cookie token = cookieOf(admin, "access_token");

            MvcResult result = mockMvc.perform(get("/admin/rights").cookie(token))
                    .andExpect(status().isOk())
                    .andReturn();

            String body = result.getResponse().getContentAsString();
            JSONArray codes = JsonPath.read(body, "$.data[*].rights[*].code");
            Set<String> returned = codes.stream().map(Object::toString).collect(Collectors.toSet());
            assertTrue(returned.containsAll(catalogueCodes()));

            JSONArray modules = JsonPath.read(body, "$.data[*].module");
            Set<String> moduleSet = modules.stream().map(Object::toString).collect(Collectors.toSet());
            Set<String> catalogueModules = Arrays.stream(RightsCatalog.RIGHTS)
                    .map(row -> row[1]).collect(Collectors.toSet());
            assertEquals(30, catalogueModules.size());
            assertTrue(moduleSet.containsAll(catalogueModules));
        }

        @Test
        void seededRoles_shouldAllCarryTheirStableCode() {
            // Backfill proof: on any DB (fresh or pre-`code`-column) every seeded
            // role ends up with its machine-readable code after startup.
            java.util.Map<String, String> expected = fpt.capstone.config.RoleCodes.NAME_TO_CODE;
            expected.forEach((name, code) -> {
                Role role = roleRepository.findByName(name).orElseThrow();
                assertEquals(code, role.getCode(), "code of role " + name);
            });
            assertEquals(expected.size(),
                    roleRepository.findAll().stream().filter(r -> expected.containsKey(r.getName())).count());
        }

        @Test
        void database_shouldContainEveryCatalogueRightExactlyOnce() {
            Set<String> seeded = rightRepository.findAll().stream()
                    .map(Right::getCode)
                    .filter(catalogueCodes()::contains)
                    .collect(Collectors.toSet());
            assertEquals(102, seeded.size());
        }
    }

    @Nested
    @DisplayName("AC12 - idempotent seeding")
    class Idempotency {

        @Test
        void rerunningSeed_shouldNotDuplicateRightsOrPermissions() throws Exception {
            long rightsBefore = rightRepository.count();
            long permissionsBefore = permissionRepository.count();

            dataInitializer.run();
            dataInitializer.run();

            assertEquals(rightsBefore, rightRepository.count());
            assertEquals(permissionsBefore, permissionRepository.count());
        }

        @Test
        void rerunningSeed_shouldNotRestoreManuallyRemovedGrant() throws Exception {
            Role records = roleRepository.findByName("Records").orElseThrow();
            Set<Integer> before = permissionRepository.findRightIdsByRoleId(records.getId());
            Integer removed = before.stream()
                    .filter(id -> !rightRepository.findById(id).orElseThrow().isSystem())
                    .findFirst().orElseThrow();
            permissionRepository.findAll().stream()
                    .filter(p -> p.getRole().getId() == records.getId()
                            && p.getRight().getId() == removed)
                    .findFirst()
                    .ifPresent(permissionRepository::delete);
            permissionCacheService.evictAll();

            dataInitializer.run();

            Set<Integer> after = permissionRepository.findRightIdsByRoleId(records.getId());
            assertFalse(after.contains(removed), "Seed must not overwrite admin-edited grants");

            fpt.capstone.entity.Permission restore = fpt.capstone.entity.Permission.builder()
                    .role(records)
                    .right(rightRepository.findById(removed).orElseThrow())
                    .build();
            permissionRepository.save(restore);
            permissionCacheService.evictAll();
            assertEquals(new HashSet<>(before), permissionRepository.findRightIdsByRoleId(records.getId()));
        }
    }

    @Nested
    @DisplayName("AC13 - Admin is purely administrative")
    class AdminScope {

        @Test
        void adminRole_shouldHoldExactly18BaseRights() {
            Role admin = roleRepository.findByName("Admin").orElseThrow();
            assertEquals(18, permissionRepository.countByRoleId(admin.getId()));
        }

        @Test
        void adminAccount_shouldGet403OnBusinessApisAndBeAudited() throws Exception {
            MvcResult admin = registerUserWithRole("Admin");
            Cookie token = cookieOf(admin, "access_token");
            String adminId = userIdFromBody(admin);

            mockMvc.perform(get("/applications").cookie(token))
                    .andExpect(status().isForbidden());
            mockMvc.perform(get("/benificiary").cookie(token))
                    .andExpect(status().isForbidden());

            boolean audited = systemLogRepository.findAll().stream()
                    .anyMatch(log -> "ILLEGAL_REQUEST".equals(log.getAction())
                            && adminId.equals(log.getUserId()));
            assertTrue(audited, "403s must be logged as ILLEGAL_REQUEST");
        }

        @Test
        void adminAccount_shouldStillReachAdministrationApis() throws Exception {
            MvcResult admin = registerUserWithRole("Admin");
            Cookie token = cookieOf(admin, "access_token");

            mockMvc.perform(get("/admin/roles").cookie(token)).andExpect(status().isOk());
            mockMvc.perform(get("/admin/users").cookie(token)).andExpect(status().isOk());
            mockMvc.perform(get("/admin/logs").cookie(token)).andExpect(status().isOk());
        }
    }
}
