package services;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.mail.ByteArrayDataSource;

public class ForceMailHandler {

    private static final String AMS_EMAIL_OBJECT_NAME = "AMS_Email__c";
    private static final String AMS_EMAIL_ATTACHMENT_OBJECT_NAME = "AMS_Email_Attachment__c";
    private static final String ATTACHMENT_FIELD_NAME = "Attachment__c";
    private static final String ATTACHMENT_OBJECT_NAME = "Attachment";

    public void handleForceMail(String mailObjectId) throws Exception {
        ForceRestApi api = new ForceRestApi(AMS_EMAIL_OBJECT_NAME);
        Map<String, Object> mailFields = api.findById(mailObjectId);

        api = new ForceRestApi(AMS_EMAIL_ATTACHMENT_OBJECT_NAME);
        List<String> attachmentObjectIds = api.findAllByField(
            AMS_EMAIL_OBJECT_NAME, mailObjectId, ATTACHMENT_FIELD_NAME);

        MimeMultipart multiPart = new MimeMultipart();

        api = new ForceRestApi(ATTACHMENT_OBJECT_NAME);
        for (String attachmentObjectId : attachmentObjectIds) {
            Map<String, Object> attachmentFields = api.findById(attachmentObjectId);

            String contentType = (String) attachmentFields.get("ContentType");
            String attachmentName = (String) attachmentFields.get("Name");

            InputStream attStream = api.getBodyById(attachmentObjectId);

            BodyPart bodyPart = new MimeBodyPart();
            bodyPart.setFileName(attachmentName);

            DataSource dataSource = new ByteArrayDataSource(attStream, contentType);
            bodyPart.setDataHandler(new DataHandler(dataSource));

            multiPart.addBodyPart(bodyPart);
        }

        String from    = (String) mailFields.get("From__c");
        String replyTo = (String) mailFields.get("Reply_To__c");
        String subject = (String) mailFields.get("Subject__c");
        String body    = (String) mailFields.get("Body__c");

        // following recipient lists are comma delimited value lists
        String toList  = (String) mailFields.get("To__c");
        String ccList  = (String) mailFields.get("CC__c");
        String bccList = (String) mailFields.get("BCC__c");

        List<String> toRecips = buildRecipientList(toList);
        List<String> ccRecips = buildRecipientList(ccList);
        List<String> bccRecips = buildRecipientList(bccList);

        AwsSesMailService mailService = new AwsSesMailService();
        mailService.sendMessage(from, replyTo, subject, body, multiPart,
            toRecips, ccRecips, bccRecips);
    }

    private static List<String> buildRecipientList(String stringList) {
        List<String> recipList = new ArrayList<String>();
        if (stringList != null && !stringList.trim().isEmpty()) {
            String[] recips = stringList.trim().split(",");
            for (String recip : recips) {
                if (!recip.trim().isEmpty())
                    recipList.add(recip.trim());
            }
        }
        return recipList;
    }
}
