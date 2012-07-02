package services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import play.Logger;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;

import config.Config;

public class AwsSesMailService {

    public void sendMessage(String from, String replyTo,
            String subject, String body, MimeMultipart multiPart,
            List<String> toRecips, List<String> ccRecips, List<String> bccRecips)

        throws AddressException, IOException, MessagingException  {

        Session mailSession = Session.getInstance(new Properties(), null);
        MimeMessage msg = new MimeMessage(mailSession);

        // ignore from & replyTo inputs since can't guarantee verified
        msg.setFrom(new InternetAddress(Config.AWS_SES_VERIFIED_SENDER));
        msg.setReplyTo(new Address[] {new InternetAddress(Config.AWS_SES_VERIFIED_SENDER)});

        msg.setSubject(subject);

        addTextBodyPart(body, multiPart);

        if (multiPart.getCount() == 0) { // multiPart can't be empty
            // so if no attachments, use subject as body
            addTextBodyPart(subject, multiPart);
        }
        msg.setContent(multiPart);

        setRecipients(msg, Message.RecipientType.TO, toRecips);
        setRecipients(msg, Message.RecipientType.CC, ccRecips);
        setRecipients(msg, Message.RecipientType.BCC, bccRecips);

        OutputStream outStream = new ByteArrayOutputStream();
        msg.writeTo(outStream);

        RawMessage rawMsg = new RawMessage();
        rawMsg.setData(ByteBuffer.wrap(outStream.toString().getBytes()));

        AmazonSimpleEmailServiceClient client = new AmazonSimpleEmailServiceClient(
            new BasicAWSCredentials(Config.AWS_ACCESS_KEY, Config.AWS_SECRET_KEY));

        SendRawEmailResult response = client.sendRawEmail(
            new SendRawEmailRequest().withRawMessage(rawMsg));
        Logger.info("Sent message: %s", response);
    }

    private void addTextBodyPart(String text, MimeMultipart multiPart) throws MessagingException {
        if (text != null && !text.trim().isEmpty()) {
            BodyPart bodyPart = new MimeBodyPart();
            bodyPart.setContent(text, "text/plain");
            multiPart.addBodyPart(bodyPart);
        }
    }

    private void setRecipients(MimeMessage msg, RecipientType recipientType, List<String> recipients)
            throws AddressException, MessagingException {
        List<Address> addresses = new ArrayList<Address>(recipients.size());
        for (String recipient : recipients) {
            addresses.add(new InternetAddress(recipient));
        }
        if (!addresses.isEmpty()) {
            msg.setRecipients(recipientType, addresses.toArray(new Address[addresses.size()]));
        }
    }
}
