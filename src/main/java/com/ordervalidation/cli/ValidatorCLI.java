package com.ordervalidation.cli;

import com.ordervalidation.engine.RulesEngine;
import com.ordervalidation.model.BusinessRule;
import com.ordervalidation.model.Order;
import com.ordervalidation.model.ValidationResult;
import com.ordervalidation.report.ComplianceReportGenerator;
import com.ordervalidation.util.ApiClientUtil;
import com.ordervalidation.util.CsvReaderUtil;
import com.ordervalidation.validator.OrderValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

/**
 * Command-line interface for Order Rules Validator.
 * Usage: java ValidatorCLI <orders-csv> <rules-api-url> [options]
 */
public class ValidatorCLI {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorCLI.class);

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                printUsage();
                System.exit(1);
            }

            String csvFile = args[0];
            String rulesApiUrl = args[1];
            String orderApiTemplate = args.length > 2 ? args[2] : null;
            String outputCsvFile = args.length > 3 ? args[3] : "compliance-report.csv";
            String outputHtmlFile = args.length > 4 ? args[4] : "compliance-report.html";

            System.out.println("\n╔══════════════════════════════════════════════════════════╗");
            System.out.println("║     Order Rules Compliance Validator v1.0                 ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝\n");

            // Validate input file
            if (!new File(csvFile).exists()) {
                System.err.println("❌ CSV file not found: " + csvFile);
                System.exit(1);
            }

            System.out.println("📋 Loading orders from: " + csvFile);
            List<Order> orders = CsvReaderUtil.readOrders(csvFile);
            System.out.println("✓ Loaded " + orders.size() + " orders\n");

            System.out.println("📡 Fetching business rules from API...");
            List<BusinessRule> rules = ApiClientUtil.fetchBusinessRules(rulesApiUrl);
            System.out.println("✓ Loaded " + rules.size() + " business rules\n");

            // Initialize engine and validator
            RulesEngine rulesEngine = new RulesEngine(rules);
            OrderValidator validator = new OrderValidator(rulesEngine);
            if (orderApiTemplate != null && !orderApiTemplate.isEmpty()) {
                validator.setOrderDetailsApiTemplate(orderApiTemplate);
            }

            // Validate all orders
            System.out.println("🔍 Validating orders...");
            List<ValidationResult> results = orders.stream()
                    .map(validator::validateOrder)
                    .toList();
            System.out.println("✓ Validation complete\n");

            // Generate reports
            System.out.println("📊 Generating reports...");
            ComplianceReportGenerator.generateCsvReport(results, outputCsvFile);
            System.out.println("  → CSV report: " + outputCsvFile);

            ComplianceReportGenerator.generateHtmlReport(results, outputHtmlFile);
            System.out.println("  → HTML report: " + outputHtmlFile);

            // Print summary
            ComplianceReportGenerator.printSummary(results);

            System.exit(0);
        } catch (Exception e) {
            logger.error("Fatal error", e);
            System.err.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printUsage() {
        System.out.println("\nUsage: java ValidatorCLI <orders-csv> <rules-api-url> [options]\n");
        System.out.println("Arguments:");
        System.out.println("  <orders-csv>        Path to CSV file with orders (orderNumber, nexusSessionId columns required)");
        System.out.println("  <rules-api-url>     API endpoint that returns business rules in JSON array");
        System.out.println("\nOptions:");
        System.out.println("  <order-api-template> Optional: API template to fetch order details (/api/orders/{{orderNumber}})");
        System.out.println("  <output-csv>        Optional: Output CSV report file (default: compliance-report.csv)");
        System.out.println("  <output-html>       Optional: Output HTML report file (default: compliance-report.html)");
        System.out.println("\nExample:");
        System.out.println("  java ValidatorCLI orders.csv http://localhost:8080/api/rules http://localhost:8080/api/orders/{{orderNumber}}");
        System.out.println();
    }
}
