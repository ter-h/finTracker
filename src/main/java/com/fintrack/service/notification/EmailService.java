package com.fintrack.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender  mailSender;
    private final TemplateEngine  templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    // @Async means this runs in a background thread
    // The HTTP request returns immediately without waiting for the email
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
            Context ctx = new Context(Locale.ENGLISH);
            ctx.setVariable("displayName",      displayName);
            ctx.setVariable("categoryName",     categoryName);
            ctx.setVariable("budgetAmount",     budgetAmount);
            ctx.setVariable("spentAmount",      spentAmount);
            ctx.setVariable("thresholdPercent", thresholdPercent);
            ctx.setVariable("isOverBudget",     thresholdPercent >= 100);

            String subject = thresholdPercent >= 100
                ? "⚠️ Budget exceeded: " + categoryName
                : "📊 80% of budget used: " + categoryName;

            String html = templateEngine.process("email/budget-alert", ctx);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);   // true = HTML email

            mailSender.send(message);
            log.info("Budget alert sent: to={} category={} threshold={}%",
                toEmail, categoryName, thresholdPercent);

        } catch (Exception e) {
            // Don't crash the app if email fails — just log it
            log.error("Failed to send budget alert email to {}: {}", toEmail, e.getMessage());
        }
    }
}