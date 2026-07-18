package com.smarttask;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Smoke test — verifies the test infrastructure is wired up.
 * Full context load tests are covered by the module-specific service tests.
 */
@DisplayName("SmartTask Application Tests")
class SmartTaskApplicationTest {

    @Test
    @DisplayName("Application test runner starts correctly")
    void contextLoads() {
        // If this runs, the test classpath and compile chain are healthy.
    }
}
