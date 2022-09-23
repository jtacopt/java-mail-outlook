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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class OutlookWorker {

    private final String username;
    private final String key;

    public OutlookWorker(final String username, final String key) {
        this.username = username;
        this.key = key;
    }

    public List<Message> findBySubject(final String subjectTextFragment) throws IOException, MessagingException {
        List<Message> messages = new ArrayList<>();
        Store store = null;
        try {
            store = getEmailStore();
            Folder emailFolder = getInboxFolder(store);
            emailFolder.open(Folder.READ_ONLY);
            for (Message msg : emailFolder.getMessages()) {
                if (msg.getSubject().contains(subjectTextFragment)) {
                    messages.add(msg);
                    Multipart multipart =(Multipart) msg.getContent();
                    MimeBodyPart part = (MimeBodyPart)multipart.getBodyPart(1);
                    if(part.getDisposition().equalsIgnoreCase("attachment")){
                        getEmailAttach(part);
                    }
                }
            }
        } finally {
            if (store != null) {
                store.close();
            }
        }
        return messages;
    }

    private void getEmailAttach(MimeBodyPart part) throws MessagingException, IOException {
        String destFilePath = "temp/" + part.getFileName();
        try( FileOutputStream output = new FileOutputStream(destFilePath)) {
            InputStream input = part.getInputStream();

            byte[] buffer = new byte[4096];

            int byteRead;

            while ((byteRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, byteRead);
            }
        }
    }
    public List<Message> findAll() throws IOException, MessagingException {
        List<Message> messages;
        Store store = null;
        try {
            store = getEmailStore();
            Folder emailFolder = getInboxFolder(store);
            emailFolder.open(Folder.READ_ONLY);
            messages = List.of(emailFolder.getMessages());
            emailFolder.close(true);
        } finally {
            if (store != null) {
                store.close();
            }
        }
        return messages;
    }

    private Folder getInboxFolder(Store store) throws MessagingException {
        return store.getFolder("INBOX");
    }

    private Store getEmailStore() throws IOException, MessagingException {
        Properties imapProperties = getIMAPProperties();
        Session emailSession = Session.getDefaultInstance(imapProperties);
        Store store = emailSession.getStore("imap");
        store.connect(this.username, this.key);
        return store;
    }

    public void cleanMailBox() throws IOException, MessagingException {
        Store store = null;
        try {
            store = getEmailStore();
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
        var properties = new Properties();
        var is = getClass().getClassLoader().getResourceAsStream(propertiesFile);
        properties.load(is);
        return properties;
    }

}
