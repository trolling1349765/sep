package fpt.capstone.service.impl;

import fpt.capstone.repository.PermissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionCacheServiceImplTest {

    @Mock
    private PermissionRepository permissionRepository;

    private long currentTime;
    private PermissionCacheServiceImpl cacheService;

    @BeforeEach
    void setUp() {
        currentTime = 1_000_000L;
        cacheService = new PermissionCacheServiceImpl(permissionRepository) {
            @Override
            protected long now() {
                return currentTime;
            }
        };
    }

    @Nested
    @DisplayName("getRightCodes")
    class GetRightCodes {

        @Test
        void getRightCodes_shouldLoadFromRepositoryOnFirstAccess() {
            when(permissionRepository.findRightCodesByRoleId(1)).thenReturn(Set.of("PROFILE_VIEW"));

            Set<String> codes = cacheService.getRightCodes(1);

            assertEquals(Set.of("PROFILE_VIEW"), codes);
            verify(permissionRepository, times(1)).findRightCodesByRoleId(1);
        }

        @Test
        void getRightCodes_shouldServeFromCacheWithinTtl() {
            when(permissionRepository.findRightCodesByRoleId(1)).thenReturn(Set.of("PROFILE_VIEW"));

            cacheService.getRightCodes(1);
            currentTime += 9 * 60 * 1000L;
            cacheService.getRightCodes(1);

            verify(permissionRepository, times(1)).findRightCodesByRoleId(1);
        }

        @Test
        void getRightCodes_shouldReloadAfterTtlExpires() {
            when(permissionRepository.findRightCodesByRoleId(1))
                    .thenReturn(Set.of("PROFILE_VIEW"))
                    .thenReturn(Set.of("PROFILE_VIEW", "POLICY_VIEW"));

            cacheService.getRightCodes(1);
            currentTime += 11 * 60 * 1000L; // beyond the TTL
            Set<String> reloaded = cacheService.getRightCodes(1);

            assertEquals(Set.of("PROFILE_VIEW", "POLICY_VIEW"), reloaded);
            verify(permissionRepository, times(2)).findRightCodesByRoleId(1);
        }

        @Test
        void getRightCodes_shouldCachePerRole() {
            when(permissionRepository.findRightCodesByRoleId(1)).thenReturn(Set.of("A"));
            when(permissionRepository.findRightCodesByRoleId(2)).thenReturn(Set.of("B"));

            assertEquals(Set.of("A"), cacheService.getRightCodes(1));
            assertEquals(Set.of("B"), cacheService.getRightCodes(2));
        }
    }

    @Nested
    @DisplayName("evictAll")
    class EvictAll {

        @Test
        void evictAll_shouldForceReloadOnNextAccess() {
            when(permissionRepository.findRightCodesByRoleId(1))
                    .thenReturn(Set.of("OLD"))
                    .thenReturn(Set.of("NEW"));

            cacheService.getRightCodes(1);
            cacheService.evictAll();
            Set<String> afterEvict = cacheService.getRightCodes(1);

            assertEquals(Set.of("NEW"), afterEvict);
            verify(permissionRepository, times(2)).findRightCodesByRoleId(1);
        }
    }

    @Nested
    @DisplayName("now (real clock)")
    class RealClock {

        @Test
        void getRightCodes_usesRealSystemClockWhenNotOverridden() {
            // The real bean's now() reads System.currentTimeMillis(); the other
            // tests override now(), so exercise a plain instance here.
            PermissionCacheServiceImpl realService =
                    new PermissionCacheServiceImpl(permissionRepository);
            when(permissionRepository.findRightCodesByRoleId(1)).thenReturn(Set.of("PROFILE_VIEW"));

            Set<String> codes = realService.getRightCodes(1);

            assertEquals(Set.of("PROFILE_VIEW"), codes);
            verify(permissionRepository, times(1)).findRightCodesByRoleId(1);
        }
    }
}
