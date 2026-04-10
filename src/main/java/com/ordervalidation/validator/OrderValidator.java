package com.ordervalidation.validator;

import com.ordervalidation.engine.RulesEngine;
import com.ordervalidation.model.Order;
import com.ordervalidation.model.ValidationResult;
import com.ordervalidation.util.ApiClientUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

/**
 * Order Validator - validates if business rules are correctly applied to orders.
 * Checks actual order state against expected rule applications.
 */
public class OrderValidator {
    private static final Logger logger = LoggerFactory.getLogger(OrderValidator.class);
    private final RulesEngine rulesEngine;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private String orderDetailsApiTemplate; // Template: /api/orders/{{orderNumber}}

    public OrderValidator(RulesEngine rulesEngine) {
        this.rulesEngine = rulesEngine;
    }

    public void setOrderDetailsApiTemplate(String template) {
        this.orderDetailsApiTemplate = template;
    }

    /**
     * Validate a single order - check if rules were applied correctly.
     *
     * @param order Order to validate
     * @return ValidationResult with pass/fail status and detailed rule results
     */
    public ValidationResult validateOrder(Order order) {
        ValidationResult result = new ValidationResult(order.getOrderNumber());

        try {
            // Step 1: Determine which rules should apply
            Map<String, String> appliedRules = rulesEngine.applyRules(order);
            logger.debug("Rules applicable to order {}: {}", order.getOrderNumber(), appliedRules);

            // Keep an explicit list of applied rule names for reporting.
            for (String ruleId : appliedRules.keySet()) {
                result.addAppliedRuleName(resolveRuleName(ruleId));
            }

            // Step 2: Fetch actual order details from API (if template provided)
            JsonNode actualOrderData = fetchOrderDetails(order);

               // Only validate against actual data if we have it
               if (actualOrderData != null) {
                   // Step 3: Validate markup if rules require it
                   if (shouldValidateMarkup(appliedRules)) {
                       validateMarkup(result, appliedRules, actualOrderData);
                   }

                   // Step 4: Validate flight visibility if rules require it
                   if (shouldValidateFlightHiding(appliedRules)) {
                       validateFlightVisibility(result, appliedRules, actualOrderData);
                   }
               } else {
                   logger.debug("No order data available for detailed validation of order {}", order.getOrderNumber());
               }

            logger.info("Validation for order {} completed - Passed: {}", 
                    order.getOrderNumber(), result.isPassed());
        } catch (Exception e) {
            logger.error("Error validating order {}: {}", order.getOrderNumber(), e.getMessage());
            ValidationResult.RuleCheckResult ruleResult = 
                    new ValidationResult.RuleCheckResult("SYSTEM", "VALIDATION_ERROR");
            ruleResult.setFailed("Validation error: " + e.getMessage(), null, null);
            result.addRuleResult(ruleResult);
        }

        return result;
    }

    /**
     * Validate that markup was correctly applied.
     */
    private void validateMarkup(ValidationResult result, 
                               Map<String, String> appliedRules, 
                               JsonNode orderData) {
        double expectedMarkup = rulesEngine.calculateTotalMarkup(appliedRules);

        // Extract actual markup from order data if available
        double actualMarkup = 0.0;
        if (orderData != null && orderData.has("markup")) {
            try {
                actualMarkup = orderData.get("markup").asDouble();
            } catch (Exception e) {
                logger.warn("Could not extract markup from order data");
            }
        }

        ValidationResult.RuleCheckResult markupCheck = 
                new ValidationResult.RuleCheckResult("MARKUP_RULE", "Markup Application");
        
        if (Math.abs(expectedMarkup - actualMarkup) < 0.01) {
            logger.info("Markup validation passed - Expected: {}, Actual: {}", 
                    expectedMarkup, actualMarkup);
        } else {
            markupCheck.setFailed(
                    String.format("Markup mismatch - Expected: %.2f%%, Actual: %.2f%%", 
                            expectedMarkup, actualMarkup),
                    expectedMarkup,
                    actualMarkup
            );
            logger.warn("Markup validation failed - Expected: {}, Actual: {}", 
                    expectedMarkup, actualMarkup);
        }

        result.addRuleResult(markupCheck);
    }

    /**
     * Validate that flights are hidden correctly.
     */
    private void validateFlightVisibility(ValidationResult result,
                                         Map<String, String> appliedRules,
                                         JsonNode orderData) {
        boolean shouldHide = rulesEngine.shouldHideFlight(appliedRules);

        // Extract visibility status from order data
        boolean isVisible = true;
        if (orderData != null && orderData.has("visible")) {
            isVisible = orderData.get("visible").asBoolean(true);
        } else if (orderData != null && orderData.has("hidden")) {
            isVisible = !orderData.get("hidden").asBoolean(false);
        }

        ValidationResult.RuleCheckResult visibilityCheck = 
                new ValidationResult.RuleCheckResult("FLIGHT_HIDING_RULE", "Flight Visibility");

        boolean valid = shouldHide == !isVisible; // shouldHide=true => visible=false

        if (valid) {
            logger.info("Flight visibility validation passed - ShouldHide: {}, IsHidden: {}", 
                    shouldHide, !isVisible);
        } else {
            visibilityCheck.setFailed(
                    String.format("Flight visibility mismatch - ShouldHide: %s, IsHidden: %s",
                            shouldHide, !isVisible),
                    shouldHide,
                    isVisible
            );
            logger.warn("Flight visibility validation failed");
        }

        result.addRuleResult(visibilityCheck);
    }

    /**
     * Fetch order details from API.
     */
    private JsonNode fetchOrderDetails(Order order) throws IOException {
        if (orderDetailsApiTemplate == null || orderDetailsApiTemplate.trim().isEmpty()) {
            logger.debug("No API template provided, skipping API fetch");
            return null;
        }

        String url = orderDetailsApiTemplate.replace("{{orderNumber}}", order.getOrderNumber())
                .replace("{{nexusSessionId}}", order.getNexusSessionId());

        try {
            String response = ApiClientUtil.fetchFromApi(url);
            return objectMapper.readTree(response);
        } catch (Exception e) {
            logger.error("Failed to fetch order details for {}: {}", order.getOrderNumber(), e.getMessage());
            return null;
        }
    }

    private boolean shouldValidateMarkup(Map<String, String> appliedRules) {
        return appliedRules.values().stream()
                .anyMatch(action -> action.startsWith("APPLY_MARKUP:"));
    }

    private boolean shouldValidateFlightHiding(Map<String, String> appliedRules) {
        return appliedRules.values().stream()
                .anyMatch(action -> action.equalsIgnoreCase("HIDE_FLIGHT"));
    }

    private String resolveRuleName(String ruleId) {
        return rulesEngine.getRules().stream()
                .filter(rule -> rule.getRuleId().equals(ruleId))
                .map(rule -> rule.getRuleId() + ": " + rule.getRuleName())
                .findFirst()
                .orElse(ruleId);
    }
}
