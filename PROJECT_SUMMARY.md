# Order Rules Validator - Project Summary

## ✅ Project Created Successfully

A complete **Java-based Business Rules Compliance Validator** has been created to validate if business rules (markup pricing, flight hiding) are correctly applied to live orders.

### Project Location
```
/Users/sheetalbargali/Workspace/Projects/order-rules-validator/
```

---

## 📦 What Was Built

### Core Framework

| Component | Purpose | Status |
|-----------|---------|--------|
| **RulesEngine** | Loads rules from API, applies conditions, calculates markup | ✅ Complete |
| **OrderValidator** | Validates if rules were applied correctly to orders | ✅ Complete |
| **ComplianceReportGenerator** | Generates CSV and HTML compliance reports | ✅ Complete |
| **CLI Tool** | Command-line interface for batch validation | ✅ Complete |
| **Test Suite** | 13 JUnit tests covering all functionality | ✅ All Passing |

### Models

| Model | Description |
|-------|-------------|
| `Order` | Represents an order from CSV with dynamic attributes |
| `BusinessRule` | Rule with condition, action, priority, enabled flag |
| `ValidationResult` | Per-order validation results with pass/fail status |

### Utilities

| Utility | Purpose |
|---------|---------|
| `CsvReaderUtil` | Reads orders from CSV files, handles flexible headers |
| `ApiClientUtil` | Fetches business rules from REST API endpoints |

---

## 🎯 Key Features

### Business Rules Engine
✅ **Conditions Supported:**
- Equality: `destination == SA`
- Greater Than: `layover > 720`
- Less Than: `layover < 120`

✅ **Actions Supported:**
- `APPLY_MARKUP:5` - Apply 5% pricing markup
- `HIDE_FLIGHT` - Hide flight from customer

✅ **Rule Engine:**
- Dynamically fetches rules from API
- Filters disabled rules automatically
- Calculates combined markup percentages
- Determines flight visibility

### Validation
✅ Field-based attribute matching  
✅ Numeric condition evaluation  
✅ Combined rule application  
✅ Flexible order data structure  

### Reporting
✅ CSV reports for machine processing  
✅ HTML reports with color-coded results  
✅ Console summary with pass/fail percentages  
✅ Per-order failure reasons  

---

## 📊 Test Coverage

```
Tests Run: 13
  ✓ All Passing (100%)
  ✓ 0 Failures
  ✓ 0 Errors
```

### Test Categories
- ✅ Markup rule application
- ✅ Flight hiding rules
- ✅ Multiple rules on single order
- ✅ Combined markup calculations
- ✅ Disabled rule filtering
- ✅ Missing attribute handling
- ✅ Invalid condition gracefully handled
- ✅ Order validation workflow

---

## 🚀 Quick Start

### Build
```bash
cd /Users/sheetalbargali/Workspace/Projects/order-rules-validator
mvn clean package
```

### Run Tests
```bash
mvn test
```

### Run as CLI Tool
```bash
java -cp target/order-rules-validator-1.0.0.jar \
  com.ordervalidation.cli.ValidatorCLI \
  sample-orders.csv \
  http://localhost:8080/api/business-rules \
  http://localhost:8080/api/orders/{{orderNumber}}
```

### Use as Library
```java
List<Order> orders = CsvReaderUtil.readOrders("orders.csv");
List<BusinessRule> rules = ApiClientUtil.fetchBusinessRules(apiUrl);
RulesEngine engine = new RulesEngine(rules);
OrderValidator validator = new OrderValidator(engine);
List<ValidationResult> results = orders.stream()
    .map(validator::validateOrder)
    .toList();
ComplianceReportGenerator.generateCsvReport(results, "report.csv");
```

---

## 📁 Project Structure

```
order-rules-validator/
├── src/main/java/com/ordervalidation/
│   ├── model/
│   │   ├── Order.java
│   │   ├── BusinessRule.java
│   │   └── ValidationResult.java
│   ├── engine/
│   │   └── RulesEngine.java            (applies rules to orders)
│   ├── validator/
│   │   └── OrderValidator.java          (validates compliance)
│   ├── report/
│   │   └── ComplianceReportGenerator.java (CSV/HTML reports)
│   ├── util/
│   │   ├── CsvReaderUtil.java           (load orders from CSV)
│   │   └── ApiClientUtil.java           (fetch rules from API)
│   └── cli/
│       └── ValidatorCLI.java            (standalone tool)
├── src/test/java/com/ordervalidation/
│   ├── OrderValidatorTests.java         (unit tests - 11 tests)
│   └── OrderValidatorIntegrationTests.java (integration tests - 2 tests)
├── src/main/resources/
│   └── application.properties
├── pom.xml                              (Maven configuration)
├── sample-orders.csv                    (example orders)
├── sample-business-rules.json           (example rules)
├── target/
│   └── order-rules-validator-1.0.0.jar  (executable JAR)
└── README.md                            (detailed documentation)
```

---

## 🔧 Dependencies

```
- Java 21+
- Jackson 2.17.0 (JSON processing)
- OkHttp3 4.12.0 (HTTP client)
- Apache Commons CSV 1.10.0 (CSV parsing)
- SLF4J + Logback (logging)
- JUnit 5.10.1 (testing)
- Mockito 5.7.0 (mocking)
```

---

## 💡 Example Business Rules

The system supports rules like:

```json
{
  "ruleId": "RULE_001",
  "ruleName": "SA Destination Markup",
  "condition": "destination == SA",
  "action": "APPLY_MARKUP:5",
  "priority": 1,
  "enabled": true
}
```

**Sample Order Data:**
```csv
orderNumber,nexusSessionId,destination,layover,price,visible,markup
ORD001,SESSION_ABC,SA,300,100.00,true,5.00
ORD002,SESSION_DEF,UK,900,200.00,false,0.00
```

---

## 📋 API Contracts

### Business Rules API
**Request:** `GET /api/business-rules`  
**Response:** JSON array of BusinessRule objects

### Order Details API (Optional)
**Request:** `GET /api/orders/{{orderNumber}}`  
**Response:** Order object with `markup`, `visible` fields

---

## 🎓 Architecture Highlights

### Separation of Concerns
- **Engine Layer**: Applies business logic
- **Validator Layer**: Checks compliance
- **Report Layer**: Formats output
- **Util Layer**: External integrations

### Flexibility
- Dynamic CSV column support
- Pluggable API clients
- Extensible condition evaluator
- Multiple report formats

### Robustness
- Exception handling at every level
- Graceful API failure handling
- Detailed logging at all stages
- Comprehensive error messages

---

## ✨ Next Steps / Enhancement Ideas

1. **Batch Processing** - Add parallel stream processing for large datasets
2. **More Conditions** - Add `IN`, `CONTAINS`, `BETWEEN` operators
3. **Custom Actions** - Extend with discount pricing, bundling rules
4. **Database Support** - Load orders from JDBC, save results to DB
5. **Performance** - Add caching layer for API calls
6. **REST API** - Expose validator as a web service
7. **UI Dashboard** - Create web dashboard to view compliance reports
8. **Audit Trail** - Log all rule applications and decisions
9. **A/B Testing** - Support rule variations for testing

---

## 📞 Support

- **Configuration:** See `README.md` for detailed setup
- **Examples:** Check `sample-orders.csv` and `sample-business-rules.json`
- **Testing:** Run `mvn test` to validate
- **Debugging:** Enable DEBUG logging in logback configuration

---

## 🎉 Summary

A **production-ready** business rules validator has been successfully created with:
- ✅ Full Maven/Java 21 project structure
- ✅ Comprehensive business rules engine
- ✅ Flexible CSV/API integration
- ✅ Detailed compliance reporting (CSV + HTML)
- ✅ 13/13 passing unit tests
- ✅ CLI tool for batch processing
- ✅ Extensible architecture for future enhancements
- ✅ Complete documentation and examples

**Ready to validate your live orders for business rule compliance!**
