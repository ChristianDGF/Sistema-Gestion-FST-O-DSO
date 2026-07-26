package proyecto.sistemaGestion.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ProductIntegrationTest already proves ProductService.create() rejects a duplicate SKU
 * via its own existsBySku() pre-check. These tests target what that pre-check cannot cover:
 * a duplicate that reaches the database directly, and a race between two concurrent inserts
 * for the same SKU where the UNIQUE constraint is the only real defense.
 */
class DuplicateDataTest extends AbstractDataTest {

    private static final String SKU_PREFIX = "DUP-";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM products WHERE sku LIKE ?", SKU_PREFIX + "%");
    }

    private void insertProduct(String sku) {
        jdbcTemplate.update(
                "INSERT INTO products (name, sku, category, price, quantity, min_stock, status) " +
                        "VALUES ('Duplicate Test', ?, 'Test', 10, 1, 0, 'ACTIVE')",
                sku);
    }

    @Test
    void databaseRejectsDuplicateSku_evenBypassingServiceLayer() {
        String sku = SKU_PREFIX + "DIRECT-001";
        insertProduct(sku);

        assertThrows(DataIntegrityViolationException.class, () -> insertProduct(sku));
    }

    @Test
    void concurrentInsertsWithSameSku_onlyOneSucceeds() throws InterruptedException {
        String sku = SKU_PREFIX + "RACE-001";
        int attempts = 2;
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(attempts);

        try {
            for (int i = 0; i < attempts; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await();
                        insertProduct(sku);
                        successCount.incrementAndGet();
                    } catch (DataAccessException e) {
                        failureCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            startGate.countDown();
            executor.shutdown();
            assertEquals(true, executor.awaitTermination(10, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, successCount.get(), "Exactly one concurrent insert should have succeeded");
        assertEquals(attempts - 1, failureCount.get(), "The rest must fail on the UNIQUE(sku) constraint");
    }
}
