package net.javaguides.identity_service.service;

import lombok.RequiredArgsConstructor;
import net.javaguides.identity_service.dto.DashboardResponse;
import net.javaguides.identity_service.dto.LoginChartProjection;
import net.javaguides.identity_service.repository.UserCredentialRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserCredentialRepository userRepository;

    public DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();

        // count active users
        long totalUsers = userRepository.countByDelFlgTrue();
        // count login today
        long loginToday = userRepository.countLoginToday(
                today.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        long onlineUsers = userRepository.countOnlineUsers(
                LocalDateTime.now().minusMinutes(10)
        );

        List<LoginChartProjection> chart =
                userRepository.countLoginByDate(
                        LocalDateTime.now().minusDays(7)
                );

        return new DashboardResponse(
                totalUsers,
                loginToday,
                onlineUsers,
                chart
        );
    }
}
