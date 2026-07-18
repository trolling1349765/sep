package fpt.capstone.service;

import fpt.capstone.dto.response.PageResponse;
import fpt.capstone.dto.response.SystemLogResponse;
import fpt.capstone.entity.SystemLog;

import java.time.LocalDateTime;

public interface SystemLogService {

    SystemLog write(SystemLog systemLog);

    PageResponse<SystemLogResponse> search(String action, String entityType, String userId,
            LocalDateTime from, LocalDateTime to, int page, int size);
}
