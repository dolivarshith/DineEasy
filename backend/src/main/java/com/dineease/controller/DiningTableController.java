package com.dineease.controller;

import com.dineease.model.DiningTable;
import com.dineease.service.DiningTableService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/tables")
public class DiningTableController {

    @Autowired
    private DiningTableService diningTableService;

    @GetMapping
    public ResponseEntity<List<DiningTable>> getAllTables() {
        return ResponseEntity.ok(diningTableService.getAllTables());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DiningTable> getTableById(@PathVariable @NonNull Long id) {
        return ResponseEntity.ok(diningTableService.getTableById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<DiningTable>> searchAvailableTables(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("time") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam("guestCount") Integer guestCount) {
        return ResponseEntity.ok(diningTableService.getAvailableTables(date, time, guestCount));
    }

    @PostMapping
    public ResponseEntity<DiningTable> createTable(@Valid @RequestBody DiningTable table) {
        return ResponseEntity.ok(diningTableService.createTable(table));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DiningTable> updateTable(@PathVariable @NonNull Long id, @Valid @RequestBody DiningTable tableDetails) {
        return ResponseEntity.ok(diningTableService.updateTable(id, tableDetails));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTable(@PathVariable @NonNull Long id) {
        diningTableService.deleteTable(id);
        return ResponseEntity.noContent().build();
    }
}
