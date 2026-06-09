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

/**
 * Service class handling all dining table CRUD management and availability lookup logic.
 */
@Service
@Transactional
public class DiningTableService {

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    /**
     * Retrieves all dining tables in the database.
     */
    public List<DiningTable> getAllTables() {
        return diningTableRepository.findAll();
    }

    /**
     * Finds a single dining table by its database ID, throwing ResourceNotFoundException if not found.
     */
    public DiningTable getTableById(@NonNull Long id) {
        return diningTableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Table not found with id: " + id));
    }

    /**
     * Creates a new dining table entity with table number validation, capacity checking, and uniqueness checking.
     * 
     * @param table DiningTable object to create.
     * @return Saved DiningTable entity.
     */
    public DiningTable createTable(DiningTable table) {
        // [DEBUG BREAKPOINT: Inspect table properties (table number, capacity, status)]
        if (table.getTableNumber() == null || table.getTableNumber() <= 0) {
            // [DEBUG BREAKPOINT: Break here if table number is missing or non-positive]
            throw new IllegalArgumentException("Table number must be positive");
        }
        if (table.getCapacity() == null || table.getCapacity() <= 0) {
            // [DEBUG BREAKPOINT: Break here if capacity is missing or less than 1]
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        if (diningTableRepository.findByTableNumber(table.getTableNumber()).isPresent()) {
            // [DEBUG BREAKPOINT: Break here if the table number is already taken by another table]
            throw new IllegalArgumentException("Table number " + table.getTableNumber() + " already exists");
        }
        
        // Normalize status value
        if (table.getStatus() == null) {
            table.setStatus("AVAILABLE");
        } else {
            String upperStatus = table.getStatus().toUpperCase();
            if (!upperStatus.equals("AVAILABLE") && !upperStatus.equals("RESERVED")) {
                // [DEBUG BREAKPOINT: Break here if status is neither AVAILABLE nor RESERVED]
                throw new IllegalArgumentException("Invalid table status: " + table.getStatus());
            }
            table.setStatus(upperStatus);
        }
        return diningTableRepository.save(table);
    }

    /**
     * Updates an existing dining table details.
     * 
     * @param id ID of the dining table to update.
     * @param tableDetails New details to apply.
     * @return Updated and saved DiningTable entity.
     */
    public DiningTable updateTable(@NonNull Long id, DiningTable tableDetails) {
        // [DEBUG BREAKPOINT: Inspect ID and table details for update]
        if (tableDetails.getTableNumber() == null || tableDetails.getTableNumber() <= 0) {
            // [DEBUG BREAKPOINT: Break here if table number update value is invalid]
            throw new IllegalArgumentException("Table number must be positive");
        }
        if (tableDetails.getCapacity() == null || tableDetails.getCapacity() <= 0) {
            // [DEBUG BREAKPOINT: Break here if capacity update value is invalid]
            throw new IllegalArgumentException("Capacity must be at least 1");
        }
        DiningTable table = getTableById(id);
        
        // If table number is changing, verify the new number is not taken
        if (!table.getTableNumber().equals(tableDetails.getTableNumber())) {
            if (diningTableRepository.findByTableNumber(tableDetails.getTableNumber()).isPresent()) {
                // [DEBUG BREAKPOINT: Break here if the updated table number conflicts with an existing table]
                throw new IllegalArgumentException("Table number " + tableDetails.getTableNumber() + " already exists");
            }
        }
        
        if (tableDetails.getStatus() != null) {
            String upperStatus = tableDetails.getStatus().toUpperCase();
            if (!upperStatus.equals("AVAILABLE") && !upperStatus.equals("RESERVED")) {
                // [DEBUG BREAKPOINT: Break here if updated status string is invalid]
                throw new IllegalArgumentException("Invalid table status: " + tableDetails.getStatus());
            }
            table.setStatus(upperStatus);
        }
        
        table.setTableNumber(tableDetails.getTableNumber());
        table.setCapacity(tableDetails.getCapacity());
        
        return diningTableRepository.save(table);
    }

    /**
     * Deletes a table if it does not have any associated active or past reservations.
     * 
     * @param id Database ID of the table to delete.
     */
    @SuppressWarnings("null")
    public void deleteTable(@NonNull Long id) {
        // [DEBUG BREAKPOINT: Inspect table ID to be deleted]
        DiningTable table = getTableById(id);
        
        // Prevent deletion if table has reservations
        if (reservationRepository.existsByDiningTableId(id)) {
            // [DEBUG BREAKPOINT: Break here if table deletion is aborted due to existing reservations]
            throw new IllegalArgumentException("Cannot delete table because it has associated reservations. Please cancel or delete the reservations first.");
        }
        
        diningTableRepository.delete(table);
    }

    /**
     * Dynamic search for available tables suitable for a specific guest count at a requested date and time.
     * Filters out:
     * 1. Tables with capacity smaller than guest count.
     * 2. Tables blocked/manually set to RESERVED by admin.
     * 3. Tables with overlapping active reservations on that date within 2 hours of the requested time.
     */
    public List<DiningTable> getAvailableTables(LocalDate date, LocalTime time, Integer guestCount) {
        // [DEBUG BREAKPOINT: Inspect available table search parameters (date, time, guestCount)]
        
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
