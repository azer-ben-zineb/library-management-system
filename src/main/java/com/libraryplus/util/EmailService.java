package com.libraryplus.util;

import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {
    private final Session session;
    private final String from;

    public EmailService() {
        String host = System.getenv().getOrDefault("SMTP_HOST", "smtp.example.com");
        String port = System.getenv().getOrDefault("SMTP_PORT", "587");
        String user = System.getenv().getOrDefault("SMTP_USER", "user");
        String pass = System.getenv().getOrDefault("SMTP_PASS", "pass");
        this.from = System.getenv().getOrDefault("SMTP_FROM", "noreply@example.com");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        this.session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
    }

    public void sendSimpleEmail(String to, String subject, String body) throws MessagingException {
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
}

