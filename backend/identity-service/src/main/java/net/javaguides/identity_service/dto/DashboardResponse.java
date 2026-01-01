package net.javaguides.identity_service.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class DashboardResponse {
    private long totalUsers;
    private long loginToday;
    private long onlineUsers;
    private List<LoginChartProjection> chart;

    public DashboardResponse(long totalUsers, long loginToday, long onlineUsers, List<LoginChartProjection> chart) {
        this.totalUsers = totalUsers;
        this.loginToday = loginToday;
        this.onlineUsers = onlineUsers;
        this.chart = chart;
    }
}
