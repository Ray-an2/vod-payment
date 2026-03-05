package com.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Testcontainers
class PaymentIntegrationTest {

    private static final String TEST_CARD_NUMBER = "4970109999999998";

    @Container
    static MariaDBContainer<?> mariaDB = new MariaDBContainer<>("mariadb:11.4")
            .withDatabaseName("test")
            .withUsername("payment_user")
            .withPassword("payment_password");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mariaDB::getJdbcUrl);
        registry.add("spring.datasource.username", mariaDB::getUsername);
        registry.add("spring.datasource.password", mariaDB::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldRunCrudQueriesOnMariaDb() {
        jdbcTemplate.update("DELETE FROM payment WHERE card_number = ?", TEST_CARD_NUMBER);

        int inserted = jdbcTemplate.update(
                "INSERT INTO payment (type, statut, montant, wallet_provider, card_owner, card_number, card_expiry_date, cvv) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                "CARTE", "EN_ATTENTE", 14, null, "Test JUnit MariaDB", TEST_CARD_NUMBER, "12/30", "456"
        );
        assertEquals(1, inserted);

        Map<String, Object> created = jdbcTemplate.queryForMap(
                "SELECT id, type, statut, montant, card_owner FROM payment WHERE card_number = ?",
                TEST_CARD_NUMBER
        );
        assertNotNull(created.get("id"));
        assertEquals("CARTE", created.get("type"));
        assertEquals("EN_ATTENTE", created.get("statut"));
        assertEquals(14, ((Number) created.get("montant")).intValue());

        int updated = jdbcTemplate.update(
                "UPDATE payment SET statut = ?, montant = ? WHERE card_number = ?",
                "EFFECTUE", 16, TEST_CARD_NUMBER
        );
        assertEquals(1, updated);

        Map<String, Object> updatedRow = jdbcTemplate.queryForMap(
                "SELECT statut, montant FROM payment WHERE card_number = ?",
                TEST_CARD_NUMBER
        );
        assertEquals("EFFECTUE", updatedRow.get("statut"));
        assertEquals(16, ((Number) updatedRow.get("montant")).intValue());

        int deleted = jdbcTemplate.update(
                "DELETE FROM payment WHERE card_number = ?",
                TEST_CARD_NUMBER
        );
        assertEquals(1, deleted);

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment WHERE card_number = ?",
                Integer.class,
                TEST_CARD_NUMBER
        );
        assertEquals(0, remaining);
    }
}
