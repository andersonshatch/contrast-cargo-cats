package com.contrast.dataservice.config;

import com.contrast.dataservice.entity.Address;
import com.contrast.dataservice.entity.Cat;
import com.contrast.dataservice.entity.Shipment;
import com.contrast.dataservice.entity.User;
import com.contrast.dataservice.repository.AddressRepository;
import com.contrast.dataservice.repository.CatRepository;
import com.contrast.dataservice.repository.ShipmentRepository;
import com.contrast.dataservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private CatRepository catRepository;
    
    @Autowired
    private AddressRepository addressRepository;
    
    @Autowired
    private ShipmentRepository shipmentRepository;
    
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) throws Exception {
        // Check if data already exists
        if (userRepository.count() > 0) {
            System.out.println("Sample data already exists, skipping initialization");
            return;
        }

        // Create a default admin user
        User adminUser = createAdminUser();
        
        if (adminUser != null) {
            // Create sample data for the admin user
            createSampleData(adminUser);
        }
    }

    private User createAdminUser() {
        try {
            if (!userRepository.existsByUsername("admin")) {
                User adminUser = new User("admin", passwordEncoder.encode("password123"));
                adminUser = userRepository.save(adminUser);
                System.out.println("Default admin user created with username: admin, password: password123");
                return adminUser;
            } else {
                User adminUser = userRepository.findByUsername("admin").orElse(null);
                System.out.println("Default admin user already exists");
                return adminUser;
            }
        } catch (Exception e) {
            System.err.println("Failed to create admin user: " + e.getMessage());
            return null;
        }
    }

    private void createSampleData(User user) {
        try {
            // Create a sample cat
            Cat cat = new Cat();
            cat.setName("Fluffy");
            cat.setType("Persian");
            cat.setObjOwner(user);
            
            cat = catRepository.save(cat);
            System.out.println("Sample cat 'Fluffy' created successfully with ID: " + cat.getId());

            // Create first address (origin)
            Address address1 = new Address();
            address1.setFname("John");
            address1.setName("Doe");
            address1.setAddress("123 Cat Street, Feline City, CA 90210");
            address1.setObjOwner(user);
            
            address1 = addressRepository.save(address1);
            System.out.println("Sample address 1 created successfully with ID: " + address1.getId());

            // Create second address (destination)
            Address address2 = new Address();
            address2.setFname("Jane");
            address2.setName("Smith");
            address2.setAddress("456 Kitten Lane, Whiskers Town, NY 10001");
            address2.setObjOwner(user);
            
            address2 = addressRepository.save(address2);
            System.out.println("Sample address 2 created successfully with ID: " + address2.getId());

            // Create a sample shipment
            Shipment shipment = new Shipment();
            shipment.setFromAddress(address1);
            shipment.setToAddress(address2);
            shipment.setCat(cat);
            shipment.setStatus("open");
            shipment.setObjOwner(user);
            
            shipment = shipmentRepository.save(shipment);
            System.out.println("Sample shipment created successfully with ID: " + shipment.getId() + " and tracking ID: " + shipment.getTrackingId());

        } catch (Exception e) {
            System.err.println("Error creating sample data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
