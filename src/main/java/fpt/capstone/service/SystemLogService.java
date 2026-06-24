package fpt.capstone.service;

import fpt.capstone.entity.SystemLog;

import java.time.LocalDate;
import java.util.List;

public interface SystemLogService {

    SystemLog write(SystemLog systemLog);

    List<SystemLog> read();
}
