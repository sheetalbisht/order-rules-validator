package com.ordervalidation.cli;

import com.ordervalidation.engine.RulesEngine;
import com.ordervalidation.model.BusinessRule;
import com.ordervalidation.model.Order;
import com.ordervalidation.model.ValidationResult;
import com.ordervalidation.report.ComplianceReportGenerator;
import com.ordervalidation.util.ApiClientUtil;
import com.ordervalidation.util.CsvReaderUtil;
import com.ordervalidation.validator.OrderValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lightweight web server to upload CSV and run validation from browser.
 */
public class ValidatorWebServer {
    private static final Logger logger = LoggerFactory.getLogger(ValidatorWebServer.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 7070;
        Path reportsDir = Paths.get("reports").toAbsolutePath().normalize();
      Path defaultRulesFile = resolveDefaultRulesFile();

        try {
            Files.createDirectories(reportsDir);
        } catch (IOException e) {
            throw new RuntimeException("Unable to create reports directory", e);
        }

        Javalin app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.http.maxRequestSize = 20_000_000L;
        });

        app.get("/", ctx -> {
            ctx.contentType("text/html; charset=utf-8");
            ctx.result(indexHtml());
        });

        app.get("/health", ctx -> ctx.json(Map.of("ok", true)));

        app.post("/api/validate", ctx -> {
            UploadedFile ordersFile = ctx.uploadedFile("ordersFile");
            String rulesApiUrl = safeTrim(ctx.formParam("rulesApiUrl"));
            String orderApiTemplate = safeTrim(ctx.formParam("orderApiTemplate"));

            if (ordersFile == null) {
                ctx.status(400).json(Map.of("error", "ordersFile is required"));
                return;
            }

            Path tempCsv = Files.createTempFile("orders-upload-", ".csv");
            try {
                Files.copy(ordersFile.content(), tempCsv, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                List<Order> orders = CsvReaderUtil.readOrders(tempCsv.toString());
                List<BusinessRule> rules;
                if (rulesApiUrl != null && !rulesApiUrl.isEmpty()) {
                  rules = ApiClientUtil.fetchBusinessRules(rulesApiUrl);
                } else {
                  rules = loadBusinessRulesFromFile(defaultRulesFile);
                }

                RulesEngine rulesEngine = new RulesEngine(rules);
                OrderValidator validator = new OrderValidator(rulesEngine);
                if (orderApiTemplate != null && !orderApiTemplate.isEmpty()) {
                    validator.setOrderDetailsApiTemplate(orderApiTemplate);
                }

                List<ValidationResult> results = orders.stream().map(validator::validateOrder).toList();
                long passed = results.stream().filter(ValidationResult::isPassed).count();
                long failed = results.size() - passed;

                String stamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
                Path csvReport = reportsDir.resolve("compliance-report-" + stamp + ".csv");
                Path htmlReport = reportsDir.resolve("compliance-report-" + stamp + ".html");

                ComplianceReportGenerator.generateCsvReport(results, csvReport.toString());
                ComplianceReportGenerator.generateHtmlReport(results, htmlReport.toString());

                Map<String, Object> response = new LinkedHashMap<>();
                response.put("totalOrders", results.size());
                response.put("passed", passed);
                response.put("failed", failed);
                response.put("csvReport", "/reports/" + csvReport.getFileName());
                response.put("htmlReport", "/reports/" + htmlReport.getFileName());
                response.put("rulesSource", (rulesApiUrl != null && !rulesApiUrl.isEmpty())
                  ? "api"
                  : defaultRulesFile.toString());

                ctx.json(response);
            } catch (Exception e) {
                logger.error("Validation failed", e);
                ctx.status(500).json(Map.of("error", e.getMessage()));
            } finally {
                Files.deleteIfExists(tempCsv);
            }
        });

        app.get("/reports/{name}", ctx -> {
            String fileName = ctx.pathParam("name");
            Path reportFile = reportsDir.resolve(fileName).normalize();

            if (!reportFile.startsWith(reportsDir) || !Files.exists(reportFile) || Files.isDirectory(reportFile)) {
                ctx.status(404).result("Report not found");
                return;
            }

            if (fileName.endsWith(".html")) {
                ctx.contentType("text/html; charset=utf-8");
            } else if (fileName.endsWith(".csv")) {
                ctx.contentType("text/csv; charset=utf-8");
            } else {
                ctx.contentType("application/octet-stream");
            }

            ctx.result(Files.newInputStream(reportFile));
        });

        app.start(port);
        logger.info("Validator Web UI started at http://localhost:{}", port);
    }

    private static String safeTrim(String value) {
        return value == null ? null : value.trim();
    }

    private static String indexHtml() {
        return """
                <!doctype html>
                <html lang=\"en\">
                <head>
                  <meta charset=\"utf-8\" />
                  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />
                  <title>Order Rules Validator</title>
                  <style>
                    :root {
                      --bg: #f3f5f7;
                      --card: #ffffff;
                      --ink: #1f2937;
                      --line: #d1d5db;
                      --brand: #0b7285;
                      --ok: #2b8a3e;
                      --bad: #c92a2a;
                    }
                    body {
                      margin: 0;
                      font-family: \"Segoe UI\", Tahoma, sans-serif;
                      background: radial-gradient(circle at top, #dceef3, var(--bg) 42%);
                      color: var(--ink);
                    }
                    .wrap {
                      max-width: 860px;
                      margin: 32px auto;
                      padding: 0 16px;
                    }
                    .card {
                      background: var(--card);
                      border: 1px solid var(--line);
                      border-radius: 12px;
                      padding: 20px;
                      box-shadow: 0 8px 18px rgba(12, 25, 38, 0.08);
                    }
                    h1 {
                      margin: 0 0 16px;
                      font-size: 26px;
                    }
                    .hint {
                      margin: 0 0 20px;
                      color: #4b5563;
                    }
                    .grid {
                      display: grid;
                      gap: 12px;
                    }
                    label {
                      font-size: 14px;
                      font-weight: 600;
                    }
                    input[type=\"text\"], input[type=\"file\"] {
                      width: 100%;
                      box-sizing: border-box;
                      border: 1px solid var(--line);
                      border-radius: 8px;
                      padding: 10px;
                      font-size: 14px;
                      margin-top: 6px;
                    }
                    button {
                      border: 0;
                      border-radius: 9px;
                      padding: 11px 14px;
                      font-size: 14px;
                      font-weight: 700;
                      background: var(--brand);
                      color: #fff;
                      cursor: pointer;
                    }
                    button:disabled {
                      opacity: 0.65;
                      cursor: wait;
                    }
                    .result {
                      margin-top: 18px;
                      padding: 12px;
                      border-radius: 8px;
                      border: 1px solid var(--line);
                      background: #fafbfc;
                      display: none;
                    }
                    .good { color: var(--ok); }
                    .bad { color: var(--bad); }
                    .links a {
                      display: inline-block;
                      margin-right: 12px;
                      margin-top: 8px;
                      color: var(--brand);
                      font-weight: 700;
                    }
                  </style>
                </head>
                <body>
                  <div class=\"wrap\">
                    <div class=\"card\">
                      <h1>Order Rules Validator</h1>
                      <p class=\"hint\">Upload CSV and generate compliance reports. Rules API URL is optional.</p>

                      <form id=\"validator-form\" class=\"grid\">
                        <label>
                          Orders CSV
                          <input type=\"file\" name=\"ordersFile\" accept=\".csv\" required />
                        </label>

                        <label>
                          Rules API URL (optional)
                          <input type=\"text\" name=\"rulesApiUrl\" placeholder=\"https://api.example.com/business-rules\" />
                        </label>

                        <label>
                          Order Details API Template (optional)
                          <input type=\"text\" name=\"orderApiTemplate\" placeholder=\"https://api.example.com/orders/{{orderNumber}}\" />
                        </label>

                        <button id=\"runBtn\" type=\"submit\">Run Validation</button>
                      </form>

                      <div class=\"result\" id=\"result\"></div>
                    </div>
                  </div>

                  <script>
                    const form = document.getElementById('validator-form');
                    const runBtn = document.getElementById('runBtn');
                    const resultBox = document.getElementById('result');

                    form.addEventListener('submit', async (e) => {
                      e.preventDefault();
                      runBtn.disabled = true;
                      runBtn.textContent = 'Running...';
                      resultBox.style.display = 'none';

                      try {
                        const fd = new FormData(form);
                        const response = await fetch('/api/validate', {
                          method: 'POST',
                          body: fd
                        });

                        const data = await response.json();

                        if (!response.ok) {
                          throw new Error(data.error || 'Validation failed');
                        }

                        resultBox.innerHTML = `
                          <div class=\"good\"><strong>Validation complete</strong></div>
                          <div>Total Orders: ${data.totalOrders}</div>
                          <div>Passed: ${data.passed}</div>
                          <div>Failed: ${data.failed}</div>
                          <div>Rules Source: ${data.rulesSource}</div>
                          <div class=\"links\">
                            <a href=\"${data.csvReport}\" target=\"_blank\">Open CSV Report</a>
                            <a href=\"${data.htmlReport}\" target=\"_blank\">Open HTML Report</a>
                          </div>
                        `;
                      } catch (err) {
                        resultBox.innerHTML = `<div class=\"bad\"><strong>Error:</strong> ${err.message}</div>`;
                      } finally {
                        resultBox.style.display = 'block';
                        runBtn.disabled = false;
                        runBtn.textContent = 'Run Validation';
                      }
                    });
                  </script>
                </body>
                </html>
                """;
    }

          private static List<BusinessRule> loadBusinessRulesFromFile(Path rulesFile) throws IOException {
            if (!Files.exists(rulesFile)) {
              throw new IOException("Rules API URL not provided and fallback rules file not found: " + rulesFile);
            }

            String json = Files.readString(rulesFile);
            return Arrays.asList(OBJECT_MAPPER.readValue(json, BusinessRule[].class));
          }

          private static Path resolveDefaultRulesFile() {
            Path direct = Paths.get("sample-business-rules.json").toAbsolutePath().normalize();
            if (Files.exists(direct)) {
              return direct;
            }

            Path nested = Paths.get("order-rules-validator", "sample-business-rules.json")
                .toAbsolutePath()
                .normalize();
            if (Files.exists(nested)) {
              return nested;
            }

            return direct;
          }
}
