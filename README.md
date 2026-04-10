# Order Rules Validator

A Java-based validation framework that checks whether business rules are correctly applied to live orders. This tool validates compliance based on rules stored in an external API and order data from CSV files.

## Features

✅ **Dynamic Business Rules** - Load rules from REST API endpoints  
✅ **CSV Order Processing** - Process orders from CSV files  
✅ **Markup Validation** - Verify markup pricing rules are applied correctly  
✅ **Flight Hiding Rules** - Validate flight visibility rules based on conditions  
✅ **Flexible Rule Engine** - Support for various conditions (==, >, <)  
✅ **Comprehensive Reports** - Generate CSV and HTML compliance reports  
✅ **Automated Testing** - Full JUnit test suite included  
✅ **CLI Tool** - Standalone command-line validator  

## Business Rules Supported

### Rule Types

| Rule Type | Example | Description |
|-----------|---------|-------------|
| **Markup** | `APPLY_MARKUP:5` | Apply 5% price markup if condition matches |
| **Flight Hiding** | `HIDE_FLIGHT` | Hide flight from user if condition matches |

### Condition Types

| Condition | Example | Meaning |
|-----------|---------|---------|
| **Equality** | `destination == SA` | Field must equal specific value |
| **Greater Than** | `layover > 720` | Numeric field must be greater than threshold |
| **Less Than** | `layover < 120` | Numeric field must be less than threshold |

## Project Structure

```
order-rules-validator/
├── src/
│   ├── main/java/com/ordervalidation/
│   │   ├── model/               # Data models (Order, BusinessRule, ValidationResult)
│   │   ├── engine/              # RulesEngine - applies rules to orders
│   │   ├── validator/           # OrderValidator - checks rule compliance
│   │   ├── report/              # ComplianceReportGenerator - CSV/HTML reports
│   │   ├── util/                # CsvReaderUtil, ApiClientUtil
│   │   └── cli/                 # ValidatorCLI - command-line interface
│   └── test/java/com/ordervalidation/
│       ├── OrderValidatorTests.java
│       └── OrderValidatorIntegrationTests.java
├── pom.xml                       # Maven configuration
├── sample-orders.csv             # Example order data
├── sample-business-rules.json    # Example business rules
└── README.md                     # This file
```

## Dependencies

- **Java 21+**
- **Jackson** - JSON processing
- **OkHttp3** - HTTP client
- **Apache Commons CSV** - CSV parsing
- **SLF4J/Logback** - Logging
- **JUnit 5** - Testing framework

## Installation & Build

```bash
# Clone/navigate to project
cd order-rules-validator

# Build the project
mvn clean package

# Run tests
mvn test

# Create executable JAR
mvn clean package -DskipTests
```

## Usage

### 1. As a Standalone CLI Tool

```bash
java -jar target/order-rules-validator-1.0.0.jar \
  sample-orders.csv \
  http://localhost:8080/api/business-rules \
  http://localhost:8080/api/orders/{{orderNumber}} \
  compliance-report.csv \
  compliance-report.html
```

**Arguments:**
- `<orders-csv>` - Path to CSV file with order data
- `<rules-api-url>` - API endpoint returning business rules (JSON array)
- `[order-api-template]` - Optional: API URL template to fetch order details
- `[output-csv]` - Optional: Output CSV report (default: compliance-report.csv)
- `[output-html]` - Optional: Output HTML report (default: compliance-report.html)

### 2. As a Java Library

```java
import com.ordervalidation.engine.RulesEngine;
import com.ordervalidation.model.BusinessRule;
import com.ordervalidation.model.Order;
import com.ordervalidation.validator.OrderValidator;
import com.ordervalidation.util.*;

// Load orders from CSV
List<Order> orders = CsvReaderUtil.readOrders("orders.csv");

// Fetch rules from API
List<BusinessRule> rules = ApiClientUtil.fetchBusinessRules("http://api/rules");

// Initialize engine
RulesEngine engine = new RulesEngine(rules);
OrderValidator validator = new OrderValidator(engine);

// Validate orders
List<ValidationResult> results = orders.stream()
    .map(validator::validateOrder)
    .toList();

// Generate reports
ComplianceReportGenerator.generateCsvReport(results, "report.csv");
ComplianceReportGenerator.generateHtmlReport(results, "report.html");
ComplianceReportGenerator.printSummary(results);
```

### 3. Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OrderValidatorTests

# Run with coverage
mvn test jacoco:report
```

## API Specifications

### Business Rules API Response

Expected JSON array format:

```json
[
  {
    "ruleId": "RULE_001",
    "ruleName": "SA Destination Markup",
    "condition": "destination == SA",
    "action": "APPLY_MARKUP:5",
    "priority": 1,
    "enabled": true
  },
  {
    "ruleId": "RULE_002",
    "ruleName": "Long Layover Hide",
    "condition": "layover > 720",
    "action": "HIDE_FLIGHT",
    "priority": 2,
    "enabled": true
  }
]
```

### Order Details API Response (Optional)

```json
{
  "orderNumber": "ORD001",
  "nexusSessionId": "SESSION_ABC",
  "markup": 5.00,
  "visible": true,
  "destination": "SA",
  "layover": 300
}
```

## CSV Format

### Input Orders CSV

Required columns: `orderNumber`, `nexusSessionId`  
Additional columns are stored as order attributes

```csv
orderNumber,nexusSessionId,destination,layover,price,visible,markup
ORD001,SESSION_ABC,SA,300,100.00,true,5.00
ORD002,SESSION_DEF,US,250,150.00,true,0.00
ORD003,SESSION_GHI,UK,900,200.00,false,0.00
```

### Output Compliance Report CSV

```csv
Order Number,Status,Rules Applied,Failure Reasons
ORD001,PASS,3,N/A
ORD002,FAIL,3,"Markup mismatch - Expected: 0.00%, Actual: 2.00%"
ORD003,PASS,3,N/A
```

## Report Formats

### CSV Report
Simple summary: Order Number, Status (PASS/FAIL), Rules Applied count, Failure Reasons

### HTML Report
- Executive summary with pass/fail percentages
- Detailed table with per-order status
- Color-coded results (green for PASS, red for FAIL)
- Timestamp of report generation

## Example Workflow

### Step 1: Prepare Data

```bash
# Create CSV file with orders
cat > orders.csv << EOF
orderNumber,nexusSessionId,destination,layover
ORD001,SESSION_ABC,SA,300
ORD002,SESSION_DEF,UK,900
EOF
```

### Step 2: Set Up Rules API

Create an endpoint that returns business rules:
```
GET /api/business-rules
Response: [{"ruleId": "RULE_001", "condition": "destination == SA", ...}]
```

### Step 3: Run Validation

```bash
java -cp order-rules-validator-1.0.0.jar com.ordervalidation.cli.ValidatorCLI \
  orders.csv \
  http://localhost:8080/api/business-rules \
  http://localhost:8080/api/orders/{{orderNumber}}
```

### Step 4: Review Reports

- `compliance-report.csv` - Machine-readable results
- `compliance-report.html` - Human-readable visualization
- Console output - Quick summary

## Testing

The project includes comprehensive test coverage:

- **OrderValidatorTests** - Unit tests for rule application
  - Markup rule validation
  - Flight hiding rule validation
  - Multi-rule scenarios
  - Edge cases (missing attributes, invalid conditions)
  - Disabled rule filtering

- **OrderValidatorIntegrationTests** - Integration test placeholders

Run tests with:
```bash
mvn test
```

## Configuration

### Logging

Configure logging in `logback.xml` (create in `src/main/resources/`):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    <root level="INFO">
        <appender-ref ref="STDOUT" />
    </root>
</configuration>
```

### Custom Rules

To add custom business rules, implement a rule evaluator in `RulesEngine.evaluateCondition()`:

```java
if (condition.contains("IN")) {
    // Handle: field IN value1,value2,value3
    String[] parts = condition.split("IN");
    String key = parts[0].trim();
    String[] values = parts[1].trim().split(",");
    Object value = order.getAttribute(key);
    return Arrays.asList(values).contains(value.toString());
}
```

## Extending the Framework

### Custom Condition Types

Modify `RulesEngine.evaluateCondition()` to support additional operators.

### Custom Report Formats

Extend `ComplianceReportGenerator` to create XML, PDF, or other formats.

### Custom Actions

Add new action types in `BusinessRule` model and implement in validators.

## Performance Considerations

- **Concurrency**: API calls for order details can be parallelized using `.parallel()` stream
- **Large CSV Files**: Process in batches to reduce memory footprint
- **API Rate Limiting**: Add retry logic with backoff in `ApiClientUtil`

## Troubleshooting

### "API call failed with status: 404"
- Verify the API endpoint URL is correct
- Check API is running and accessible
- Review API response format matches expected JSON structure

### "CSV file not found"
- Verify CSV file path is absolute or relative to execution directory
- Check file permissions

### "No rules loaded"
- Ensure API returns non-empty JSON array
- Check rule `enabled` flag is `true`

### "Validation results unexpected"
- Review business rule conditions - ensure they match order attributes
- Check order CSV has required attributes for rule evaluation
- Enable DEBUG logging in logback configuration

## License

MIT

## Support

For issues or questions:
1. Review the sample data in `sample-orders.csv` and `sample-business-rules.json`
2. Enable DEBUG logging in logback configuration
3. Check test cases in `OrderValidatorTests.java` for usage examples
