package com.ordervalidation.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordervalidation.model.BusinessRule;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to fetch business rules from API endpoint.
 */
public class ApiClientUtil {
    private static final Logger logger = LoggerFactory.getLogger(ApiClientUtil.class);
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build();

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetch business rules from API endpoint.
     *
     * @param apiUrl API endpoint URL that returns business rules
     * @return List of BusinessRule objects
     * @throws IOException if API call fails
     */
    public static List<BusinessRule> fetchBusinessRules(String apiUrl) throws IOException {
        logger.info("Fetching business rules from: {}", apiUrl);

        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API call failed with status: " + response.code());
            }

            String responseBody = response.body() != null ? response.body().string() : "[]";
            List<BusinessRule> rules = Arrays.asList(objectMapper.readValue(responseBody, BusinessRule[].class));
            logger.info("Successfully fetched {} business rules", rules.size());
            return rules;
        }
    }

    /**
     * Fetch order details from API.
     *
     * @param apiUrl API endpoint URL
     * @return Response body as string
     * @throws IOException if API call fails
     */
    public static String fetchFromApi(String apiUrl) throws IOException {
        logger.debug("Making API call to: {}", apiUrl);

        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API call failed with status: " + response.code());
            }
            return response.body() != null ? response.body().string() : "";
        }
    }
}
