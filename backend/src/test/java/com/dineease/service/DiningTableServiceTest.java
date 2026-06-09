package com.dineease.service;

import com.dineease.exception.ResourceNotFoundException;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
public class DiningTableServiceTest {

    @Mock
    private DiningTableRepository diningTableRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @InjectMocks
    private DiningTableService diningTableService;

    private DiningTable table1;
    private DiningTable table2;

    @BeforeEach
    void setUp() {
        table1 = new DiningTable();
        table1.setId(1L);
        table1.setTableNumber(1);
        table1.setCapacity(4);
        table1.setStatus("AVAILABLE");

        table2 = new DiningTable();
        table2.setId(2L);
        table2.setTableNumber(2);
        table2.setCapacity(6);
        table2.setStatus("AVAILABLE");
    }

    @Test
    void getAllTables_Success() {
        List<DiningTable> expected = List.of(table1, table2);
        when(diningTableRepository.findAll()).thenReturn(expected);

        List<DiningTable> actual = diningTableService.getAllTables();

        assertEquals(2, actual.size());
        verify(diningTableRepository, times(1)).findAll();
    }

    @Test
    void getTableById_Success() {
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table1));

        DiningTable actual = diningTableService.getTableById(1L);

        assertNotNull(actual);
        assertEquals(1, actual.getTableNumber());
    }

    @Test
    void getTableById_NotFound_ThrowsException() {
        when(diningTableRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            diningTableService.getTableById(99L);
        });

        assertEquals("Table not found with id: 99", exception.getMessage());
    }

    @Test
    void createTable_Success() {
        when(diningTableRepository.findByTableNumber(table1.getTableNumber())).thenReturn(Optional.empty());
        when(diningTableRepository.save(any(DiningTable.class))).thenReturn(table1);

        DiningTable created = diningTableService.createTable(table1);

        assertNotNull(created);
        assertEquals(1, created.getTableNumber());
        verify(diningTableRepository, times(1)).save(table1);
    }

    @Test
    void createTable_InvalidTableNumber_ThrowsException() {
        table1.setTableNumber(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            diningTableService.createTable(table1);
        });

        assertEquals("Table number must be positive", exception.getMessage());
    }

    @Test
    void createTable_DuplicateTableNumber_ThrowsException() {
        when(diningTableRepository.findByTableNumber(table1.getTableNumber())).thenReturn(Optional.of(table2));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            diningTableService.createTable(table1);
        });

        assertEquals("Table number 1 already exists", exception.getMessage());
    }

    @Test
    void createTable_InvalidStatus_ThrowsException() {
        table1.setStatus("UNKNOWN");
        when(diningTableRepository.findByTableNumber(table1.getTableNumber())).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            diningTableService.createTable(table1);
        });

        assertEquals("Invalid table status: UNKNOWN", exception.getMessage());
    }

    @Test
    void updateTable_Success() {
        DiningTable updatedDetails = new DiningTable(null, 1, 8, "RESERVED");
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table1));
        when(diningTableRepository.save(any(DiningTable.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DiningTable updated = diningTableService.updateTable(1L, updatedDetails);

        assertNotNull(updated);
        assertEquals(8, updated.getCapacity());
        assertEquals("RESERVED", updated.getStatus());
    }

    @Test
    void updateTable_DuplicateTableNumber_ThrowsException() {
        DiningTable updatedDetails = new DiningTable(null, 2, 8, "AVAILABLE");
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table1));
        when(diningTableRepository.findByTableNumber(2)).thenReturn(Optional.of(table2));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            diningTableService.updateTable(1L, updatedDetails);
        });

        assertEquals("Table number 2 already exists", exception.getMessage());
    }

    @Test
    void deleteTable_Success() {
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table1));
        when(reservationRepository.existsByDiningTableId(1L)).thenReturn(false);

        diningTableService.deleteTable(1L);

        verify(diningTableRepository, times(1)).delete(table1);
    }

    @Test
    void deleteTable_WithReservations_ThrowsException() {
        when(diningTableRepository.findById(1L)).thenReturn(Optional.of(table1));
        when(reservationRepository.existsByDiningTableId(1L)).thenReturn(true);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            diningTableService.deleteTable(1L);
        });

        assertTrue(exception.getMessage().contains("Cannot delete table because it has associated reservations"));
        verify(diningTableRepository, never()).delete(any(DiningTable.class));
    }

    @Test
    void getAvailableTables_Success() {
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime time = LocalTime.of(19, 0);
        Integer guestCount = 4;

        // Suitable capacity
        List<DiningTable> suitable = List.of(table1, table2);
        when(diningTableRepository.findByCapacityGreaterThanEqual(guestCount)).thenReturn(suitable);
        
        // Active reservations for table1 (no overlap)
        Reservation r1 = new Reservation();
        r1.setId(10L);
        r1.setReservationTime(LocalTime.of(16, 0)); // 3 hours difference
        when(reservationRepository.findActiveReservationsForTableAndDate(table1.getId(), date))
                .thenReturn(List.of(r1));

        // Active reservations for table2 (overlapping)
        Reservation r2 = new Reservation();
        r2.setId(11L);
        r2.setReservationTime(LocalTime.of(18, 0)); // 1 hour difference -> overlap!
        when(reservationRepository.findActiveReservationsForTableAndDate(table2.getId(), date))
                .thenReturn(List.of(r2));

        List<DiningTable> available = diningTableService.getAvailableTables(date, time, guestCount);

        assertEquals(1, available.size());
        assertEquals(table1.getId(), available.get(0).getId());
    }
}
