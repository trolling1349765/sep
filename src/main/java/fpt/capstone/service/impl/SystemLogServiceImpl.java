package fpt.capstone.service.impl;

import fpt.capstone.entity.SystemLog;
import fpt.capstone.repository.SystemLogRepository;
import fpt.capstone.service.SystemLogService;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class SystemLogServiceImpl implements SystemLogService {

    private SystemLogRepository systemLogRepository;

    SystemLogServiceImpl(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    @Override
    public SystemLog write(SystemLog systemLog) {
        SystemLog record = new SystemLog();
        systemLog.builder()
                .action(systemLog.getAction())
                .entityType(systemLog.getEntityType())
                .entityId(systemLog.getEntityId())
                .oldValue(systemLog.getOldValue())
                .newValue(systemLog.getNewValue())
                .createdAt(LocalDate.now())
                .build();
        return systemLogRepository.save(record);
    }

    @Override
    public List<SystemLog> read() {
        List<SystemLog> systemLogs = systemLogRepository.findAll();
        return systemLogs;
    }
}
