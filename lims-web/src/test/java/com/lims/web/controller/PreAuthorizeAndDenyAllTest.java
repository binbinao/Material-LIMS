package com.lims.web.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #5: a set of controller methods have no
 * {@code @PreAuthorize} gate, and {@code prodFilterChain} uses
 * {@code anyRequest().authenticated()} instead of denyAllByDefault.
 *
 * Six contracts the fix must satisfy (asserted at source level — a heavier
 * Spring Security integration test would need a full {@code @WebMvcTest}
 * slice and the project's other beans; this catches the structural gap):
 *
 *  1. {@code ReportController} mutation endpoints ({@code syncFromSharePoint},
 *     {@code submit}, {@code revise}, {@code revisions}) all carry an
 *     {@code @PreAuthorize} annotation so non-ENGINEERs / non-MANAGERs
 *     cannot drive report state changes.
 *  2. {@code EquipmentController} read endpoints ({@code list}, {@code getById})
 *     carry an {@code @PreAuthorize("isAuthenticated()")} so anonymous
 *     traffic is rejected at the controller level.
 *  3. {@code EquipmentRepairController} read endpoints carry an
 *     {@code @PreAuthorize}.
 *  4. {@code KnowledgeDocController} read endpoints carry an
 *     {@code @PreAuthorize}.
 *  5. {@code DashboardController.myTasks} derives {@code userId} from
 *     {@code SecurityUtils.getCurrentUserId()} — the current implementation
 *     reads it from a {@code @RequestParam}, which lets any logged-in
 *     user query dashboard stats for any other user.
 *  6. {@code SecurityConfig.prodFilterChain} ends with
 *     {@code .anyRequest().denyAll()} instead of
 *     {@code .anyRequest().authenticated()}, so a missing controller-level
 *     gate does not silently grant access.
 */
class PreAuthorizeAndDenyAllTest {

    private static String readSource(String relPath) throws Exception {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (Path p = userDir; p != null; p = p.getParent()) {
            Path candidate = p.resolve(relPath);
            if (Files.isRegularFile(candidate)) {
                return Files.readString(candidate);
            }
        }
        throw new IllegalStateException(relPath + " not found above " + userDir);
    }

    private static String readWindowBeforeMethod(String content, String methodName) {
        // Find the method's `public R` declaration, then look at the 200-char
        // window above it for the @PreAuthorize annotation. Use the second
        // occurrence of `public R` after the method name (handles signatures
        // that span multiple lines).
        int nameIdx = content.indexOf(methodName);
        if (nameIdx < 0) return "";
        int publicRIdx = content.indexOf("public R", nameIdx);
        if (publicRIdx < 0) return "";
        int start = Math.max(0, publicRIdx - 200);
        return content.substring(start, publicRIdx + "public R".length());
    }

    @Test
    void reportControllerMutationEndpointsHavePreAuthorize() throws Exception {
        String content = readSource(
                "lims-web/src/main/java/com/lims/web/controller/ReportController.java");
        for (String method : new String[]{
                "syncFromSharePoint", "submit", "revise", "revisions"}) {
            String window = readWindowBeforeMethod(content, method);
            assertTrue(window.contains("@PreAuthorize"),
                    "ReportController." + method + " must carry an @PreAuthorize " +
                            "annotation. Today any logged-in user can call it.");
        }
    }

    @Test
    void equipmentControllerReadEndpointsHavePreAuthorize() throws Exception {
        String content = readSource(
                "lims-web/src/main/java/com/lims/web/controller/EquipmentController.java");
        for (String method : new String[]{"list", "getById"}) {
            String window = readWindowBeforeMethod(content, method);
            assertTrue(window.contains("@PreAuthorize"),
                    "EquipmentController." + method + " must carry an @PreAuthorize " +
                            "(e.g. isAuthenticated() for read, hasRole('ADMIN') for admin).");
        }
    }

    @Test
    void equipmentRepairControllerReadEndpointsHavePreAuthorize() throws Exception {
        String content = readSource(
                "lims-web/src/main/java/com/lims/web/controller/EquipmentRepairController.java");
        for (String method : new String[]{"list", "getById"}) {
            String window = readWindowBeforeMethod(content, method);
            assertTrue(window.contains("@PreAuthorize"),
                    "EquipmentRepairController." + method + " must carry an @PreAuthorize.");
        }
    }

    @Test
    void knowledgeDocControllerReadEndpointsHavePreAuthorize() throws Exception {
        String content = readSource(
                "lims-web/src/main/java/com/lims/web/controller/KnowledgeDocController.java");
        for (String method : new String[]{"list", "getById"}) {
            String window = readWindowBeforeMethod(content, method);
            assertTrue(window.contains("@PreAuthorize"),
                    "KnowledgeDocController." + method + " must carry an @PreAuthorize.");
        }
    }

    @Test
    void dashboardMyTasksDerivesUserIdFromSecurityUtils() throws Exception {
        String content = readSource(
                "lims-web/src/main/java/com/lims/web/controller/DashboardController.java");
        // The fix must:
        //   - NOT take `userId` as a @RequestParam anywhere
        //   - call SecurityUtils.getCurrentUserId() somewhere in the myTasks flow
        boolean takesUserIdFromQuery = content.contains("@RequestParam String userId");
        boolean callsGetCurrentUserId = content.contains("SecurityUtils.getCurrentUserId()");
        assertTrue(!takesUserIdFromQuery && callsGetCurrentUserId,
                "DashboardController must NOT take userId as a @RequestParam and " +
                        "MUST call SecurityUtils.getCurrentUserId() (typically inside " +
                        "myTasks). Today the old signature takes userId via " +
                        "@RequestParam, so any logged-in user can fetch any other " +
                        "user's dashboard stats.");
    }

    @Test
    void securityConfigProdFilterChainDeniesByDefault() throws Exception {
        String content = readSource(
                "lims-web/src/main/java/com/lims/web/config/SecurityConfig.java");
        assertTrue(content.contains("denyAll"),
                "SecurityConfig.prodFilterChain must use .anyRequest().denyAll() " +
                        "(not .anyRequest().authenticated()) so that any new controller " +
                        "or endpoint without an explicit @PreAuthorize is denied by " +
                        "default. Today it falls through to .anyRequest().authenticated(), " +
                        "which means any authenticated user can call any new endpoint " +
                        "that has not been individually gated.");
    }
}
