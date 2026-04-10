package com.ordervalidation;

import com.ordervalidation.engine.RulesEngine;
import com.ordervalidation.model.BusinessRule;
import com.ordervalidation.model.Order;
import com.ordervalidation.validator.OrderValidator;
import com.ordervalidation.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Order Rules Validator.
 */
@DisplayName("Order Rules Validator Tests")
class OrderValidatorTests {
    private RulesEngine rulesEngine;
    private OrderValidator validator;

    @BeforeEach
    void setUp() {
        List<BusinessRule> rules = createTestRules();
        rulesEngine = new RulesEngine(rules);
        validator = new OrderValidator(rulesEngine);
    }

    private List<BusinessRule> createTestRules() {
        List<BusinessRule> rules = new ArrayList<>();
        
        // Rule 1: Apply 5% markup if destination is SA
        rules.add(new BusinessRule("RULE_001", "SA Markup", "destination == SA", "APPLY_MARKUP:5", 1));
        
        // Rule 2: Hide flight if layover > 720 minutes (12 hours)
        rules.add(new BusinessRule("RULE_002", "Long Layover Hide", "layover > 720", "HIDE_FLIGHT", 2));
        
        // Rule 3: Apply 2% markup if destination is AE
        rules.add(new BusinessRule("RULE_003", "AE Markup", "destination == AE", "APPLY_MARKUP:2", 1));
        
        return rules;
    }

    @Test
    @DisplayName("Should apply SA markup rule correctly")
    void testSaMarkupRule() {
        Order order = new Order("ORD001", "SESSION_ABC");
        order.setAttribute("destination", "SA");
        order.setAttribute("price", 100.0);
        order.setAttribute("layover", 300);

        Map<String, String> appliedRules = rulesEngine.applyRules(order);
        
        assertTrue(appliedRules.containsKey("RULE_001"), "SA markup rule should apply");
        assertEquals("APPLY_MARKUP:5", appliedRules.get("RULE_001"));
        assertEquals(5.0, rulesEngine.calculateTotalMarkup(appliedRules));
    }

    @Test
    @DisplayName("Should not apply SA markup rule for other destinations")
    void testSaMarkupRuleNotApplied() {
        Order order = new Order("ORD002", "SESSION_DEF");
        order.setAttribute("destination", "US");
        order.setAttribute("layover", 300);

        Map<String, String> appliedRules = rulesEngine.applyRules(order);
        
        assertFalse(appliedRules.containsKey("RULE_001"), "SA markup rule should not apply");
    }

    @Test
    @DisplayName("Should hide flight with long layover")
    void testFlightHidingRule() {
        Order order = new Order("ORD003", "SESSION_GHI");
        order.setAttribute("destination", "UK");
        order.setAttribute("layover", 900);  // 15 hours

        Map<String, String> appliedRules = rulesEngine.applyRules(order);
        
        assertTrue(appliedRules.containsKey("RULE_002"), "Long layover rule should apply");
        assertTrue(rulesEngine.shouldHideFlight(appliedRules), "Flight should be hidden");
    }

    @Test
    @DisplayName("Should not hide flight with short layover")
    void testFlightHidingRuleNotApplied() {
        Order order = new Order("ORD004", "SESSION_JKL");
        order.setAttribute("destination", "UK");
        order.setAttribute("layover", 300);  // 5 hours

        Map<String, String> appliedRules = rulesEngine.applyRules(order);
        
        assertFalse(appliedRules.containsKey("RULE_002"), "Long layover rule should not apply");
        assertFalse(rulesEngine.shouldHideFlight(appliedRules), "Flight should not be hidden");
    }

    @Test
    @DisplayName("Should apply multiple rules to order")
    void testMultipleRulesApplication() {
        Order order = new Order("ORD005", "SESSION_MNO");
        order.setAttribute("destination", "SA");
        order.setAttribute("layover", 900);

        Map<String, String> appliedRules = rulesEngine.applyRules(order);
        
        assertEquals(2, appliedRules.size(), "Two rules should apply");
        assertTrue(appliedRules.containsKey("RULE_001"), "SA markup rule should apply");
        assertTrue(appliedRules.containsKey("RULE_002"), "Long layover rule should apply");
        assertEquals(5.0, rulesEngine.calculateTotalMarkup(appliedRules));
        assertTrue(rulesEngine.shouldHideFlight(appliedRules));
    }

    @Test
    @DisplayName("Should apply combined markup from multiple rules")
    void testCombinedMarkup() {
        Order order = new Order("ORD006", "SESSION_PQR");
        order.setAttribute("destination", "SA");

        // Manually add another markup rule
        List<BusinessRule> testRules = new ArrayList<>();
        testRules.add(new BusinessRule("RULE_001", "SA Markup", "destination == SA", "APPLY_MARKUP:5", 1));
        testRules.add(new BusinessRule("RULE_004", "Premium Markup", "destination == SA", "APPLY_MARKUP:3", 2));
        
        RulesEngine testEngine = new RulesEngine(testRules);
        Map<String, String> appliedRules = testEngine.applyRules(order);
        
        assertEquals(2, appliedRules.size());
        assertEquals(8.0, testEngine.calculateTotalMarkup(appliedRules), "Total markup should be 8%");
    }

    @Test
    @DisplayName("Should validate order with passing rules")
    void testOrderValidationPass() {
        Order order = new Order("ORD007", "SESSION_STU");
        order.setAttribute("destination", "SA");
        order.setAttribute("layover", 300);

        ValidationResult result = validator.validateOrder(order);
        
        assertTrue(result.isPassed(), "Order should pass validation");
        // Order has markup rule applied, so should have rule results
        assertTrue(!result.getRuleResults().isEmpty() || result.getFailureReasons().isEmpty());
    }

    @Test
    @DisplayName("Should handle orders with missing attributes")
    void testOrderWithMissingAttributes() {
        Order order = new Order("ORD008", "SESSION_VWX");
        // No attributes set

        Map<String, String> appliedRules = rulesEngine.applyRules(order);
        
        assertTrue(appliedRules.isEmpty(), "No rules should apply with missing attributes");
    }

    @Test
    @DisplayName("Should handle invalid rule conditions gracefully")
    void testInvalidRuleCondition() {
        List<BusinessRule> rules = new ArrayList<>();
        rules.add(new BusinessRule("RULE_ERR", "Invalid", "invalid_condition", "APPLY_MARKUP:5", 1));
        
        RulesEngine testEngine = new RulesEngine(rules);
        Order order = new Order("ORD009", "SESSION_YZA");
        order.setAttribute("parameter", "value");

        Map<String, String> appliedRules = testEngine.applyRules(order);
        
        assertTrue(appliedRules.isEmpty(), "Invalid rule should not apply");
    }

    @Test
    @DisplayName("Rule engine should filter disabled rules")
    void testDisabledRulesFiltering() {
        List<BusinessRule> rules = new ArrayList<>();
        BusinessRule enabledRule = new BusinessRule("RULE_E1", "Enabled", "destination == SA", "APPLY_MARKUP:5", 1);
        enabledRule.setEnabled(true);
        
        BusinessRule disabledRule = new BusinessRule("RULE_D1", "Disabled", "destination == US", "HIDE_FLIGHT", 2);
        disabledRule.setEnabled(false);
        
        rules.add(enabledRule);
        rules.add(disabledRule);
        
        RulesEngine testEngine = new RulesEngine(rules);
        assertEquals(1, testEngine.getRules().size(), "Only enabled rules should be loaded");
    }

    @Test
    @DisplayName("Should correctly parse AE destination markup")
    void testAeMarkupRule() {
        Order order = new Order("ORD010", "SESSION_BCD");
        order.setAttribute("destination", "AE");

        Map<String, String> appliedRules = rulesEngine.applyRules(order);
        
        assertTrue(appliedRules.containsKey("RULE_003"));
        assertEquals(2.0, rulesEngine.calculateTotalMarkup(appliedRules));
    }
}
