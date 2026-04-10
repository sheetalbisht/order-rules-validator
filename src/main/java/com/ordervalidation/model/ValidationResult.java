package com.ordervalidation.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the validation result for a single order against all business rules.
 */
public class ValidationResult {
    private String orderNumber;
    private boolean passed;
    private final List<RuleCheckResult> ruleResults = new ArrayList<>();
    private final List<String> appliedRuleNames = new ArrayList<>();
    private List<String> failureReasons = new ArrayList<>();

    public ValidationResult(String orderNumber) {
        this.orderNumber = orderNumber;
        this.passed = true;
    }

    public void addRuleResult(RuleCheckResult result) {
        ruleResults.add(result);
        if (!result.isPassed()) {
            passed = false;
            failureReasons.add(result.getReason());
        }
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public boolean isPassed() {
        return passed;
    }

    public List<RuleCheckResult> getRuleResults() {
        return new ArrayList<>(ruleResults);
    }

    public void addAppliedRuleName(String ruleName) {
        if (ruleName != null && !ruleName.isBlank()) {
            appliedRuleNames.add(ruleName);
        }
    }

    public List<String> getAppliedRuleNames() {
        return new ArrayList<>(appliedRuleNames);
    }

    public List<String> getFailureReasons() {
        return new ArrayList<>(failureReasons);
    }

    @Override
    public String toString() {
        return "ValidationResult{" +
                "orderNumber='" + orderNumber + '\'' +
                ", passed=" + passed +
                ", ruleResults=" + ruleResults +
                ", failureReasons=" + failureReasons +
                '}';
    }

    /**
     * Represents the result of checking a single rule against an order.
     */
    public static class RuleCheckResult {
        private String ruleId;
        private String ruleName;
        private boolean passed;
        private String reason;
        private Object expectedValue;
        private Object actualValue;

        public RuleCheckResult(String ruleId, String ruleName) {
            this.ruleId = ruleId;
            this.ruleName = ruleName;
            this.passed = true;
        }

        public void setFailed(String reason, Object expected, Object actual) {
            this.passed = false;
            this.reason = reason;
            this.expectedValue = expected;
            this.actualValue = actual;
        }

        public String getRuleId() {
            return ruleId;
        }

        public String getRuleName() {
            return ruleName;
        }

        public boolean isPassed() {
            return passed;
        }

        public String getReason() {
            return reason;
        }

        public Object getExpectedValue() {
            return expectedValue;
        }

        public Object getActualValue() {
            return actualValue;
        }

        @Override
        public String toString() {
            return "RuleCheckResult{" +
                    "ruleId='" + ruleId + '\'' +
                    ", ruleName='" + ruleName + '\'' +
                    ", passed=" + passed +
                    ", reason='" + reason + '\'' +
                    ", expectedValue=" + expectedValue +
                    ", actualValue=" + actualValue +
                    '}';
        }
    }
}
