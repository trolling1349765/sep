package fpt.capstone.service.impl;

import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SystemLogDetailResponse;
import fpt.capstone.dto.response.SystemLogResponse;
import fpt.capstone.entity.Role;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.LogSeverity;
import fpt.capstone.repository.SystemLogRepository;
import fpt.capstone.repository.UserRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SystemLogServiceImplTest {

    @Mock
    private SystemLogRepository systemLogRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SystemLogServiceImpl systemLogService;

    private SystemLog fullLog(int id, String userId) {
        return SystemLog.builder()
                .id(id)
                .userId(userId)
                .action("USER_LOGIN")
                .entityType("USERS")
                .entityId("e-1")
                .oldValue("old")
                .newValue("new")
                .ipAddress("127.0.0.1")
                .createdAt(LocalDateTime.of(2026, 7, 19, 4, 38, 12))
                .build();
    }

    private User user(String id, String name) {
        User u = new User();
        u.setId(id);
        u.setName(name);
        return u;
    }

    /** Default-filter search call: no q, no severity. */
    private PageResponse<SystemLogResponse> searchDefault(int page, int size) {
        return systemLogService.search(null, null, null, null, null, null, null, page, size);
    }

    @Nested
    class Write {

        @Test
        void write_delegatesToRepository() {
            SystemLog log = fullLog(1, "u1");
            when(systemLogRepository.save(log)).thenReturn(log);

            SystemLog saved = systemLogService.write(log);

            assertSame(log, saved);
            verify(systemLogRepository).save(log);
        }
    }

    @Nested
    class Search {

        @Test
        void search_negativePage_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> searchDefault(-1, 20));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void search_sizeZero_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> searchDefault(0, 0));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void search_fromAfterTo_throwsBadRequest() {
            LocalDateTime from = LocalDateTime.of(2026, 7, 19, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 1, 1, 0, 0);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> systemLogService.search(null, null, null, from, to, null, null, 0, 20));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
            verifyNoInteractions(systemLogRepository);
        }

        @Test
        void search_sizeOverMax_clampedToHundred() {
            when(systemLogRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), anyString(), anyList(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            searchDefault(0, 1000);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(systemLogRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), anyString(), anyList(), pageableCaptor.capture());
            assertEquals(100, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void search_filtersPassedThrough() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 7, 19, 0, 0);
            when(systemLogRepository.search(eq("USER_LOGIN"), eq("USERS"), eq("u1"), eq(from), eq(to),
                    isNull(), eq(SystemLogServiceImpl.SEV_NONE), eq(SystemLogServiceImpl.NO_SEV_ACTIONS),
                    eq(PageRequest.of(2, 50))))
                    .thenReturn(Page.empty());

            systemLogService.search("USER_LOGIN", "USERS", "u1", from, to, null, null, 2, 50);

            verify(systemLogRepository).search(eq("USER_LOGIN"), eq("USERS"), eq("u1"), eq(from), eq(to),
                    isNull(), eq(SystemLogServiceImpl.SEV_NONE), eq(SystemLogServiceImpl.NO_SEV_ACTIONS),
                    eq(PageRequest.of(2, 50)));
        }

        @Test
        void search_q_isTrimmedLowercasedEscapedAndWrapped() {
            when(systemLogRepository.search(any(), any(), any(), any(), any(),
                    anyString(), anyString(), anyList(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            systemLogService.search(null, null, null, null, null, "  %Bình_A!B  ", null, 0, 20);

            ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
            verify(systemLogRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    qCaptor.capture(), anyString(), anyList(), any(Pageable.class));
            assertEquals("%!%bình!_a!!b%", qCaptor.getValue());
        }

        @Test
        void search_blankQ_passedAsNull() {
            when(systemLogRepository.search(any(), any(), any(), any(), any(),
                    isNull(), anyString(), anyList(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            systemLogService.search(null, null, null, null, null, "   ", null, 0, 20);

            verify(systemLogRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), eq(SystemLogServiceImpl.SEV_NONE), anyList(), any(Pageable.class));
        }

        @Test
        void search_severityCritical_translatesToInFilter() {
            when(systemLogRepository.search(any(), any(), any(), any(), any(),
                    any(), anyString(), anyList(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            systemLogService.search(null, null, null, null, null, null, "critical", 0, 20);

            ArgumentCaptor<List<String>> actionsCaptor = ArgumentCaptor.forClass(List.class);
            verify(systemLogRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), eq(SystemLogServiceImpl.SEV_IN), actionsCaptor.capture(), any(Pageable.class));
            assertEquals(LogSeverity.criticalActions(), Set.copyOf(actionsCaptor.getValue()));
        }

        @Test
        void search_severityWarning_translatesToInFilter() {
            when(systemLogRepository.search(any(), any(), any(), any(), any(),
                    any(), anyString(), anyList(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            systemLogService.search(null, null, null, null, null, null, "WARNING", 0, 20);

            ArgumentCaptor<List<String>> actionsCaptor = ArgumentCaptor.forClass(List.class);
            verify(systemLogRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), eq(SystemLogServiceImpl.SEV_IN), actionsCaptor.capture(), any(Pageable.class));
            assertEquals(LogSeverity.warningActions(), Set.copyOf(actionsCaptor.getValue()));
        }

        @Test
        void search_severityInfo_translatesToNotInFilterOverBothClosedBuckets() {
            when(systemLogRepository.search(any(), any(), any(), any(), any(),
                    any(), anyString(), anyList(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            systemLogService.search(null, null, null, null, null, null, "INFO", 0, 20);

            ArgumentCaptor<List<String>> actionsCaptor = ArgumentCaptor.forClass(List.class);
            verify(systemLogRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), eq(SystemLogServiceImpl.SEV_NOT_IN), actionsCaptor.capture(), any(Pageable.class));
            assertEquals(LogSeverity.nonInfoActions(), Set.copyOf(actionsCaptor.getValue()));
        }

        @Test
        void search_invalidSeverity_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> systemLogService.search(null, null, null, null, null, null, "FATAL", 0, 20));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
            verifyNoInteractions(systemLogRepository);
        }

        @Test
        void search_mapsEntityToResponseAndResolvesActorNamesInOneBatch() {
            List<SystemLog> logs = List.of(fullLog(3, "u1"), fullLog(2, "u2"), fullLog(1, null));
            when(systemLogRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), anyString(), anyList(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(logs, PageRequest.of(0, 20), 135));
            when(userRepository.findAllById(List.of("u1", "u2")))
                    .thenReturn(List.of(user("u1", "Hoàng Minh Demo")));

            PageResponse<SystemLogResponse> page = searchDefault(0, 20);

            verify(userRepository, times(1)).findAllById(List.of("u1", "u2"));
            assertEquals(0, page.getPage());
            assertEquals(20, page.getSize());
            assertEquals(135, page.getTotalElements());
            assertEquals(7, page.getTotalPages());
            assertEquals(3, page.getItems().size());

            SystemLogResponse first = page.getItems().get(0);
            assertEquals(3, first.getId());
            assertEquals("u1", first.getUserId());
            assertEquals("Hoàng Minh Demo", first.getActorName());
            assertEquals("USER_LOGIN", first.getAction());
            assertEquals("USERS", first.getEntityType());
            assertEquals("e-1", first.getEntityId());
            assertEquals("old", first.getOldValue());
            assertEquals("new", first.getNewValue());
            assertEquals("127.0.0.1", first.getIpAddress());
            assertEquals("INFO", first.getSeverity());
            assertEquals(LocalDateTime.of(2026, 7, 19, 4, 38, 12), first.getCreatedAt());

            // unknown user -> null, null userId -> null
            assertNull(page.getItems().get(1).getActorName());
            assertNull(page.getItems().get(2).getActorName());
        }

        @Test
        void search_emptyPage_skipsUserLookup() {
            when(systemLogRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    isNull(), anyString(), anyList(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            PageResponse<SystemLogResponse> page = searchDefault(0, 20);

            assertTrue(page.getItems().isEmpty());
            verify(userRepository, never()).findAllById(any());
        }
    }

    @Nested
    class GetDetail {

        @Test
        void getDetail_unknownId_throwsNotFound() {
            when(systemLogRepository.findById(99)).thenReturn(Optional.empty());

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> systemLogService.getDetail(99));
            assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
            assertEquals(ErrorCode.LOG_NOT_FOUND.name(), ex.getReason());
        }

        @Test
        void getDetail_resolvesActorNameAndCurrentRole() {
            SystemLog log = fullLog(7, "u1");
            log.setAction("PERMISSION_UPDATE");
            User actor = user("u1", "Trần Bình");
            actor.setRole(Role.builder().name("Admin").build());
            when(systemLogRepository.findById(7)).thenReturn(Optional.of(log));
            when(userRepository.findById("u1")).thenReturn(Optional.of(actor));

            SystemLogDetailResponse detail = systemLogService.getDetail(7);

            assertEquals(7, detail.getId());
            assertEquals("u1", detail.getUserId());
            assertEquals("Trần Bình", detail.getActorName());
            assertEquals("Admin", detail.getActorRole());
            assertEquals("PERMISSION_UPDATE", detail.getAction());
            assertEquals("WARNING", detail.getSeverity());
            assertEquals("127.0.0.1", detail.getIpAddress());
            assertEquals("old", detail.getOldValue());
            assertEquals("new", detail.getNewValue());
        }

        @Test
        void getDetail_actorWithoutRole_roleIsNull() {
            SystemLog log = fullLog(8, "u1");
            when(systemLogRepository.findById(8)).thenReturn(Optional.of(log));
            when(userRepository.findById("u1")).thenReturn(Optional.of(user("u1", "Trần Bình")));

            SystemLogDetailResponse detail = systemLogService.getDetail(8);

            assertEquals("Trần Bình", detail.getActorName());
            assertNull(detail.getActorRole());
        }

        @Test
        void getDetail_deletedActor_nameAndRoleNull() {
            SystemLog log = fullLog(9, "gone");
            when(systemLogRepository.findById(9)).thenReturn(Optional.of(log));
            when(userRepository.findById("gone")).thenReturn(Optional.empty());

            SystemLogDetailResponse detail = systemLogService.getDetail(9);

            assertNull(detail.getActorName());
            assertNull(detail.getActorRole());
        }

        @Test
        void getDetail_nullUserId_skipsUserLookup() {
            SystemLog log = fullLog(10, null);
            when(systemLogRepository.findById(10)).thenReturn(Optional.of(log));

            SystemLogDetailResponse detail = systemLogService.getDetail(10);

            assertNull(detail.getActorName());
            assertNull(detail.getActorRole());
            verifyNoInteractions(userRepository);
        }
    }
}
