package cinemasystem.util;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class EmailService {

    private static final String SENDER_EMAIL = System.getenv("CINEMA_SENDER_EMAIL");
    private static final String APP_PASSWORD = System.getenv("CINEMA_EMAIL_APP_PASSWORD");

    public static void sendEmail(String recipient, String subject, String body) throws Exception {
        if (SENDER_EMAIL == null || SENDER_EMAIL.isBlank()
                || APP_PASSWORD == null || APP_PASSWORD.isBlank()) {
            throw new IllegalStateException(
                    "Email is not configured. Set CINEMA_SENDER_EMAIL and CINEMA_EMAIL_APP_PASSWORD."
            );
        }

        Properties properties = new Properties();
        properties.put("mail.smtp.host", "smtp.gmail.com");
        properties.put("mail.smtp.port", "587");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(SENDER_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
}
