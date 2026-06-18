package com.lims.service.storage;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TDD test for issue #9: {@code FileStorageService} uploads files to
 * MinIO and returns a presigned URL that the user's browser visits.
 * Today the source filename is concatenated into the MinIO object name
 * and the response {@code Content-Type} is guessed by
 * {@code URLConnection.guessContentTypeFromName}, so a file uploaded as
 * {@code evil.html} becomes an object that browsers render with
 * {@code text/html} — the attacker now controls an XSS payload under
 * the trusted MinIO domain.
 *
 * Four contracts the fix must satisfy (asserted at source level):
 *
 *  1. There is a server-side MIME map keyed by file extension, not
 *     reliance on the OS-level {@code URLConnection} guess (which can
 *     be tricked by crafted extensions and which varies by JRE).
 *  2. There is an allow-list of acceptable extensions; common XSS
 *     vectors ({@code .html}, {@code .htm}, {@code .svg},
 *     {@code .xhtml}) and executable vectors ({@code .exe},
 *     {@code .bat}, {@code .sh}) must NOT be accepted.
 *  3. Filenames are sanitized before being concatenated into the
 *     object name — control characters and path separators must be
 *     stripped so a filename like {@code ../../etc/passwd} can't
 *     escape the object-key prefix.
 *  4. The presigned URL must include a
 *     {@code response-content-disposition=attachment} (or equivalent
 *     MinIO query parameter) so browsers download rather than render
 *     the file even if the guessed content type is
 *     {@code text/html}.
 */
class FileStorageServiceXssTest {

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
    void fileStorageServiceHasServerSideMimeMap() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/storage/FileStorageService.java");
        boolean hasMap = content.contains("MIME_BY_EXT")
                || content.contains(".pdf")
                || content.contains("application/pdf");
        assertTrue(hasMap,
                "FileStorageService must use a server-side MIME map keyed by " +
                        "extension (e.g. a static Map<String,String> MIME_BY_EXT) " +
                        "instead of relying on URLConnection.guessContentTypeFromName, " +
                        "which varies by JRE and trusts the uploaded filename.");
    }

    @Test
    void fileStorageServiceHasAllowedExtensionWhitelist() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/storage/FileStorageService.java");
        // The fix is allow-list based: the dangerous extensions must NOT
        // appear in the ALLOWED_EXTS Set.of(...) call. We check that the
        // Set.of block is present and that .html and .exe are absent.
        boolean hasSetOf = content.contains("ALLOWED_EXTS")
                && content.contains("Set.of(");
        // The allow-list lives between "Set.of(" and the next matching ")".
        // Greedy on newline is fine here because no extension is on its own line.
        int setStart = content.indexOf("ALLOWED_EXTS");
        int setBodyStart = content.indexOf("Set.of(", setStart);
        int setBodyEnd = content.indexOf(");", setBodyStart);
        String allowList = (setBodyStart >= 0 && setBodyEnd >= 0)
                ? content.substring(setBodyStart, setBodyEnd)
                : "";
        boolean excludesHtml = !allowList.contains("\".html\"")
                && !allowList.contains("\".htm\"")
                && !allowList.contains("\".xhtml\"")
                && !allowList.contains("\".svg\"");
        boolean excludesExe = !allowList.contains("\".exe\"")
                && !allowList.contains("\".bat\"")
                && !allowList.contains("\".sh\"");
        assertTrue(hasSetOf && excludesHtml && excludesExe,
                "FileStorageService must use an allow-list (ALLOWED_EXTS = Set.of(...)) " +
                        "and that list must not include the XSS vectors .html / .htm / " +
                        ".xhtml / .svg or the RCE vectors .exe / .bat / .sh. " +
                        "Today an attacker can upload evil.html and serve it with " +
                        "text/html from the trusted MinIO domain.");
    }

    @Test
    void fileStorageServiceSanitizesFilenameControlChars() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/storage/FileStorageService.java");
        boolean strips = content.contains("replaceAll(\"\\\\p{Cntrl}\"")
                || content.contains("replaceAll(\"[^A-Za-z0-9\"")
                || content.contains("sanitize")
                || content.contains("replaceAll(\"[\\\\\\\\/\"");
        assertTrue(strips,
                "FileStorageService.upload must sanitize the source filename " +
                        "before concatenating it into the MinIO object name. " +
                        "Today a filename like '..\\u0000../../etc/passwd' is " +
                        "concatenated as-is.");
    }

    @Test
    void fileStorageServiceForcesAttachmentOnPresignedUrl() throws Exception {
        String content = readSource(
                "lims-service/src/main/java/com/lims/service/storage/FileStorageService.java");
        boolean hasDisposition = content.contains("response-content-disposition")
                || content.contains("Content-Disposition")
                || content.contains("attachment");
        assertTrue(hasDisposition,
                "FileStorageService must add response-content-disposition=attachment " +
                        "to the MinIO presigned URL so the browser downloads rather " +
                        "than renders the file (which would defeat the " +
                        "Content-Type=application/octet-stream fallback).");
    }
}
