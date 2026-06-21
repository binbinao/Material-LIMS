package com.lims.dao;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for DAO integration tests.
 *
 * <p>Uses an in-memory H2 database with the H2-flavoured schema
 * (classpath:schema-h2.sql). No Docker, no Testcontainers — every test
 * runs against a JVM-local H2 instance, so {@code ./mvnw verify} works
 * on any developer machine regardless of Docker availability.
 *
 * <p>Why this used to be Testcontainers + PostgreSQL: the original
 * author wanted the IT to exercise real PostgreSQL syntax. In practice
 * the project keeps the canonical DDL in {@code lims-web}'s
 * {@code schema.sql} (PostgreSQL 15) and uses H2 only for the DAO
 * smoke test. H2 with {@code MODE=PostgreSQL} is sufficient for the
 * narrow CRUD surface these tests cover (insert / selectById /
 * deleteById) and matches what {@link H2SchemaIT} already proved loads
 * cleanly.
 *
 * <p>Schema isolation: the in-memory DB is named {@code lims_dao_it} and
 * kept alive with {@code DB_CLOSE_DELAY=-1} so all subclasses share one
 * context (Spring caches the {@code @SpringBootTest} application context
 * by configuration) and the schema is loaded exactly once per JVM.
 */
@SpringBootTest
public abstract class AbstractDaoIT {

    private static final String H2_URL =
            "jdbc:h2:mem:lims_dao_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1";

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> H2_URL);
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.sql.init.mode", () -> "always");
        registry.add("spring.sql.init.schema-locations",
                () -> "classpath:schema-h2.sql");
    }
}
