package net.javaguides.identity_service.dto;

import java.time.LocalDate;

public interface LoginChartProjection {
    LocalDate getDate();
    Long getCount();
}
