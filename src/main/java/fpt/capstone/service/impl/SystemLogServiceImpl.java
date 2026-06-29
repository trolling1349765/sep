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
        return systemLogRepository.save(systemLog);
    }

    @Override
    public List<SystemLog> read() {
        List<SystemLog> systemLogs = systemLogRepository.findAll();
        return systemLogs;
    }
}
