package com.lims.service.report;

import com.lims.common.exception.BusinessException;
import com.lims.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * Word→PDF 转换：调用本机 LibreOffice headless。
 * 通过 libreoffice.command 配置可执行路径（默认 "libreoffice"）。
 * 若命令不可用，dev 环境优雅降级（返回 null，不抛异常）。
 */
@Slf4j
@Service
public class WordToPdfConverter {

    @Value("${libreoffice.command:libreoffice}")
    private String libreofficeCommand;

    @Value("${libreoffice.timeout-seconds:60}")
    private long timeoutSeconds;

    @Value("${libreoffice.fail-on-error:false}")
    private boolean failOnError;

    /**
     * 转换 docx 为 pdf。
     * @return 生成的 pdf 路径；失败且 failOnError=false 时返回 null。
     */
    public Path convert(Path docxPath) {
        if (!Files.exists(docxPath)) {
            throw new BusinessException(ErrorCode.FILE_CONVERT_FAILED, "Source file not found: " + docxPath);
        }
        Path outDir = docxPath.getParent();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    libreofficeCommand, "--headless", "--convert-to", "pdf",
                    "--outdir", outDir.toString(), docxPath.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return failOrNull("LibreOffice timeout after " + timeoutSeconds + "s");
            }
            int code = process.exitValue();
            if (code != 0) {
                String stdout = new String(process.getInputStream().readAllBytes());
                return failOrNull("LibreOffice exited with " + code + ": " + stdout);
            }
            String docxName = docxPath.getFileName().toString();
            String pdfName = docxName.substring(0, docxName.lastIndexOf('.')) + ".pdf";
            Path pdfPath = outDir.resolve(pdfName);
            if (!Files.exists(pdfPath)) {
                return failOrNull("PDF not produced: " + pdfPath);
            }
            return pdfPath;
        } catch (IOException e) {
            return failOrNull("LibreOffice command not available: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return failOrNull("Conversion interrupted");
        }
    }

    private Path failOrNull(String reason) {
        if (failOnError) {
            log.error("Word→PDF conversion failed: {}", reason);
            throw new BusinessException(ErrorCode.FILE_CONVERT_FAILED, reason);
        }
        log.warn("Word→PDF conversion skipped: {}", reason);
        return null;
    }

    /** 兜底：只读 ProcessBuilder 工厂方法，方便测试覆盖 */
    @SuppressWarnings("unused")
    public static Path defaultOutputDir() {
        return Paths.get(System.getProperty("java.io.tmpdir"), "lims-pdfs");
    }
}
