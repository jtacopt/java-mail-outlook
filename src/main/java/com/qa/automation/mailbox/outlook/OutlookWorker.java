package com.qa.automation.mailbox.outlook;

import javax.mail.Authenticator;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OutlookWorker {


    private final String username;
    private final String key;

    public OutlookWorker(final String username, final String key) {
        this.username = username;
        this.key = key;
    }

    public static void main(String[] args) throws MessagingException, IOException {
        new OutlookWorker("mb.testing1@outlook.com", "$Teste123!").retrieveMailBoxMessages();
    }

    public void retrieveMailBoxMessages() throws IOException, MessagingException {
        Properties prop = getIMAPProperties();

        Session emailSession = Session.getDefaultInstance(prop);

        Store store = null;
        try {
            store = emailSession.getStore("imap");
            store.connect(this.username, this.key);
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            Message[] messages = emailFolder.getMessages();
            for (Message message : messages) {
                //Mail - Subject
                message.getSubject();
                //Mail Body
                message.getContent().toString();
            }
        } finally {
            if (store != null) {
                store.close();
            }
        }

    }

    public void cleanMailBox() throws IOException, MessagingException {
        Properties prop = getIMAPProperties();

        Session emailSession = Session.getDefaultInstance(prop);

        Store store = null;
        try {
            store = emailSession.getStore("imap");
            store.connect(this.username, this.key);
            Folder emailFolder = store.getFolder("INBOX");
            emailFolder.open(Folder.READ_ONLY);

            Message[] messages = emailFolder.getMessages();
            for (Message message : messages) {
                message.setFlag(Flags.Flag.DELETED, true);
            }
        } finally {
            if (store != null) {
                store.close();
            }
        }

    }

    public void send(final String to, final String subject, final String emailBody) throws MessagingException, IOException {
        Properties properties = getSMTPProperties();

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, key);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        message.setRecipients(
                Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);

        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(emailBody, "text/html; charset=utf-8");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        message.setContent(multipart);

        Transport.send(message);
    }

    private Properties getSMTPProperties() throws IOException {
        return getProperties("smtp.properties");
    }

    private Properties getIMAPProperties() throws IOException {
        return getProperties("imap.properties");
    }

    private Properties getProperties(final String propertiesFile) throws IOException {
        Properties properties = new Properties();
        InputStream is = getClass().getClassLoader().getResourceAsStream(propertiesFile);
        properties.load(is);
        return properties;
    }

}
