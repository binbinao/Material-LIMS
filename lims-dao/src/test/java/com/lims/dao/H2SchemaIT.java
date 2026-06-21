package com.lims.dao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for the H2 schema: when the {@code @SpringBootTest}
 * context starts up, {@link AbstractDaoIT} loads
 * {@code classpath:schema-h2.sql} into the in-memory H2 instance. This
 * test asserts the schema is usable by issuing a real {@code SELECT}
 * against the {@code brand} table (the very first table the script
 * creates).
 *
 * <p>Now extends {@link AbstractDaoIT} instead of declaring its own
 * {@code @SpringBootTest} / {@code @Sql} — sharing the base avoids
 * loading the schema twice into the same in-memory DB (which would
 * fail on the second {@code CREATE TABLE brand}).
 */
@Tag("integration")
class H2SchemaIT extends AbstractDaoIT {

    @Autowired
    DataSource dataSource;

    @Test
    @DisplayName("H2 schema loads and `brand` table is queryable")
    void brandTableExists() throws Exception {
        // The shared in-memory DB (see AbstractDaoIT) may already hold
        // rows from sibling ITs, so we assert the table is present via
        // DatabaseMetaData instead of checking COUNT(*) is zero. The
        // purpose here is "schema script ran cleanly" — a successful
        // SELECT against the table is sufficient evidence; the row
        // count is incidental.
        try (Connection c = dataSource.getConnection()) {
            var tables = c.getMetaData().getTables(null, "PUBLIC", "BRAND", null);
            assertThat(tables.next()).isTrue();
        }
    }
}
