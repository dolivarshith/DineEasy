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
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
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
        // 1. Find table
        DiningTable table = diningTableRepository.findById(request.getDiningTableId())
                .orElseThrow(() -> new ResourceNotFoundException("Dining table not found with id: " + request.getDiningTableId()));

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

    public Reservation updateReservationStatus(Long id, String status) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found with id: " + id));

        reservation.setStatus(status.toUpperCase());
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
