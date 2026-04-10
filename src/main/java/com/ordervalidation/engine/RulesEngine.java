package com.ordervalidation.engine;

import com.ordervalidation.model.BusinessRule;
import com.ordervalidation.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business Rules Engine - applies rules to orders based on conditions.
 * Supports markup calculations and flight hiding logic.
 */
public class RulesEngine {
    private static final Logger logger = LoggerFactory.getLogger(RulesEngine.class);
    private final List<BusinessRule> rules;

    public RulesEngine(List<BusinessRule> rules) {
        this.rules = rules.stream().filter(BusinessRule::isEnabled).toList();
        logger.info("Initialized RulesEngine with {} enabled rules", this.rules.size());
    }

    /**
     * Apply all business rules to an order and return applied rules.
     *
     * @param order Order to process
     * @return Map of rule IDs to applied actions
     */
    public Map<String, String> applyRules(Order order) {
        Map<String, String> appliedRules = new HashMap<>();

        for (BusinessRule rule : rules) {
            try {
                if (evaluateCondition(rule.getCondition(), order)) {
                    String appliedAction = rule.getAction();
                    appliedRules.put(rule.getRuleId(), appliedAction);
                    logger.debug("Rule {} ({}) applied to order {}", 
                            rule.getRuleId(), rule.getRuleName(), order.getOrderNumber());
                }
            } catch (Exception e) {
                logger.error("Error evaluating rule {}: {}", rule.getRuleId(), e.getMessage());
            }
        }

        return appliedRules;
    }

    /**
     * Calculate markup percentage for an order based on rules.
     * Example action format: "APPLY_MARKUP:5" means 5% markup
     *
     * @param appliedRules Map of rules applied to the order
     * @return Total markup percentage
     */
    public double calculateTotalMarkup(Map<String, String> appliedRules) {
        return appliedRules.values().stream()
                .filter(action -> action.startsWith("APPLY_MARKUP:"))
                .mapToDouble(action -> {
                    try {
                        return Double.parseDouble(action.substring("APPLY_MARKUP:".length()));
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid markup value: {}", action);
                        return 0.0;
                    }
                })
                .sum();
    }

    /**
     * Check if flight should be hidden based on rules.
     *
     * @param appliedRules Map of rules applied to the order
     * @return true if flight should be hidden, false otherwise
     */
    public boolean shouldHideFlight(Map<String, String> appliedRules) {
        return appliedRules.values().stream()
                .anyMatch(action -> action.equalsIgnoreCase("HIDE_FLIGHT"));
    }

    /**
     * Evaluate a condition string against an order.
     * Simple parser for conditions like "destination == SA" or "layover > 720"
     *
     * @param condition Condition string
     * @param order Order to evaluate against
     * @return true if condition matches
     */
    private boolean evaluateCondition(String condition, Order order) {
        if (condition == null || condition.trim().isEmpty()) {
            return false;
        }

        try {
            // Handle "destination == SA"
            if (condition.contains("==")) {
                String[] parts = condition.split("==");
                String key = parts[0].trim();
                String expectedValue = parts[1].trim();
                Object actualValue = order.getAttribute(key);
                return actualValue != null && actualValue.toString().equals(expectedValue);
            }

            // Handle "layover > 720"
            if (condition.contains(">")) {
                String[] parts = condition.split(">");
                String key = parts[0].trim();
                int threshold = Integer.parseInt(parts[1].trim());
                Object value = order.getAttribute(key);
                if (value != null) {
                    try {
                        int intValue = Integer.parseInt(value.toString());
                        return intValue > threshold;
                    } catch (NumberFormatException e) {
                        logger.warn("Cannot parse {} as integer", value);
                        return false;
                    }
                }
            }

            // Handle "layover < 120"
            if (condition.contains("<")) {
                String[] parts = condition.split("<");
                String key = parts[0].trim();
                int threshold = Integer.parseInt(parts[1].trim());
                Object value = order.getAttribute(key);
                if (value != null) {
                    try {
                        int intValue = Integer.parseInt(value.toString());
                        return intValue < threshold;
                    } catch (NumberFormatException e) {
                        logger.warn("Cannot parse {} as integer", value);
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error evaluating condition '{}': {}", condition, e.getMessage());
        }

        return false;
    }

    public List<BusinessRule> getRules() {
        return new ArrayList<>(rules);
    }
}
