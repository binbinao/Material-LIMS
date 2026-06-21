package com.lims.dao;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Test-only Spring Boot configuration marker for lims-dao tests. The module is a
 * library (no @SpringBootApplication), so test classes that use @SpringBootTest
 * need an explicit configuration class. This stub enables auto-configuration
 * (so H2 / DataSource are picked up) but does NOT scan the main source tree
 * (which lives in lims-web, not lims-dao).
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class DaoTestApplication {
}
