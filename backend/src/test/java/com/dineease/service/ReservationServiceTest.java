package com.dineease.service;

import com.dineease.dto.ReservationRequest;
import com.dineease.dto.StatsResponse;
import com.dineease.exception.TableNotAvailableException;
import com.dineease.model.DiningTable;
import com.dineease.model.Reservation;
import com.dineease.repository.DiningTableRepository;
import com.dineease.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private DiningTableRepository diningTableRepository;

    @InjectMocks
    private ReservationService reservationService;

    private DiningTable table;
    private ReservationRequest validRequest;
    private Reservation existingReservation;

    @BeforeEach
    void setUp() {
        table = new DiningTable();
        table.setId(1L);
        table.setTableNumber(1);
        table.setCapacity(4);
        table.setStatus("AVAILABLE");

        validRequest = new ReservationRequest();
        validRequest.setDiningTableId(1L);
        validRequest.setCustomerName("Alice");
        validRequest.setCustomerEmail("alice@example.com");
        validRequest.setCustomerPhone("1234567890");
        validRequest.setReservationDate(LocalDate.now().plusDays(2)); // safe in future
        validRequest.setReservationTime(LocalTime.of(19, 0));
        validRequest.setNumberOfGuests(2);
        validRequest.setSpecialRequests("Window seat");

        existingReservation = new Reservation();
        existingReservation.setId(10L);
        existingReservation.setDiningTable(table);
        existingReservation.setCustomerName("Bob");
        existingReservation.setCustomerEmail("bob@example.com");
        existingReservation.setCustomerPhone("0987654321");
        existingReservation.setReservationDate(LocalDate.now().plusDays(2));
        existingReservation.setReservationTime(LocalTime.of(18, 0)); // 1 hour overlap potential
        existingReservation.setNumberOfGuests(3);
        existingReservation.setStatus("PENDING");
    }

    @Test
    void getAllReservations_Success() {
        when(reservationRepository.findAll()).thenReturn(List.of(existingReservation));
        List<Reservation> list = reservationService.getAllReservations();
        assertEquals(1, list.size());
    }

    @Test
    void getReservationsByEmail_Success() {
        when(reservationRepository.findByCustomerEmailOrderByReservationDateDescReservationTimeDesc("bob@example.com"))
                .thenReturn(List.of(existingReservation));
        List<Reservation> list = reservationService.getReservationsByEmail("bob@example.com");
        assertEquals(1, list.size());
    }

    @Test
    void bookTable_Success() {
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.findActiveReservationsForTableAndDate(1L, validRequest.getReservationDate()))
                .thenReturn(Collections.emptyList());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation result = reservationService.bookTable(validRequest);

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals("Alice", result.getCustomerName());
        verify(reservationRepository, times(1)).save(any(Reservation.class));
    }

    @Test
    void bookTable_InvalidGuests_ThrowsException() {
        validRequest.setNumberOfGuests(0);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            reservationService.bookTable(validRequest);
        });
        assertTrue(ex.getMessage().contains("Number of guests must be at least 1"));
    }

    @Test
    void bookTable_PastDateTime_ThrowsException() {
        validRequest.setReservationDate(LocalDate.now().minusDays(1));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            reservationService.bookTable(validRequest);
        });
        assertTrue(ex.getMessage().contains("must be in the future"));
    }

    @Test
    void bookTable_TableBlocked_ThrowsException() {
        table.setStatus("RESERVED"); // Blocked table
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table));

        TableNotAvailableException ex = assertThrows(TableNotAvailableException.class, () -> {
            reservationService.bookTable(validRequest);
        });
        assertTrue(ex.getMessage().contains("blocked or unavailable"));
    }

    @Test
    void bookTable_CapacityExceeded_ThrowsException() {
        validRequest.setNumberOfGuests(5); // Capacity is 4
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            reservationService.bookTable(validRequest);
        });
        assertTrue(ex.getMessage().contains("capacity is smaller than guest size"));
    }

    @Test
    void bookTable_DoubleBookingOverlap_ThrowsException() {
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table));
        when(reservationRepository.findActiveReservationsForTableAndDate(1L, validRequest.getReservationDate()))
                .thenReturn(List.of(existingReservation));

        TableNotAvailableException ex = assertThrows(TableNotAvailableException.class, () -> {
            reservationService.bookTable(validRequest);
        });
        assertTrue(ex.getMessage().contains("is already reserved"));
    }

    @Test
    void updateReservationStatus_Success() {
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(existingReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reservation updated = reservationService.updateReservationStatus(10L, "CONFIRMED");

        assertEquals("CONFIRMED", updated.getStatus());
    }

    @Test
    void updateReservationStatus_OverlapOnConfirm_ThrowsException() {
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(existingReservation));
        
        // Let's create an overlapping confirmed reservation
        Reservation overlapping = new Reservation();
        overlapping.setId(20L);
        overlapping.setDiningTable(table);
        overlapping.setReservationDate(existingReservation.getReservationDate());
        overlapping.setReservationTime(existingReservation.getReservationTime().plusMinutes(30)); // 30 mins later
        overlapping.setStatus("CONFIRMED");

        when(reservationRepository.findActiveReservationsForTableAndDate(table.getId(), existingReservation.getReservationDate()))
                .thenReturn(List.of(overlapping));

        TableNotAvailableException ex = assertThrows(TableNotAvailableException.class, () -> {
            reservationService.updateReservationStatus(10L, "CONFIRMED");
        });
        assertTrue(ex.getMessage().contains("is already reserved and confirmed"));
    }

    @Test
    void getStats_Success() {
        when(diningTableRepository.count()).thenReturn(10L);
        when(reservationRepository.countByReservationDateAndStatus(any(LocalDate.class), eq("CONFIRMED"))).thenReturn(3L);
        when(reservationRepository.countByStatus("PENDING")).thenReturn(5L);
        when(reservationRepository.count()).thenReturn(20L);

        StatsResponse stats = reservationService.getStats();

        assertEquals(10, stats.getTotalTables());
        assertEquals(3, stats.getActiveReservationsToday());
        assertEquals(5, stats.getPendingReservationsCount());
        assertEquals(20, stats.getTotalReservations());
    }
}
