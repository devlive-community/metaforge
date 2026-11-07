package org.devlive.metaforge.service.settings;

import javax.mail.MessagingException;

/** Sends a plain text mail through the configured SMTP server. */
public interface MailService
{
    void send(MailSettings settings, String to, String subject, String text)
            throws MessagingException;
}
