package net.javaguides.identity_service.controller;

import lombok.RequiredArgsConstructor;
import net.javaguides.common_lib.dto.ApiResponse;
import net.javaguides.identity_service.dto.DashboardResponse;
import net.javaguides.identity_service.dto.LoginResponse;
import net.javaguides.identity_service.service.DashboardService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getDashboard() {
        DashboardResponse dashboardResponse = dashboardService.getDashboard();
        ApiResponse<DashboardResponse> apiResponse = new ApiResponse<>(dashboardResponse, HttpStatus.OK.value());
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }
}
