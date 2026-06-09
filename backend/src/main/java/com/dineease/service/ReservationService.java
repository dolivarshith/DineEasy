package com.dineease.service;

import com.dineease.dto.ReservationRequest;
import com.dineease.dto.StatsResponse;
import com.dineease.exception.ResourceNotFoundException;
import com.dineease.exception.TableNotAvailableException;
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
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class handling all hotel table booking management logic.
 * Contains critical checks for reservation overlaps, table capacity, status changes, and statistics.
 */
@Service
@Transactional
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private DiningTableRepository diningTableRepository;

    /**
     * Retrieves all reservations stored in the database.
     * Useful for the admin dashboard.
     */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Retrieves reservations associated with a specific customer email, sorted by date and time descending.
     * Used for the "My Bookings" page.
     */
    public List<Reservation> getReservationsByEmail(String email) {
        return reservationRepository.findByCustomerEmailOrderByReservationDateDescReservationTimeDesc(email);
    }

    /**
     * Places a new table booking request. Applies capacity validation and overlapping reservation checks.
     * 
     * @param request Data transfer object containing the reservation details.
     * @return Saved Reservation entity in PENDING status.
     */
    public Reservation bookTable(ReservationRequest request) {
        // [DEBUG BREAKPOINT: Inspect request parameters (guest count, date, time, table ID)]
        if (request.getNumberOfGuests() == null || request.getNumberOfGuests() < 1) {
            // [DEBUG BREAKPOINT: Break here if guest size is invalid]
            throw new IllegalArgumentException("Number of guests must be at least 1.");
        }
        if (request.getReservationDate() == null || request.getReservationTime() == null) {
            // [DEBUG BREAKPOINT: Break here if date or time fields are null]
            throw new IllegalArgumentException("Reservation date and time are required.");
        }

        // Validate that reservation date & time is in the future
        LocalDateTime reservationDateTime = LocalDateTime.of(request.getReservationDate(), request.getReservationTime());
        if (reservationDateTime.isBefore(LocalDateTime.now())) {
            // [DEBUG BREAKPOINT: Break here if booking date/time is in the past]
            throw new IllegalArgumentException("Reservation date and time must be in the future.");
        }

        Long rawTableId = request.getDiningTableId();
        if (rawTableId == null) {
            throw new IllegalArgumentException("Dining table ID is required.");
        }
        long tableId = rawTableId;

        // 1. Find table from repository
        DiningTable table = diningTableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Dining table not found with id: " + tableId));

        // Check if table is available (not force-blocked by admin)
        if (!"AVAILABLE".equalsIgnoreCase(table.getStatus())) {
            // [DEBUG BREAKPOINT: Break here if table is blocked or reserved manually by admin]
            throw new TableNotAvailableException("Table " + table.getTableNumber() + " is currently blocked or unavailable.");
        }

        // 2. Check capacity compatibility
        if (table.getCapacity() < request.getNumberOfGuests()) {
            // [DEBUG BREAKPOINT: Break here if the table capacity is insufficient for the party size]
            throw new IllegalArgumentException("Selected table capacity is smaller than guest size.");
        }

        // 3. Double-booking check: Ensure there is at least a 2-hour buffer between reservations on the same table
        List<Reservation> activeReservations = reservationRepository
                .findActiveReservationsForTableAndDate(table.getId(), request.getReservationDate());

        for (Reservation res : activeReservations) {
            long minutesDiff = Duration.between(res.getReservationTime(), request.getReservationTime()).abs().toMinutes();
            if (minutesDiff < 120) {
                // [DEBUG BREAKPOINT: Break here if there is an overlapping reservation (difference < 120 minutes)]
                throw new TableNotAvailableException("Table " + table.getTableNumber() + " is already reserved at " + res.getReservationTime() + " on " + request.getReservationDate());
            }
        }

        // 4. Save reservation details as PENDING
        Reservation reservation = new Reservation();
        reservation.setDiningTable(table);
        reservation.setCustomerName(request.getCustomerName());
        reservation.setCustomerEmail(request.getCustomerEmail());
        reservation.setCustomerPhone(request.getCustomerPhone());
        reservation.setReservationDate(request.getReservationDate());
        reservation.setReservationTime(request.getReservationTime());
        reservation.setNumberOfGuests(request.getNumberOfGuests());
        reservation.setSpecialRequests(request.getSpecialRequests());
        reservation.setStatus("PENDING"); // Starts as pending, requires admin approval

        return reservationRepository.save(reservation);
    }

    /**
     * Updates the reservation status. Includes special safety validation when confirming a booking.
     * 
     * @param id ID of the reservation to update.
     * @param status The target status ("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED").
     * @return Saved Reservation entity with the updated status.
     */
    public Reservation updateReservationStatus(@NonNull Long id, String status) {
        // [DEBUG BREAKPOINT: Inspect reservation ID and target status]
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        String upperStatus = status.toUpperCase();
        if (!upperStatus.equals("PENDING") && !upperStatus.equals("CONFIRMED") && 
            !upperStatus.equals("CANCELLED") && !upperStatus.equals("COMPLETED")) {
            // [DEBUG BREAKPOINT: Break here if status string is invalid]
            throw new IllegalArgumentException("Invalid reservation status: " + status);
        }

        // Additional overlap check when confirming a booking
        if ("CONFIRMED".equals(upperStatus)) {
            DiningTable table = reservation.getDiningTable();
            // Check if table is available (not force-blocked by admin)
            if (!"AVAILABLE".equalsIgnoreCase(table.getStatus())) {
                // [DEBUG BREAKPOINT: Break here if table is blocked]
                throw new TableNotAvailableException("Table " + table.getTableNumber() + " is currently blocked or unavailable.");
            }

            // Check if there is any overlapping CONFIRMED or COMPLETED reservation
            List<Reservation> activeReservations = reservationRepository
                    .findActiveReservationsForTableAndDate(table.getId(), reservation.getReservationDate());

            for (Reservation res : activeReservations) {
                if (res.getId().equals(id)) {
                    continue; // Skip self comparison
                }
                if ("CONFIRMED".equals(res.getStatus()) || "COMPLETED".equals(res.getStatus())) {
                    long minutesDiff = Duration.between(res.getReservationTime(), reservation.getReservationTime()).abs().toMinutes();
                    if (minutesDiff < 120) {
                        // [DEBUG BREAKPOINT: Break here if there is a conflict with an already CONFIRMED/COMPLETED booking]
                        throw new TableNotAvailableException("Table " + table.getTableNumber() + " is already reserved and confirmed at " + res.getReservationTime() + " on " + reservation.getReservationDate());
                    }
                }
            }
        }

        reservation.setStatus(upperStatus);
        return reservationRepository.save(reservation);
    }

    /**
     * Gathers stats for the Admin Dashboard: total tables, active bookings today, pending bookings count, total bookings.
     */
    public StatsResponse getStats() {
        long totalTables = diningTableRepository.count();
        long activeToday = reservationRepository.countByReservationDateAndStatus(LocalDate.now(), "CONFIRMED");
        long pending = reservationRepository.countByStatus("PENDING");
        long totalReservations = reservationRepository.count();

        return new StatsResponse(totalTables, activeToday, pending, totalReservations);
    }
}
