package main.java;

import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import com.amazonaws.services.simpleemail.AWSJavaMailTransport;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;

public class MailSender
{
/*      private Transport AWSTransport;
      //Initialize transport
      private void initAWSTransport() throws MessagingException
      {
        String keyID = "";
        String secretKey = "";
        MailAWSCredentials credentials = new MailAWSCredentials();
        credentials.setCredentials(keyID, secretKey);
        AmazonSimpleEmailService ses = new AmazonSimpleEmailServiceClient(credentials);
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "aws");
        props.setProperty("mail.aws.user", credentials.getAWSAccessKeyId());
        props.setProperty("mail.aws.password", credentials.getAWSSecretKey());
        AWSsession = Session.getInstance(props);
        AWStransport = new AWSJavaMailTransport(AWSsession, null);
        AWStransport.connect();
      }

  public void sendEmail(byte[] attachment)
  {
    //mail properties
    String senderAddress = <Sender address>;
    String recipientAddress = <Recipient address>;
    String subject = <Mail subject>;
    String text = <Your text>;
    String mimeTypeOfText = <MIME type of text part>;
    String fileMimeType = <MIME type of your attachment>;
    String fileName = <Name of attached file>;

    initAWSTransport();

    try
    {
      // Create new message
      Message msg = new MimeMessage(AWSsession);
      msg.setFrom(new InternetAddress(senderAddress));
      msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientAddress));
      msg.setSubject(subject);

      //Text part
      Multipart multipart = new MimeMultipart();
      BodyPart messageBodyPart = new MimeBodyPart();
      messageBodyPart.setContent(text, mimeTypeOfText);
      multipart.addBodyPart(messageBodyPart);

      //Attachment part
      if (attachment != null && attachment.length != 0)
      {
        messageBodyPart = new MimeBodyPart();
        DataSource source = new ByteArrayDataSource(attachment,fileMimeType);
        messageBodyPart.setDataHandler(new DataHandler(source));
        messageBodyPart.setFileName(fileName);
        multipart.addBodyPart(messageBodyPart);
      }
      msg.setContent(multipart);

      //send message
      msg.saveChanges();
      AWSTransport.sendMessage(msg, null);
    } catch (MessagingException e){System.out.println(e.getMessage());}
  }*/
}
