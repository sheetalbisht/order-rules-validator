package com.ordervalidation.util;

import com.ordervalidation.model.Order;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to read orders from CSV file.
 */
public class CsvReaderUtil {
    private static final Logger logger = LoggerFactory.getLogger(CsvReaderUtil.class);

    /**
     * Read orders from CSV file.
     * Expected columns: orderNumber, nexusSessionId
     *
     * @param csvFile Path to CSV file
     * @return List of Order objects
     * @throws IOException if file cannot be read
     */
    public static List<Order> readOrders(String csvFile) throws IOException {
        List<Order> orders = new ArrayList<>();

        try (FileReader reader = new FileReader(csvFile);
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            for (CSVRecord record : csvParser) {
                String orderNumber = record.get("orderNumber");
                String nexusSessionId = record.get("nexusSessionId");

                if (orderNumber == null || orderNumber.trim().isEmpty()) {
                    logger.warn("Skipping record with empty orderNumber");
                    continue;
                }

                Order order = new Order(orderNumber, nexusSessionId != null ? nexusSessionId : "");

                // Store all additional attributes
                for (String header : csvParser.getHeaderMap().keySet()) {
                    if (!header.equals("orderNumber") && !header.equals("nexusSessionId")) {
                        order.setAttribute(header, record.get(header));
                    }
                }

                orders.add(order);
                logger.debug("Loaded order: {}", orderNumber);
            }
        }

        logger.info("Successfully loaded {} orders from CSV", orders.size());
        return orders;
    }
}
