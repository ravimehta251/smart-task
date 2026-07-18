package com.smarttask.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Async email service — fire-and-forget so it never blocks the request thread.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${application.frontend.url}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Async
    public void sendVerificationEmail(String toEmail, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;
        sendEmail(toEmail,
                "Verify your SmartTask account",
                "Hello,\n\nPlease verify your email by clicking the link below:\n\n"
                + link
                + "\n\nThis link expires in 24 hours.\n\nSmartTask Team");
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        sendEmail(toEmail,
                "Reset your SmartTask password",
                "Hello,\n\nClick the link below to reset your password:\n\n"
                + link
                + "\n\nThis link expires in 1 hour.\n\nIf you did not request this, please ignore this email.\n\nSmartTask Team");
    }

    @Async
    public void sendTaskAssignedEmail(String toEmail, String assigneeName,
                                       String taskTitle, String projectName) {
        sendEmail(toEmail,
                "New task assigned: " + taskTitle,
                "Hello " + assigneeName + ",\n\nA new task has been assigned to you:\n\n"
                + "Task: " + taskTitle + "\nProject: " + projectName
                + "\n\nLog in to SmartTask to view the details.\n\nSmartTask Team");
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
