package com.dineease.service;

import com.dineease.exception.ResourceNotFoundException;
import com.dineease.model.DiningTable;
import com.dineease.model.Reservation;
import com.dineease.repository.DiningTableRepository;
import com.dineease.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class DiningTableService {

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    public List<DiningTable> getAllTables() {
        return diningTableRepository.findAll();
    }

    public DiningTable getTableById(@NonNull Long id) {
        return diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));
    }

    public DiningTable createTable(DiningTable table) {
        if (table.getTableNumber() == null || table.getTableNumber() <= 0) {
            throw new IllegalArgumentException("Table number must be positive");
        }
        if (table.getCapacity() == null || table.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        if (diningTableRepository.findByTableNumber(table.getTableNumber()).isPresent()) {
            throw new IllegalArgumentException("Table number " + table.getTableNumber() + " already exists");
        }
        if (table.getStatus() == null) {
            table.setStatus("AVAILABLE");
        } else {
            String upperStatus = table.getStatus().toUpperCase();
            if (!upperStatus.equals("AVAILABLE") && !upperStatus.equals("RESERVED")) {
                throw new IllegalArgumentException("Invalid table status: " + table.getStatus());
            }
            table.setStatus(upperStatus);
        }
        return diningTableRepository.save(table);
    }

    public DiningTable updateTable(@NonNull Long id, DiningTable tableDetails) {
        if (tableDetails.getTableNumber() == null || tableDetails.getTableNumber() <= 0) {
            throw new IllegalArgumentException("Table number must be positive");
        }
        if (tableDetails.getCapacity() == null || tableDetails.getCapacity() <= 0) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        DiningTable table = getTableById(id);
        
        // If table number is changing, verify the new number is not taken
        if (!table.getTableNumber().equals(tableDetails.getTableNumber())) {
            if (diningTableRepository.findByTableNumber(tableDetails.getTableNumber()).isPresent()) {
                throw new IllegalArgumentException("Table number " + tableDetails.getTableNumber() + " already exists");
            }
        }
        
        if (tableDetails.getStatus() != null) {
            String upperStatus = tableDetails.getStatus().toUpperCase();
            if (!upperStatus.equals("AVAILABLE") && !upperStatus.equals("RESERVED")) {
                throw new IllegalArgumentException("Invalid table status: " + tableDetails.getStatus());
            }
            table.setStatus(upperStatus);
        }
        
        table.setTableNumber(tableDetails.getTableNumber());
        table.setCapacity(tableDetails.getCapacity());
        
        return diningTableRepository.save(table);
    }

    @SuppressWarnings("null")
    public void deleteTable(@NonNull Long id) {
        DiningTable table = getTableById(id);
        
        // Prevent deletion if table has reservations
        if (reservationRepository.existsByDiningTableId(id)) {
            throw new IllegalArgumentException("Cannot delete table because it has associated reservations. Please cancel or delete the reservations first.");
        }
        
        diningTableRepository.delete(table);
    }

    // Dynamic search for available tables for booking
    public List<DiningTable> getAvailableTables(LocalDate date, LocalTime time, Integer guestCount) {
        // 1. Get all tables that can fit the party size
        List<DiningTable> suitableTables = diningTableRepository.findByCapacityGreaterThanEqual(guestCount);
        List<DiningTable> availableTables = new ArrayList<>();

        for (DiningTable table : suitableTables) {
            // Check if table is physically available (not force-blocked by admin)
            if (!"AVAILABLE".equalsIgnoreCase(table.getStatus())) {
                continue;
            }

            // 2. Fetch active reservations on that date
            List<Reservation> activeReservations = reservationRepository
                    .findActiveReservationsForTableAndDate(table.getId(), date);

            boolean isOverlapping = false;
            for (Reservation res : activeReservations) {
                // 3. Overlap check: is time difference less than 2 hours?
                long minutesDiff = Duration.between(res.getReservationTime(), time).abs().toMinutes();
                if (minutesDiff < 120) {
                    isOverlapping = true;
                    break;
                }
            }

            if (!isOverlapping) {
                availableTables.add(table);
            }
        }

        return availableTables;
    }
}
