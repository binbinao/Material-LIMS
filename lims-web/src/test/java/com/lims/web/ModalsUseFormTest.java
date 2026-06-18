package com.lims.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #11: the reject-modal, receive-sample-modal, and
 * revise-modal in {@code RequestDetail} / {@code ReportDetail} read
 * their form values via {@code document.getElementById(...).value}
 * inside the {@code onOk} callback. That bypasses Ant's {@code <Form>}
 * validation entirely ({@code rules={[{ required: true }]}} is silently
 * ignored), and the {@code el?.value || ''} fallback lets the user
 * submit {@code ""} without a validation error.
 *
 * Two contracts the fix must satisfy (asserted at source level):
 *
 *  1. {@code RequestDetail/index.tsx} and {@code ReportDetail/index.tsx}
 *     must NOT call {@code document.getElementById}.
 *  2. Each modal's form must use Ant's {@code Form.useForm()} hook and
 *     the {@code onOk} handler must call {@code form.validateFields()}
 *     to obtain the value (so the {@code required: true} rule fires).
 */
class ModalsUseFormTest {

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

    @Test
    void requestDetailDoesNotReadModalValuesViaGetElementById() throws Exception {
        String content = readSource(
                "lims-web-ui/src/pages/request/RequestDetail/index.tsx");
        assertFalse(content.contains("document.getElementById("),
                "RequestDetail/index.tsx must not use document.getElementById to " +
                        "read modal form values. Today the reject modal and the " +
                        "receive-sample modal both do `const el = document.getElementById(...)` " +
                        "inside onOk, which bypasses Ant's required-validation and " +
                        "lets the user submit empty values.");
    }

    @Test
    void requestDetailUsesFormUseFormHook() throws Exception {
        String content = readSource(
                "lims-web-ui/src/pages/request/RequestDetail/index.tsx");
        assertTrue(content.contains("Form.useForm(") || content.contains("useForm("),
                "RequestDetail/index.tsx must define at least one Form.useForm() " +
                        "instance and pass it to the modal's <Form form={...}> so the " +
                        "onOk handler can call form.validateFields() and Ant's " +
                        "required rules actually fire.");
    }

    @Test
    void reportDetailDoesNotReadModalValuesViaGetElementById() throws Exception {
        String content = readSource(
                "lims-web-ui/src/pages/report/ReportDetail/index.tsx");
        assertFalse(content.contains("document.getElementById("),
                "ReportDetail/index.tsx must not use document.getElementById to " +
                        "read modal form values. Today the revise modal does " +
                        "`const el = document.getElementById('revision-note-input')` " +
                        "inside onOk, which lets the user submit a report revision " +
                        "without a revision note.");
    }

    @Test
    void reportDetailUsesFormUseFormHook() throws Exception {
        String content = readSource(
                "lims-web-ui/src/pages/report/ReportDetail/index.tsx");
        assertTrue(content.contains("Form.useForm(") || content.contains("useForm("),
                "ReportDetail/index.tsx must define at least one Form.useForm() " +
                        "instance and pass it to the revise modal's " +
                        "<Form form={...}> so the onOk handler can call " +
                        "form.validateFields() and Ant's required rules actually " +
                        "fire.");
    }
}
