package com.dineease.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StatsResponse {
    private long totalTables;
    private long activeReservationsToday;
    private long pendingReservationsCount;
    private long totalReservations;
}
