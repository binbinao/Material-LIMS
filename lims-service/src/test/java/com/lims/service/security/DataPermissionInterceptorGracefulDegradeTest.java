package com.lims.service.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataPermissionInterceptorGracefulDegradeTest {

    @Test
    void fallbackStillInjectsPermissionPredicateForSupportedComplexQuery() {
        String sql = "SELECT r.id FROM request r ORDER BY r.created_at";
        String table = DataPermissionInterceptor.tryRegexFallback(sql);

        assertEquals("request", table);
        assertTrue(DataPermissionInterceptor.containsControlledTableReference(sql));
    }

    @Test
    void controlledTableReferenceIsRecognizedInSchemaQualifiedQuery() {
        String sql = "SELECT * FROM public.analysis_task task WHERE task.deleted_at IS NULL";

        assertTrue(DataPermissionInterceptor.containsControlledTableReference(sql));
    }
}
