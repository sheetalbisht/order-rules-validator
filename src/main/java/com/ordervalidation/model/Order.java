package com.ordervalidation.model;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an order read from CSV file.
 * Dynamically stores attributes since CSV structure may vary.
 */
public class Order {
    private String orderNumber;
    private String nexusSessionId;
    private final Map<String, Object> attributes = new HashMap<>();

    public Order(String orderNumber, String nexusSessionId) {
        this.orderNumber = orderNumber;
        this.nexusSessionId = nexusSessionId;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public String getNexusSessionId() {
        return nexusSessionId;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    public Map<String, Object> getAllAttributes() {
        return new HashMap<>(attributes);
    }

    @Override
    public String toString() {
        return "Order{" +
                "orderNumber='" + orderNumber + '\'' +
                ", nexusSessionId='" + nexusSessionId + '\'' +
                ", attributes=" + attributes +
                '}';
    }
}
