package fpt.capstone.service;

import fpt.capstone.dto.response.DashboardResponse;

public interface DashboardService {

    DashboardResponse getDashboard(int recentSize);
}
