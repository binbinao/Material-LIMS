package com.lims.service.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPermissionInterceptorFailClosedTest {

    @Test
    void detectsControlledTableInComplexQuery() {
        String sql = "SELECT r.id FROM request r JOIN report p ON p.request_id = r.id "
                + "WHERE r.deleted_at IS NULL";

        assertTrue(DataPermissionInterceptor.containsControlledTableReference(sql));
    }

    @Test
    void doesNotTreatUnprotectedTableAsControlled() {
        String sql = "SELECT id FROM equipment WHERE deleted_at IS NULL";

        assertFalse(DataPermissionInterceptor.containsControlledTableReference(sql));
    }

    @Test
    void regexFallbackFindsControlledTable() {
        assertTrue("request".equals(
                DataPermissionInterceptor.tryRegexFallback("SELECT * FROM request r ORDER BY r.created_at")));
    }
}
