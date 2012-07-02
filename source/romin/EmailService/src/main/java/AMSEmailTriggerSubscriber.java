package main.java;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClient;
import com.amazonaws.services.simpleemail.model.RawMessage;
import com.amazonaws.services.simpleemail.model.SendRawEmailRequest;
import com.amazonaws.services.simpleemail.model.SendRawEmailResult;

public class AMSEmailTriggerSubscriber {
	
	//Amazon Web Services Credentials given by CloudSpokes
	private static final String AWS_AccessKeyID = "YOUR_AWS_KEY";
	private static final String AWS_Secret_Access_Key = "YOUR_AWS_SECRET";

	// Skips certificate validation in the case of logging proxies for debugging
	private static final boolean NO_VALIDATION = false;

	// The long poll duration
	private static final int TIMEOUT = 120 * 1000;

	// Get credentials etc from environment
	private static final String LOGIN_SERVER = "https://login.salesforce.com/";
	private static final String USERNAME = "YOUR_SALESFORCE_EMAIL_ADDRESS";
	private static final String PASSWORD = "SALESFORCEPASSWORD+SECURITYTOKEN";

	private static final String CLIENT_ID = "YOURCLIENTID";
	private static final String CLIENT_SECRET = "YOURCLIENTSECRET";
	
	//This is the Push Topic. Change it if you are using something else
	private static final String TOPIC_NAME = "AllAMSEmailsTriggerTest";

	// The path for the Streaming API endpoint
	private static final String DEFAULT_PUSH_ENDPOINT = "/cometd/23.0";

	// Global variables
	private static String accessToken = "";
	private static String instanceURL = "";
	
	/**
	 * This is the main method that is waiting for any thing to be published on the TOPIC_NAME i.e. AllAMSEmails1
	 * The push topic is created as shown below:
	 *  PushTopic pushTopic = new PushTopic();
	    pushTopic.ApiVersion = 23.0;
		pushTopic.Name = 'AllAMSEmails1';
		pushTopic.Description = 'All records for the AMS_Emails object';
		pushtopic.Query = 'SELECT Id, Name, From__c, Reply_To__c, Subject__c, To__c, CC__c, BCC__c, Body__c FROM AMS_Email__c';
		insert pushTopic;
	 *
	 * Once the message is received, it extracts out the fields above and then proceeds to send off the email.	
	 * @param args
	 * @throws Exception
	 */

	public static void main(String[] args) throws Exception {
		BayeuxClient client = null;

		String topic = TOPIC_NAME;

		System.out.println("Running AMS Email Trigger Subscriber....");
		if (NO_VALIDATION) {
			setNoValidation();
		}

		client = getClient();
		client.handshake();

		System.out.println("Waiting for handshake");
		waitForHandshake(client, 60 * 1000, 1000);

		System.out.println("Subscribing to topic: " + topic);
		client.getChannel("/topic/" + topic).subscribe(new MessageListener() {
			@Override
			public void onMessage(ClientSessionChannel channel,
					org.cometd.bayeux.Message message) {
				try {
					JSONObject responseJSON = new JSONObject(new JSONTokener(message.getJSON()));
					System.out.println("Received Message: "	+ responseJSON.toString(2));
					
					//Check if event type is 'created'. If not, we can safely return.
					JSONObject sEvent = responseJSON.getJSONObject("data").getJSONObject("event");
					String eventType = sEvent.getString("type");
					if (!eventType.equalsIgnoreCase("created")) return;

					//Since Event type is 'created', let us extract out the email fields
					JSONObject sObject = responseJSON.getJSONObject("data").getJSONObject("sobject");
					String AMSEmailRecordID = sObject.getString("AMS_Email__c");
					
					//Extract out the values
					String url = "/services/data/v20.0/query/?q=SELECT+Id,+Name,+From__c,+Reply_To__c,+Subject__c,+To__c,+CC__c,+BCC__c,+Body__c,(select+Id,Name+from+Attachments)+FROM+AMS_Email__c+where+Id='"+ AMSEmailRecordID + "'";
					
						StatusRecord _statusProject = RESTUtils.doGet(instanceURL, url,	accessToken);
						if (_statusProject.getStatusCode() != org.apache.http.HttpStatus.SC_OK) {
							throw new Exception(_statusProject.getStatusMessage());
						} 
						else {
							String resultREST = _statusProject.getStatusMessage();
							JSONObject EmailRecordJSON = new JSONObject(resultREST);
							//System.out.println("Need to process the following Record : " + EmailRecordJSON.toString(2));
							int totalSize = EmailRecordJSON.getInt("totalSize");
							if (totalSize == 0) throw new Exception("No records found");
							JSONObject record = (JSONObject) (EmailRecordJSON.getJSONArray("records").get(0));
							String fromAddress = record.getString("From__c");

							String toAddresses = record.getString("To__c");
							List<String> toAddressesList = Arrays.asList(toAddresses.split(","));

							List<String> bccAddressesList = null;
							String bccAddresses = record.optString("BCC__c");
							if (!bccAddresses.equals("")) bccAddressesList = Arrays.asList(bccAddresses.split(","));

							List<String> ccAddressesList = null;
							String ccAddresses = record.optString("CC__c");
							if (!ccAddresses.equals("")) ccAddressesList = Arrays.asList(ccAddresses.split(","));

							String emailSubject = record.optString("Subject__c","DEFAULT_SUBJECT");

							String emailBody = record.optString("Body__c","DEFAULT_BODY");

							String replyToAddresses = record.optString("Reply_To__c");
							List<String> replyToAddressesList = Arrays.asList(replyToAddresses.split(","));
							
							/**************** ATTACHMENTS ************************
							 * 
							 */
							List<AttachmentRecord> attachmentRecords = new ArrayList<AttachmentRecord>();
							JSONObject attachments = record.optJSONObject("Attachments");
							//System.out.println(attachments.toString(2));
							if (attachments != null) {
								int numAttachments = attachments.getInt("totalSize");
								for (int i = 0; i < numAttachments; i++) {
									JSONObject aObject = (JSONObject) attachments.getJSONArray("records").get(i);
									String aId = aObject.getString("Id");
									String attachmentURL = "/services/data/v20.0/sobjects/Attachment/"+ aId;
									_statusProject = RESTUtils.doGet(instanceURL,attachmentURL, accessToken);
									if (_statusProject.getStatusCode() != org.apache.http.HttpStatus.SC_OK) {
										throw new Exception(_statusProject.getStatusMessage());
									} else {
										resultREST = _statusProject.getStatusMessage();
										JSONObject responseAttachmentJSON = new JSONObject(resultREST);
										String attachmentName = responseAttachmentJSON.getString("Name");
										String attachmentBodyURL = responseAttachmentJSON.getString("Body");
										String attachmentContentType = responseAttachmentJSON.getString("ContentType");
										AttachmentRecord _AR = new AttachmentRecord(attachmentName, attachmentBodyURL, attachmentContentType);
										attachmentRecords.add(_AR);
									}
								}
							}
							
							//DONE WITH ATTACHMENTS

							send_email(AMSEmailRecordID, 
									   fromAddress, 
									   toAddressesList,
									   ccAddressesList, 
									   bccAddressesList,
									   replyToAddressesList, 
									   emailSubject, 
									   emailBody,
									   attachmentRecords);

						}
				} catch (Exception e) {
					e.printStackTrace();
				} 
			}
		});
		System.out.println("Waiting for streamed data from Force.com...");
		while (true) {
			// This infinite loop is for demo only, to receive streamed events
			// on the specified topic from Salesforce.com
			Thread.sleep(TIMEOUT);
		}
	}

	
	/**
	 * This is the send email method that uses Amazon SES to send the email
	 * @param salesForceRecordId The Sales Force Record Id
	 * @param from The from Email Address
	 * @param toAddresses A List of to Email Addresses
	 * @param ccAddresses A List of CC Email Addresses
	 * @param bccAddresses A List of BCc Email Addresses
	 * @param replyToAddresses A List of Reply To Email Addresses
	 * @param subject The subject of the Email
	 * @param emailContents The contents of the Email
	 * @param attachments List of Attachment Records
	 */
	protected static void send_email(String salesForceRecordId, String from,
			List<String> toAddresses, List<String> ccAddresses,
			List<String> bccAddresses, List<String> replyToAddresses,
			String subject, String emailContents, List<AttachmentRecord> attachments) {

		try {
			
			// JavaMail representation of the message
            Session s = Session.getInstance(new Properties(), null);
            MimeMessage msg = new MimeMessage(s);
            
            // From Address
            msg.setFrom(new InternetAddress(from));
            
            // ReplyTo Addresses
            Address[] ReplyToAddresses = new Address[replyToAddresses.size()];
            for (int i = 0; i < replyToAddresses.size(); i++) {
            	ReplyToAddresses[i] = new InternetAddress(replyToAddresses.get(i));
			}
            msg.setReplyTo(ReplyToAddresses);

            // To Addresses
            Address[] ToAddresses = new Address[toAddresses.size()];
            for (int i = 0; i < toAddresses.size(); i++) {
				ToAddresses[i] = new InternetAddress(toAddresses.get(i));
			}
            msg.setRecipients(javax.mail.Message.RecipientType.TO, ToAddresses);

            //BCC Addresses
            if (bccAddresses != null) {
	            Address[] BCCAddresses = new Address[bccAddresses.size()];
	            for (int i = 0; i < bccAddresses.size(); i++) {
					BCCAddresses[i] = new InternetAddress(bccAddresses.get(i));
				}
	            msg.setRecipients(javax.mail.Message.RecipientType.CC, BCCAddresses);
            }
            
            //CC Addresses
            if (ccAddresses != null) {
	            Address[] CCAddresses = new Address[ccAddresses.size()];
	            for (int i = 0; i < ccAddresses.size(); i++) {
	            	CCAddresses[i] = new InternetAddress(ccAddresses.get(i));
				}
	            msg.setRecipients(javax.mail.Message.RecipientType.BCC, CCAddresses);
            }
            
            // Subject
            msg.setSubject(subject);
    		
            // Add a MIME part to the message
            MimeMultipart mp = new MimeMultipart();
	            
            BodyPart part = new MimeBodyPart();
            String myText = emailContents;
            part.setContent(myText, "text/html");
            mp.addBodyPart(part);
            
            //Attachment part
            
            for (AttachmentRecord attachmentRecord : attachments) {
				StatusFileRecord _attachmentResult = RESTUtils.doFileGet(instanceURL, attachmentRecord.getAttachmentBodyURL(), accessToken);
				if (_attachmentResult.getStatusCode() != org.apache.http.HttpStatus.SC_OK) {
					System.out.println("Could not process attachment : " + attachmentRecord.getAttachmentName());
				} else {
					  part = new MimeBodyPart();
				      DataSource source = new ByteArrayDataSource(_attachmentResult.getContents(),attachmentRecord.getAttachmentContentType());
		              part.setDataHandler(new DataHandler(source));
		              part.setHeader("Content-Type", attachmentRecord.getAttachmentContentType()); 
		              part.setHeader("Content-Disposition","attachment");
		              part.setFileName(attachmentRecord.getAttachmentName());
		              mp.addBodyPart(part);				
		        }
			}
            
            msg.setContent(mp);
    
         // Capture the raw message
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            msg.writeTo(out);

            RawMessage rm = new RawMessage();
            rm.setData(ByteBuffer.wrap(out.toString().getBytes()));

            // Set AWS access credentials
			AmazonSimpleEmailServiceClient client = new AmazonSimpleEmailServiceClient(new BasicAWSCredentials(AWS_AccessKeyID, AWS_Secret_Access_Key));

            // Call Amazon SES to send the message 
            try {
                SendRawEmailResult SRER = client.sendRawEmail(new SendRawEmailRequest().withRawMessage(rm));
                System.out.println("Delivered to Amazon SES via Message ID : " + SRER.getMessageId());
            } catch (Exception e) {
                e.printStackTrace();
            }
		} catch (AmazonClientException e) {
			System.out.println(e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	

	private static BayeuxClient getClient() throws Exception {
		// Authenticate via OAuth
		JSONObject response = oauthLogin();
		System.out.println("Login response: " + response.toString(2));
		if (!response.has("access_token")) {
			throw new Exception("OAuth failed: " + response.toString());
		}

		accessToken = response.getString("access_token");
		instanceURL = response.getString("instance_url");
		// Get what we need from the OAuth response
		final String sid = response.getString("access_token");
		String instance_url = response.getString("instance_url");

		// Set up a Jetty HTTP client to use with CometD
		HttpClient httpClient = new HttpClient();
		httpClient.setConnectTimeout(TIMEOUT);
		httpClient.setTimeout(TIMEOUT);
		httpClient.start();

		Map<String, Object> options = new HashMap<String, Object>();
		options.put(ClientTransport.TIMEOUT_OPTION, TIMEOUT);

		// Adds the OAuth header in LongPollingTransport
		LongPollingTransport transport = new LongPollingTransport(options,
				httpClient) {
			@Override
			protected void customize(ContentExchange exchange) {
				super.customize(exchange);
				exchange.addRequestHeader("Authorization", "OAuth " + sid);
			}
		};

		// Now set up the Bayeux client itself
		BayeuxClient client = new BayeuxClient(instance_url
				+ DEFAULT_PUSH_ENDPOINT, transport);

		return client;
	}

	private static void waitForHandshake(BayeuxClient client,
			long timeoutInMilliseconds, long intervalInMilliseconds) {
		long start = System.currentTimeMillis();
		long end = start + timeoutInMilliseconds;
		while (System.currentTimeMillis() < end) {
			if (client.isHandshook())
				return;
			try {
				Thread.sleep(intervalInMilliseconds);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		throw new IllegalStateException("Client did not handshake with server");
	}

	public static void setNoValidation() throws Exception {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
			}

			@Override
			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		// Install the all-trusting host verifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	}

	private static JSONObject oauthLogin() throws Exception {
		HttpClient httpClient = new HttpClient();
		httpClient.start();

		String url = LOGIN_SERVER + "/services/oauth2/token";

		ContentExchange exchange = new ContentExchange();
		exchange.setMethod("POST");
		exchange.setURL(url);

		String message = "grant_type=password&client_id=" + CLIENT_ID
				+ "&client_secret=" + CLIENT_SECRET + "&username=" + USERNAME
				+ "&password=" + PASSWORD;

		exchange.setRequestHeader("Content-Type",
				"application/x-www-form-urlencoded");
		exchange.setRequestContentSource(new ByteArrayInputStream(message
				.getBytes("UTF-8")));

		httpClient.send(exchange);
		exchange.waitForDone();

		return new JSONObject(new JSONTokener(exchange.getResponseContent()));

	}
}
