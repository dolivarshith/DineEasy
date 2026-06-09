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

@Service
@Transactional
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private DiningTableRepository diningTableRepository;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public List<Reservation> getReservationsByEmail(String email) {
        return reservationRepository.findByCustomerEmailOrderByReservationDateDescReservationTimeDesc(email);
    }

    public Reservation bookTable(ReservationRequest request) {
        if (request.getNumberOfGuests() == null || request.getNumberOfGuests() < 1) {
            throw new IllegalArgumentException("Number of guests must be at least 1.");
        }
        if (request.getReservationDate() == null || request.getReservationTime() == null) {
            throw new IllegalArgumentException("Reservation date and time are required.");
        }

        // Validate that reservation date & time is in the future
        LocalDateTime reservationDateTime = LocalDateTime.of(request.getReservationDate(), request.getReservationTime());
        if (reservationDateTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reservation date and time must be in the future.");
        }

        Long rawTableId = request.getDiningTableId();
        if (rawTableId == null) {
            throw new IllegalArgumentException("Dining table ID is required.");
        }
        long tableId = rawTableId;

        // 1. Find table
        DiningTable table = diningTableRepository.findById(tableId)
                .orElseThrow(() -> new ResourceNotFoundException("Dining table not found with id: " + tableId));

        // Check if table is available (not force-blocked by admin)
        if (!"AVAILABLE".equalsIgnoreCase(table.getStatus())) {
            throw new TableNotAvailableException("Table " + table.getTableNumber() + " is currently blocked or unavailable.");
        }

        // 2. Check capacity
        if (table.getCapacity() < request.getNumberOfGuests()) {
            throw new IllegalArgumentException("Selected table capacity is smaller than guest size.");
        }

        // 3. Double-booking check
        List<Reservation> activeReservations = reservationRepository
                .findActiveReservationsForTableAndDate(table.getId(), request.getReservationDate());

        for (Reservation res : activeReservations) {
            long minutesDiff = Duration.between(res.getReservationTime(), request.getReservationTime()).abs().toMinutes();
            if (minutesDiff < 120) {
                throw new TableNotAvailableException("Table " + table.getTableNumber() + " is already reserved at " + res.getReservationTime() + " on " + request.getReservationDate());
            }
        }

        // 4. Save reservation
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

    public Reservation updateReservationStatus(@NonNull Long id, String status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        String upperStatus = status.toUpperCase();
        if (!upperStatus.equals("PENDING") && !upperStatus.equals("CONFIRMED") && 
            !upperStatus.equals("CANCELLED") && !upperStatus.equals("COMPLETED")) {
            throw new IllegalArgumentException("Invalid reservation status: " + status);
        }

        if ("CONFIRMED".equals(upperStatus)) {
            DiningTable table = reservation.getDiningTable();
            // Check if table is available (not force-blocked by admin)
            if (!"AVAILABLE".equalsIgnoreCase(table.getStatus())) {
                throw new TableNotAvailableException("Table " + table.getTableNumber() + " is currently blocked or unavailable.");
            }

            // Check if there is any overlapping CONFIRMED or COMPLETED reservation
            List<Reservation> activeReservations = reservationRepository
                    .findActiveReservationsForTableAndDate(table.getId(), reservation.getReservationDate());

            for (Reservation res : activeReservations) {
                if (res.getId().equals(id)) {
                    continue; // Skip self
                }
                if ("CONFIRMED".equals(res.getStatus()) || "COMPLETED".equals(res.getStatus())) {
                    long minutesDiff = Duration.between(res.getReservationTime(), reservation.getReservationTime()).abs().toMinutes();
                    if (minutesDiff < 120) {
                        throw new TableNotAvailableException("Table " + table.getTableNumber() + " is already reserved and confirmed at " + res.getReservationTime() + " on " + reservation.getReservationDate());
                    }
                }
            }
        }

        reservation.setStatus(upperStatus);
        return reservationRepository.save(reservation);
    }

    public StatsResponse getStats() {
        long totalTables = diningTableRepository.count();
        long activeToday = reservationRepository.countByReservationDateAndStatus(LocalDate.now(), "CONFIRMED");
        long pending = reservationRepository.countByStatus("PENDING");
        long totalReservations = reservationRepository.count();

        return new StatsResponse(totalTables, activeToday, pending, totalReservations);
    }
}
