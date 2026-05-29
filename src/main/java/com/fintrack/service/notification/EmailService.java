package com.fintrack.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import jakarta.annotation.PostConstruct;

import org.springframework.http.*;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${mailgun.api.key}")
    private String apiKey;

    @Value("${mailgun.domain}")
    private String domain;

    @PostConstruct
    void debug() {
        System.out.println("Mailgun key loaded: " + apiKey);
    }

    private final org.springframework.web.client.RestTemplate restTemplate =
            new org.springframework.web.client.RestTemplate();

    @Async
    public void sendBudgetAlert(
            String toEmail,
            String displayName,
            String categoryName,
            BigDecimal budgetAmount,
            BigDecimal spentAmount,
            int thresholdPercent
    ) {
        try {
            String subject = thresholdPercent >= 100
                    ? "⚠️ Budget exceeded: " + categoryName
                    : "📊 80% of budget used: " + categoryName;

            String text = buildMessage(displayName, categoryName,
                    budgetAmount, spentAmount, thresholdPercent);

            String url = "https://api.mailgun.net/v3/" + domain + "/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth("api", apiKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("from", "FinTrack <mail@" + domain + ">");
            body.add("to", toEmail);
            body.add("subject", subject);
            body.add("text", text);

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(body, headers);

            restTemplate.postForEntity(url, request, String.class);

            log.info("Budget alert sent via Mailgun: to={} category={} threshold={}%",
                    toEmail, categoryName, thresholdPercent);

        } catch (Exception e) {
            log.error("Failed to send Mailgun email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildMessage(
            String name,
            String category,
            BigDecimal budget,
            BigDecimal spent,
            int threshold
    ) {
        return """
                Hi %s,

                Your budget alert has been triggered.

                Category: %s
                Budget: %s
                Spent: %s
                Threshold: %d%%

                FinTrack
                """.formatted(name, category, budget, spent, threshold);
    }
}