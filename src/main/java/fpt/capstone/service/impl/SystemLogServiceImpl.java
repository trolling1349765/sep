package fpt.capstone.service.impl;

import fpt.capstone.entity.SystemLog;
import fpt.capstone.repository.SystemLogRepository;
import fpt.capstone.service.SystemLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SystemLogServiceImpl implements SystemLogService {

    private SystemLogRepository systemLogRepository;

    SystemLogServiceImpl(SystemLogRepository systemLogRepository) {
        this.systemLogRepository = systemLogRepository;
    }

    @Override
    // Own transaction: audit rows survive a business rollback (login failures,
    // 403 ILLEGAL_REQUEST) and a failed write never rolls back the business op.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SystemLog write(SystemLog systemLog) {
        return systemLogRepository.save(systemLog);
    }

    @Override
    public List<SystemLog> read() {
        List<SystemLog> systemLogs = systemLogRepository.findAll();
        return systemLogs;
    }
}
