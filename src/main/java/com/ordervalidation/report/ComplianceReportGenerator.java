package com.ordervalidation.report;

import com.ordervalidation.model.ValidationResult;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Report Generator - creates compliance reports in CSV format.
 */
public class ComplianceReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ComplianceReportGenerator.class);

    /**
     * Generate CSV report from validation results.
     *
     * @param results List of validation results
     * @param outputFile Path to output CSV file
     * @throws IOException if file write fails
     */
    public static void generateCsvReport(List<ValidationResult> results, String outputFile) 
            throws IOException {
        logger.info("Generating compliance report to: {}", outputFile);

        try (FileWriter writer = new FileWriter(outputFile);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .withHeader("Order Number", "Status", "Rules Applied", "Failure Reasons"))) {

            for (ValidationResult result : results) {
                String status = result.isPassed() ? "PASS" : "FAIL";
                String rulesApplied = formatRulesApplied(result.getAppliedRuleNames(), "; ");
                String failureReasons = String.join("; ", result.getFailureReasons());

                csvPrinter.printRecord(
                        result.getOrderNumber(),
                        status,
                    rulesApplied,
                        failureReasons.isEmpty() ? "N/A" : failureReasons
                );
            }

            csvPrinter.flush();
            logger.info("Report generated successfully with {} records", results.size());
        }
    }

    /**
     * Generate detailed HTML report.
     *
     * @param results List of validation results
     * @param outputFile Path to output HTML file
     * @throws IOException if file write fails
     */
    public static void generateHtmlReport(List<ValidationResult> results, String outputFile) 
            throws IOException {
        logger.info("Generating HTML compliance report to: {}", outputFile);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("<meta charset='UTF-8'>\n");
        html.append("<title>Order Compliance Report</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin-top: 20px; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }\n");
        html.append("th { background-color: #4CAF50; color: white; }\n");
        html.append(".pass { background-color: #d4edda; }\n");
        html.append(".fail { background-color: #f8d7da; }\n");
        html.append("h1 { color: #333; }\n");
        html.append(".summary { background-color: #e7f3ff; padding: 10px; border-radius: 5px; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("<h1>Order Compliance Report</h1>\n");
        html.append("<p>Generated: ").append(LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>\n");

        // Summary
        long passedCount = results.stream().filter(ValidationResult::isPassed).count();
        long failedCount = results.size() - passedCount;
        double passPercentage = results.isEmpty() ? 0 : (passedCount * 100.0 / results.size());

        html.append("<div class='summary'>\n");
        html.append("<h2>Summary</h2>\n");
        html.append("<p>Total Orders: ").append(results.size()).append("</p>\n");
        html.append("<p>Passed: ").append(passedCount).append(" (").append(String.format("%.1f%%", passPercentage)).append(")</p>\n");
        html.append("<p>Failed: ").append(failedCount).append(" (").append(String.format("%.1f%%", 100 - passPercentage)).append(")</p>\n");
        html.append("</div>\n");

        // Details table
        html.append("<h2>Details</h2>\n");
        html.append("<table>\n");
        html.append("<tr><th>Order Number</th><th>Status</th><th>Rules Applied</th><th>Failure Reasons</th></tr>\n");

        for (ValidationResult result : results) {
            String rowClass = result.isPassed() ? "pass" : "fail";
            String status = result.isPassed() ? "PASS" : "FAIL";

            html.append("<tr class='").append(rowClass).append("'>\n");
            html.append("<td>").append(result.getOrderNumber()).append("</td>\n");
            html.append("<td>").append(status).append("</td>\n");
            String rulesApplied = formatRulesApplied(result.getAppliedRuleNames(), ", ");
            html.append("<td>").append(rulesApplied).append("</td>\n");
            html.append("<td>").append(String.join("<br/>", result.getFailureReasons())).append("</td>\n");
            html.append("</tr>\n");
        }

        html.append("</table>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write(html.toString());
        }

        logger.info("HTML report generated successfully");
    }

    /**
     * Print summary to console.
     */
    public static void printSummary(List<ValidationResult> results) {
        long passedCount = results.stream().filter(ValidationResult::isPassed).count();
        long failedCount = results.size() - passedCount;
        double passPercentage = results.isEmpty() ? 0 : (passedCount * 100.0 / results.size());

        System.out.println("\n" + "=".repeat(60));
        System.out.println("COMPLIANCE VALIDATION SUMMARY");
        System.out.println("=".repeat(60));
        System.out.printf("Total Orders: %d%n", results.size());
        System.out.printf("✓ Passed: %d (%.1f%%)%n", passedCount, passPercentage);
        System.out.printf("✗ Failed: %d (%.1f%%)%n", failedCount, 100 - passPercentage);
        System.out.println("=".repeat(60) + "\n");

        // Show failed orders
        if (failedCount > 0) {
            System.out.println("FAILED ORDERS:");
            results.stream()
                    .filter(r -> !r.isPassed())
                    .forEach(r -> {
                        System.out.printf("  Order: %s%n", r.getOrderNumber());
                        r.getFailureReasons().forEach(reason -> System.out.printf("    - %s%n", reason));
                    });
            System.out.println();
        }
    }

    private static String formatRulesApplied(List<String> appliedRuleNames, String delimiter) {
        if (appliedRuleNames == null || appliedRuleNames.isEmpty()) {
            return "No matching rule";
        }
        return String.join(delimiter, appliedRuleNames);
    }
}
