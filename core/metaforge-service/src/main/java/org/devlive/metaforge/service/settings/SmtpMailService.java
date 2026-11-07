package org.devlive.metaforge.service.settings;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class SmtpMailService
        implements MailService
{
    private static final String TIMEOUT = "10000";

    @Override
    public void send(MailSettings settings, String to, String subject, String text)
            throws MessagingException
    {
        Properties properties = new Properties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.host", settings.getHost());
        properties.put("mail.smtp.port", String.valueOf(settings.getPort()));
        properties.put("mail.smtp.connectiontimeout", TIMEOUT);
        properties.put("mail.smtp.timeout", TIMEOUT);
        properties.put("mail.smtp.writetimeout", TIMEOUT);
        if (settings.isSsl()) {
            properties.put("mail.smtp.ssl.enable", "true");
        }
        else {
            properties.put("mail.smtp.starttls.enable", "true");
        }
        boolean auth = StringUtils.isNotEmpty(settings.getUsername());
        properties.put("mail.smtp.auth", String.valueOf(auth));
        Session session = Session.getInstance(properties, auth ? new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(settings.getUsername(), settings.getPassword());
            }
        } : null);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(settings.getFrom()));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(text, StandardCharsets.UTF_8.name());
        Transport.send(message);
    }
}
