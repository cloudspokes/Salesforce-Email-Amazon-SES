package services;

import models.Authentication;

import com.force.api.ApiConfig;
import com.force.api.ApiSession;
import com.force.api.Auth;

import config.Config;

public class ForceAuthenticator {

    private static ApiConfig API_CONFIG = new ApiConfig()
      .setUsername(Config.FORCE_USERNAME)
      .setPassword(Config.FORCE_PASSWORD + Config.FORCE_SECURITY_TOKEN);

    public Authentication authenticate() {
        // without clientId and clientSecret, this follows Soap login/password flow
        ApiSession session = Auth.authenticate(API_CONFIG);

        // getApiEndpoint() returns endpoint for web service call, just extract instance id
        int i = session.getApiEndpoint().indexOf("na");
        int j = session.getApiEndpoint().indexOf("-api");
        String instance = session.getApiEndpoint().substring(i, j);
        // SF serverUrl for APEX REST call
        String serverUrl = "https://" + instance + ".salesforce.com";

        return new Authentication(session.getAccessToken(), serverUrl);
    }
}
