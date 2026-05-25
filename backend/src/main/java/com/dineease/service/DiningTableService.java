package com.dineease.service;

import com.dineease.exception.ResourceNotFoundException;
import com.dineease.model.DiningTable;
import com.dineease.model.Reservation;
import com.dineease.repository.DiningTableRepository;
import com.dineease.repository.ReservationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class DiningTableService {

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    public List<DiningTable> getAllTables() {
        return diningTableRepository.findAll();
    }

    public DiningTable getTableById(Long id) {
        return diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));
    }

    public DiningTable createTable(DiningTable table) {
        if (diningTableRepository.findByTableNumber(table.getTableNumber()).isPresent()) {
            throw new RuntimeException("Table number " + table.getTableNumber() + " already exists");
        }
        return diningTableRepository.save(table);
    }

    public DiningTable updateTable(Long id, DiningTable tableDetails) {
        DiningTable table = getTableById(id);
        
        // If table number is changing, verify the new number is not taken
        if (!table.getTableNumber().equals(tableDetails.getTableNumber())) {
            if (diningTableRepository.findByTableNumber(tableDetails.getTableNumber()).isPresent()) {
                throw new RuntimeException("Table number " + tableDetails.getTableNumber() + " already exists");
            }
        }
        
        table.setTableNumber(tableDetails.getTableNumber());
        table.setCapacity(tableDetails.getCapacity());
        table.setStatus(tableDetails.getStatus());
        
        return diningTableRepository.save(table);
    }

    public void deleteTable(Long id) {
        DiningTable table = getTableById(id);
        diningTableRepository.delete(table);
    }

    // Dynamic search for available tables for booking
    public List<DiningTable> getAvailableTables(LocalDate date, LocalTime time, Integer guestCount) {
        // 1. Get all tables that can fit the party size
        List<DiningTable> suitableTables = diningTableRepository.findByCapacityGreaterThanEqual(guestCount);
        List<DiningTable> availableTables = new ArrayList<>();

        for (DiningTable table : suitableTables) {
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
