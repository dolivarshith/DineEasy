package com.dineease.repository;

import com.dineease.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    
    List<Reservation> findByCustomerEmailOrderByReservationDateDescReservationTimeDesc(String customerEmail);
    
    List<Reservation> findByReservationDate(LocalDate reservationDate);
    
    @Query("SELECT r FROM Reservation r WHERE r.diningTable.id = :tableId AND r.reservationDate = :date AND r.status <> 'CANCELLED'")
    List<Reservation> findActiveReservationsForTableAndDate(
        @Param("tableId") Long tableId, 
        @Param("date") LocalDate date
    );

    long countByStatus(String status);
    
    long countByReservationDateAndStatus(LocalDate date, String status);

    boolean existsByDiningTableId(Long diningTableId);
}
