package com.ordervalidation.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a business rule retrieved from API.
 * Example: Apply 5% markup if destination is SA, Hide flights with layover > 720 min
 */
public class BusinessRule {
    @JsonProperty("ruleId")
    private String ruleId;

    @JsonProperty("ruleName")
    private String ruleName;

    @JsonProperty("condition")
    private String condition;  // e.g., "destination == SA", "layover > 720"

    @JsonProperty("action")
    private String action;  // e.g., "APPLY_MARKUP:5", "HIDE_FLIGHT"

    @JsonProperty("priority")
    private int priority;

    @JsonProperty("enabled")
    private boolean enabled;

    public BusinessRule() {
    }

    public BusinessRule(String ruleId, String ruleName, String condition, String action, int priority) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.condition = condition;
        this.action = action;
        this.priority = priority;
        this.enabled = true;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String toString() {
        return "BusinessRule{" +
                "ruleId='" + ruleId + '\'' +
                ", ruleName='" + ruleName + '\'' +
                ", condition='" + condition + '\'' +
                ", action='" + action + '\'' +
                ", priority=" + priority +
                ", enabled=" + enabled +
                '}';
    }
}
