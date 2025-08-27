package com.contrast.cargocats.dataservice;

import com.contrast.dataservice.PaymentController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private JdbcTemplate creditCardsJdbcTemplate;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        // Set up the creditCardsJdbcTemplate field via reflection since it's autowired
        try {
            var field = PaymentController.class.getDeclaredField("creditCardsJdbcTemplate");
            field.setAccessible(true);
            field.set(paymentController, creditCardsJdbcTemplate);
        } catch (Exception e) {
            fail("Could not set up creditCardsJdbcTemplate mock");
        }
    }

    @Test
    void shouldUseParameterizedQueriesForValidInput() {
        // Arrange
        String creditCard = "4111111111111111";
        String shipmentId = "123";

        // Act
        List<Map<String, Object>> result = paymentController.executeRawQuery(creditCard, shipmentId);

        // Assert
        assertEquals(1, result.size());
        assertTrue((Boolean) result.get(0).get("success"));
        assertEquals(shipmentId, result.get(0).get("shipment_id"));

        // Verify parameterized queries are used (not string concatenation)
        verify(creditCardsJdbcTemplate).execute(anyString()); // table creation
        verify(creditCardsJdbcTemplate).update(
            eq("INSERT INTO credit_card (card_number, shipment_id) VALUES (?, ?)"), 
            eq(creditCard), 
            eq(123L)
        );
        verify(jdbcTemplate).update(
            eq("UPDATE shipment SET credit_card = ? WHERE id = ?"), 
            eq("XXXX-XXXX-XXXX-1111"), 
            eq(123L)
        );
    }

    @Test 
    void shouldHandleSqlInjectionAttemptSafely() {
        // Arrange - SQL injection attempt in credit card parameter
        String maliciousInput = "'; DROP TABLE credit_card; --";
        String shipmentId = "123";
        
        // Act
        List<Map<String, Object>> result = paymentController.executeRawQuery(maliciousInput, shipmentId);

        // Assert
        assertEquals(1, result.size());
        assertTrue((Boolean) result.get(0).get("success"));

        // Verify that the malicious input is passed as a parameter, not concatenated into SQL
        verify(creditCardsJdbcTemplate).update(
            eq("INSERT INTO credit_card (card_number, shipment_id) VALUES (?, ?)"), 
            eq(maliciousInput), 
            eq(123L)
        );
    }

    @Test
    void shouldRejectInvalidShipmentIdFormat() {
        // Arrange
        String creditCard = "4111111111111111";
        String maliciousShipmentId = "1; DROP TABLE shipment; --";
        
        // Act
        List<Map<String, Object>> result = paymentController.executeRawQuery(creditCard, maliciousShipmentId);

        // Assert
        assertEquals(1, result.size());
        assertTrue((Boolean) result.get(0).get("error"));
        assertEquals("Invalid shipment ID format. Must be a valid number.", result.get(0).get("message"));

        // Verify no SQL queries were executed due to invalid input
        verify(creditCardsJdbcTemplate, never()).update(anyString(), any(), any());
        verify(jdbcTemplate, never()).update(anyString(), any(), any());
    }

    @Test
    void shouldRequireBothParameters() {
        // Act & Assert - missing creditCard parameter
        List<Map<String, Object>> result1 = paymentController.executeRawQuery(null, "123");
        assertEquals(1, result1.size());
        assertTrue((Boolean) result1.get(0).get("error"));
        assertTrue(result1.get(0).get("message").toString().contains("Both creditCard and shipmentId parameters are required"));

        // Act & Assert - missing shipmentId parameter
        List<Map<String, Object>> result2 = paymentController.executeRawQuery("4111111111111111", null);
        assertEquals(1, result2.size());
        assertTrue((Boolean) result2.get(0).get("error"));
        assertTrue(result2.get(0).get("message").toString().contains("Both creditCard and shipmentId parameters are required"));

        // Verify no SQL queries were executed due to missing parameters
        verifyNoInteractions(creditCardsJdbcTemplate, jdbcTemplate);
    }
}