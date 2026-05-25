package com.dineease.config;

import com.dineease.model.DiningTable;
import com.dineease.model.User;
import com.dineease.repository.DiningTableRepository;
import com.dineease.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        seedUsers();
        seedTables();
    }

    private void seedUsers() {
        if (userRepository.count() == 0) {
            // Admin user
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword("admin123");
            admin.setEmail("admin@dineease.com");
            admin.setRole("ADMIN");
            userRepository.save(admin);

            // Demo Customer user
            User customer = new User();
            customer.setUsername("john");
            customer.setPassword("password");
            customer.setEmail("john@gmail.com");
            customer.setRole("CUSTOMER");
            userRepository.save(customer);

            System.out.println("DatabaseSeeder: Default users seeded (admin/admin123, john/password).");
        }
    }

    private void seedTables() {
        if (diningTableRepository.count() == 0) {
            // 5 Tables of 4 Seats
            diningTableRepository.save(new DiningTable(null, 1, 4, "AVAILABLE"));
            diningTableRepository.save(new DiningTable(null, 2, 4, "AVAILABLE"));
            diningTableRepository.save(new DiningTable(null, 3, 4, "AVAILABLE"));
            diningTableRepository.save(new DiningTable(null, 4, 4, "AVAILABLE"));
            diningTableRepository.save(new DiningTable(null, 5, 4, "AVAILABLE"));

            // 5 Tables of 6 Seats
            diningTableRepository.save(new DiningTable(null, 6, 6, "AVAILABLE"));
            diningTableRepository.save(new DiningTable(null, 7, 6, "AVAILABLE"));
            diningTableRepository.save(new DiningTable(null, 8, 6, "AVAILABLE"));
            diningTableRepository.save(new DiningTable(null, 9, 6, "AVAILABLE"));
            diningTableRepository.save(new DiningTable(null, 10, 6, "AVAILABLE"));

            System.out.println("DatabaseSeeder: 10 dining tables (5 for 4-seating, 5 for 6-seating) seeded successfully.");
        }
    }
}
