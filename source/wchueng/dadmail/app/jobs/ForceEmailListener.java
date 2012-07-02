package jobs;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.cometd.bayeux.Message;
import org.cometd.bayeux.client.ClientSessionChannel;
import org.cometd.bayeux.client.ClientSessionChannel.MessageListener;
import org.cometd.client.BayeuxClient;
import org.cometd.client.transport.ClientTransport;
import org.cometd.client.transport.LongPollingTransport;
import org.eclipse.jetty.client.ContentExchange;
import org.eclipse.jetty.client.HttpClient;
import org.json.JSONObject;
import org.json.JSONTokener;

import play.Logger;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import services.ForceMailHandler;
import config.Config;

@OnApplicationStart
public class ForceEmailListener extends Job {

    private static final String PUSH_TOPIC_NAME = "AllAMSEmails";

    // The path for the Streaming API endpoint
    private static final String DEFAULT_PUSH_ENDPOINT = "/cometd/23.0";

    // The long poll duration
    private static final int TIMEOUT = 120 * 1000; // 2 minutes

    private static final int WAIT_INTERVAL = 1000;

    private static final int JSON_INDENT_LEVEL = 2;

    @Override
    public void doJob() throws Exception {
        BayeuxClient client = getClient();
        client.handshake();
        waitForHandshake(client, TIMEOUT / 2, WAIT_INTERVAL);

        subcribeToTopic(client);
    }

    private void subcribeToTopic(BayeuxClient client) {
        Logger.info("Subscribing to topic: %s", PUSH_TOPIC_NAME);
        client.getChannel("/topic/" + PUSH_TOPIC_NAME).subscribe(new MessageListener() {
            @Override
            public void onMessage(ClientSessionChannel channel, Message message) {
                try {
                    Logger.info("Received Message: %s",
                        (new JSONObject(new JSONTokener(message.getJSON()))).toString(JSON_INDENT_LEVEL));

                    String forceMailId = (String) ((Map)((Map)message.get("data")).get("sobject")).get("Id");

                    new ForceMailHandler().handleForceMail(forceMailId);

                } catch (Exception e) {
                    Logger.error(e, e.getLocalizedMessage());
                }
            }
        });
        Logger.info("Waiting for streamed data from Force.com...");
    }

    private BayeuxClient getClient() throws Exception {
        // Authenticate via OAuth
        JSONObject response = oauthLogin();
        Logger.info("Login response: %s", response.toString(JSON_INDENT_LEVEL));
        if (!response.has("access_token"))
            throw new RuntimeException("OAuth failed: " + response.toString());

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

        // Add the OAuth header in LongPollingTransport
        LongPollingTransport transport = new LongPollingTransport(options, httpClient) {
            @Override
            protected void customize(ContentExchange exchange) {
                super.customize(exchange);
                exchange.addRequestHeader("Authorization", "OAuth " + sid);
            }
        };

        // Now set up the Bayeux client itself
        return new BayeuxClient(instance_url + DEFAULT_PUSH_ENDPOINT, transport);
    }

    private void waitForHandshake(BayeuxClient client,
            long timeoutInMilliseconds, long intervalInMilliseconds) {
        Logger.info("Waiting for handshake");
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

    private JSONObject oauthLogin() throws Exception {
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        String url = Config.FORCE_LOGIN_SERVER + "/services/oauth2/token";

        ContentExchange exchange = new ContentExchange();
        exchange.setMethod("POST");
        exchange.setURL(url);

        String message = "grant_type=password"
            + "&client_id=" + Config.FORCE_CLIENT_ID
            + "&client_secret=" + Config.FORCE_CLIENT_SECRET
            + "&username=" + Config.FORCE_USERNAME
            + "&password=" + Config.FORCE_PASSWORD + Config.FORCE_SECURITY_TOKEN;
        exchange.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
        exchange.setRequestContentSource(new ByteArrayInputStream(message.getBytes("UTF-8")));

        httpClient.send(exchange);
        exchange.waitForDone();

        return new JSONObject(new JSONTokener(exchange.getResponseContent()));
    }
}
