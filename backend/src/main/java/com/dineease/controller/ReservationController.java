package com.dineease.controller;

import com.dineease.dto.ReservationRequest;
import com.dineease.dto.StatsResponse;
import com.dineease.model.Reservation;
import com.dineease.service.ReservationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<Reservation>> getAllReservations() {
        return ResponseEntity.ok(reservationService.getAllReservations());
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<List<Reservation>> getMyBookings(@RequestParam("email") String email) {
        return ResponseEntity.ok(reservationService.getReservationsByEmail(email));
    }

    @PostMapping
    public ResponseEntity<Reservation> bookTable(@Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.bookTable(request));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<Reservation> updateStatus(
            @PathVariable @NonNull Long id,
            @RequestParam("status") String status) {
        return ResponseEntity.ok(reservationService.updateReservationStatus(id, status));
    }

    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getStats() {
        return ResponseEntity.ok(reservationService.getStats());
    }
}
