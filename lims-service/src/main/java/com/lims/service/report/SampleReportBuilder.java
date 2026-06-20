package com.lims.service.report;

import com.lims.model.entity.Report;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds a randomized sample .docx for a report. Used by
 * {@link com.lims.service.ReportService#getSampleWordBytes(String)}
 * so every report (even seeded rows whose {@code file_url} is a
 * placeholder string) has a real, downloadable Word document.
 *
 * Each call to {@link #build(XWPFDocument, Report)} produces a
 * different output — the analysis-method list, the result values,
 * the lab-tech / sample-batch ids and the pass/fail conclusion are
 * all drawn from {@link ThreadLocalRandom}. Layout is intentionally
 * plain (headings, paragraphs, a results table) so the resulting
 * .docx is small, opens cleanly in Word / WPS / Pages, and round-
 * trips through zip-2-docx validators.
 */
public final class SampleReportBuilder {

    private static final String[] METHODS = {
            "Tensile Strength (ASTM D638)", "Hardness, Shore D (ASTM D2240)",
            "FTIR Spectroscopy (ASTM E1252)", "TGA (ISO 11358)",
            "DSC (ISO 11357)", "Melt Flow Index (ASTM D1238)",
            "Density (ASTM D792)", "Ash Content (ASTM D5630)",
            "Moisture (ASTM D6869)", "LOI (ASTM D2863)"
    };
    private static final String[] UNITS = {
            "MPa", "Shore D", "%", "°C", "g/10min", "g/cm³", "%", "ppm", "%", "Vol%"
    };
    private static final String[] LAB_TECHS = {
            "Tech-A", "Tech-B", "Tech-C", "Tech-D", "Tech-E"
    };
    private static final String[] CONCLUSIONS = {
            "All measured values meet the acceptance criteria. The sample PASSES.",
            "Tensile and hardness results meet the spec. The sample PASSES.",
            "FTIR spectrum shows minor deviation at 1720 cm^-1; material is within tolerance. The sample PASSES.",
            "One result is below the lower control limit. Recommend a re-test. The sample FAILS.",
            "Density is out of range; the lot is likely mis-graded. The sample FAILS.",
            "All physical tests pass; chemical analysis pending. Hold release pending QA."
    };
    private static final String[] NOTATIONS = {
            "Note: results are based on a single lot sample.",
            "Note: ambient lab conditions 23+/-2 C / 50+/-5% RH.",
            "Note: instrument calibrated per IQ/OQ on file.",
            "Note: method deviation recorded in deviation log %s.",
            "Note: sample conditioned 24h at 23C / 50% RH prior to testing."
    };

    private SampleReportBuilder() {}

    public static void build(XWPFDocument doc, Report report) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Title
        XWPFParagraph title = doc.createParagraph();
        title.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = title.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(20);
        titleRun.setText("LIMS Material Test Report - Sample for " + report.getId());

        XWPFParagraph sub = doc.createParagraph();
        sub.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun subRun = sub.createRun();
        subRun.setItalic(true);
        subRun.setFontSize(10);
        subRun.setText("Generated " + LocalDateTime.now().format(fmt) +
                " - This is a randomized sample, not real lab data.");

        addBlank(doc);

        // 1. Report metadata
        addHeading(doc, "1. Report Information");
        addKv(doc, "Report ID",        nz(report.getId()));
        addKv(doc, "Request ID",       nz(report.getRequestId()));
        addKv(doc, "Version",          nz(report.getVersionNumber()));
        addKv(doc, "Status",           nz(report.getStatus()));
        addKv(doc, "Author",           nz(report.getAuthorId()));
        addKv(doc, "Sample Batch",     "BATCH-" + String.format("%04d", rnd.nextInt(1000)));
        addKv(doc, "Lab Technician",   LAB_TECHS[rnd.nextInt(LAB_TECHS.length)]);
        addBlank(doc);

        // 2. Test methods + random results table
        addHeading(doc, "2. Test Methods and Results");
        int methodCount = 3 + rnd.nextInt(3); // 3..5
        XWPFTable table = doc.createTable(methodCount + 1, 4);
        XWPFTableRow header = table.getRow(0);
        setCell(header.getCell(0), "Method", true);
        setCell(header.getCell(1), "Standard", true);
        setCell(header.getCell(2), "Result", true);
        setCell(header.getCell(3), "Unit", true);
        for (int i = 0; i < methodCount; i++) {
            int idx = rnd.nextInt(METHODS.length);
            XWPFTableRow row = table.getRow(i + 1);
            setCell(row.getCell(0), METHODS[idx], false);
            String standard = METHODS[idx].contains("(")
                    ? METHODS[idx].substring(METHODS[idx].indexOf('(') + 1, METHODS[idx].lastIndexOf(')'))
                    : "Internal";
            setCell(row.getCell(1), standard, false);
            String result = String.format("%.2f", 10 + rnd.nextDouble() * 90);
            if (rnd.nextBoolean()) result = result + " *";
            setCell(row.getCell(2), result, false);
            setCell(row.getCell(3), UNITS[idx % UNITS.length], false);
        }
        addBlank(doc);

        // 3. Conclusion
        addHeading(doc, "3. Conclusion");
        XWPFParagraph conclusion = doc.createParagraph();
        XWPFRun cRun = conclusion.createRun();
        cRun.setText(CONCLUSIONS[rnd.nextInt(CONCLUSIONS.length)]);
        addBlank(doc);

        // 4. Notes
        addHeading(doc, "4. Notes");
        XWPFParagraph note = doc.createParagraph();
        XWPFRun nRun = note.createRun();
        String n = NOTATIONS[rnd.nextInt(NOTATIONS.length)];
        nRun.setText(n.contains("%s") ? String.format(n, "DEV-" + (1000 + rnd.nextInt(9000))) : n);
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private static void addHeading(XWPFDocument doc, String text) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun r = p.createRun();
        r.setBold(true);
        r.setFontSize(14);
        r.setText(text);
    }

    private static void addKv(XWPFDocument doc, String k, String v) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun rk = p.createRun();
        rk.setBold(true);
        rk.setText(k + ": ");
        XWPFRun rv = p.createRun();
        rv.setText(v == null ? "" : v);
    }

    private static void addBlank(XWPFDocument doc) {
        doc.createParagraph();
    }

    private static void setCell(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        XWPFRun r = p.createRun();
        r.setBold(bold);
        r.setText(text);
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
