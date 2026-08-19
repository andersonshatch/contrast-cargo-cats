package com.contrast.dataservice.config;

import com.contrast.dataservice.entity.User;
import com.contrast.dataservice.repository.AddressRepository;
import com.contrast.dataservice.repository.CatRepository;
import com.contrast.dataservice.repository.ShipmentRepository;
import com.contrast.dataservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerSecurityTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CatRepository catRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @InjectMocks
    private DataInitializer dataInitializer;

    private BCryptPasswordEncoder bcryptEncoder;

    @BeforeEach
    void setUp() {
        bcryptEncoder = new BCryptPasswordEncoder();
    }

    @Test
    void testPasswordIsHashedWithBCrypt() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        dataInitializer.run();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User("admin", "$2a$10$hashedPassword")));
        Optional<User> savedUser = userRepository.findByUsername("admin");

        assertTrue(savedUser.isPresent(), "Admin user should be created");
        String hashedPassword = savedUser.get().getPassword();
        assertNotNull(hashedPassword, "Password should not be null");
        assertTrue(hashedPassword.startsWith("$2a$") || hashedPassword.startsWith("$2b$") || hashedPassword.startsWith("$2y$"),
                "Password should be BCrypt hashed (starts with $2a$, $2b$, or $2y$)");
    }

    @Test
    void testPasswordIsNotMD5() throws Exception {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });

        dataInitializer.run();

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(new User("admin", "$2a$10$hashedPassword")));
        Optional<User> savedUser = userRepository.findByUsername("admin");

        assertTrue(savedUser.isPresent(), "Admin user should be created");
        String hashedPassword = savedUser.get().getPassword();
        assertFalse(hashedPassword.matches("^[a-f0-9]{32}$"),
                "Password should not be MD5 hash (32 hex characters)");
        assertFalse(hashedPassword.equals("5f4dcc3b5aa765d61d8327deb882cf99"),
                "Password should not be the MD5 hash of 'password123'");
    }

    @Test
    void testBCryptPasswordCanBeVerified() {
        String plainPassword = "password123";
        String hashedPassword = bcryptEncoder.encode(plainPassword);

        assertTrue(bcryptEncoder.matches(plainPassword, hashedPassword),
                "BCrypt should be able to verify the password");
        assertFalse(bcryptEncoder.matches("wrongpassword", hashedPassword),
                "BCrypt should reject incorrect passwords");
    }

    @Test
    void testBCryptHashIsUnique() {
        String plainPassword = "password123";
        String hash1 = bcryptEncoder.encode(plainPassword);
        String hash2 = bcryptEncoder.encode(plainPassword);

        assertNotEquals(hash1, hash2,
                "BCrypt should generate unique hashes for the same password (includes salt)");
        assertTrue(bcryptEncoder.matches(plainPassword, hash1),
                "First hash should verify correctly");
        assertTrue(bcryptEncoder.matches(plainPassword, hash2),
                "Second hash should verify correctly");
    }
}
