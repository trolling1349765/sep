package fpt.capstone.service.impl;

import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SystemLogResponse;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.entity.User;
import fpt.capstone.enums.ErrorCode;
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
                    () -> systemLogService.search(null, null, null, null, null, -1, 20));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
        }

        @Test
        void search_sizeZero_throwsBadRequest() {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> systemLogService.search(null, null, null, null, null, 0, 0));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        }

        @Test
        void search_fromAfterTo_throwsBadRequest() {
            LocalDateTime from = LocalDateTime.of(2026, 7, 19, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 1, 1, 0, 0);

            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> systemLogService.search(null, null, null, from, to, 0, 20));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals(ErrorCode.ARGUMENT_INVALID.name(), ex.getReason());
            verifyNoInteractions(systemLogRepository);
        }

        @Test
        void search_sizeOverMax_clampedToHundred() {
            when(systemLogRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            systemLogService.search(null, null, null, null, null, 0, 1000);

            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(systemLogRepository).search(isNull(), isNull(), isNull(), isNull(), isNull(),
                    pageableCaptor.capture());
            assertEquals(100, pageableCaptor.getValue().getPageSize());
        }

        @Test
        void search_filtersPassedThrough() {
            LocalDateTime from = LocalDateTime.of(2026, 1, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2026, 7, 19, 0, 0);
            when(systemLogRepository.search("USER_LOGIN", "USERS", "u1", from, to, PageRequest.of(2, 50)))
                    .thenReturn(Page.empty());

            systemLogService.search("USER_LOGIN", "USERS", "u1", from, to, 2, 50);

            verify(systemLogRepository).search("USER_LOGIN", "USERS", "u1", from, to, PageRequest.of(2, 50));
        }

        @Test
        void search_mapsEntityToResponseAndResolvesActorNamesInOneBatch() {
            List<SystemLog> logs = List.of(fullLog(3, "u1"), fullLog(2, "u2"), fullLog(1, null));
            when(systemLogRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(logs, PageRequest.of(0, 20), 135));
            when(userRepository.findAllById(List.of("u1", "u2")))
                    .thenReturn(List.of(user("u1", "Hoàng Minh Demo")));

            PageResponse<SystemLogResponse> page = systemLogService.search(null, null, null, null, null, 0, 20);

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
            assertEquals(LocalDateTime.of(2026, 7, 19, 4, 38, 12), first.getCreatedAt());

            // unknown user -> null, null userId -> null
            assertNull(page.getItems().get(1).getActorName());
            assertNull(page.getItems().get(2).getActorName());
        }

        @Test
        void search_emptyPage_skipsUserLookup() {
            when(systemLogRepository.search(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                    .thenReturn(Page.empty());

            PageResponse<SystemLogResponse> page = systemLogService.search(null, null, null, null, null, 0, 20);

            assertTrue(page.getItems().isEmpty());
            verify(userRepository, never()).findAllById(any());
        }
    }
}
