package com.petsapp.auth;

import java.security.SecureRandom;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Wysylka emaili transakcyjnych.
 *
 * <p>
 * Metody sa @Async — nie blokuja watku obslugi requesta. W srodowisku dev
 * wiadomosci
 * trafiaja do MailHog (localhost:8025) skonfigurowanego w application-dev.yml.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int VERIFICATION_CODE_DIGITS = 6;

    private final org.springframework.mail.javamail.JavaMailSender mailSender;
    private final String frontendUrl;

    public EmailService(
            org.springframework.mail.javamail.JavaMailSender mailSender,
            @org.springframework.beans.factory.annotation.Value("${app.frontend-url:http://localhost:3000}") String frontendUrl) {
        this.mailSender = mailSender;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Wysyla email z kodem weryfikacyjnym.
     *
     * @param toEmail  adres odbiorcy
     * @param code     6-cyfrowy kod
     * @param username nazwa uzytkownika (do personalizacji)
     */
    @Async
    public void sendVerificationCode(String toEmail, String code, String username) {
        String subject = "PetsApp — kod weryfikacyjny: " + code;
        String body = String.format(
                "Czesc %s,%n%nTwoj kod weryfikacyjny to: %s%n%nKod jest wazny przez 15 minut.%n%nZespol PetsApp",
                username, code);
        sendEmail(toEmail, subject, body);
    }

    /**
     * Generuje kryptograficznie losowy 6-cyfrowy kod weryfikacyjny.
     *
     * <p>
     * Uzywamy SecureRandom zamiast Math.random() — kody sluza do weryfikacji
     * tozsamosci.
     *
     * @return 6-znakowy String cyfr z leading zeros jesli potrzeba
     */
    public String generateVerificationCode() {
        int code = RANDOM.nextInt((int) Math.pow(10, VERIFICATION_CODE_DIGITS));
        return String.format("%0" + VERIFICATION_CODE_DIGITS + "d", code);
    }

    /**
     * Zwraca czas wygasniecia kodu weryfikacyjnego (15 minut od teraz).
     *
     * @return Instant wygasniecia
     */
    public Instant verificationCodeExpiry() {
        return Instant.now().plusSeconds(15 * 60);
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
            message.setFrom("noreply@petsapp.dev");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.debug("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
