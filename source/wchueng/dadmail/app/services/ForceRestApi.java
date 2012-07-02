package services;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import models.Authentication;

import org.codehaus.jackson.map.ObjectMapper;

import play.Logger;
import play.cache.Cache;

import com.force.api.ApiException;
import com.force.api.ApiTokenException;
import com.force.api.http.Http;
import com.force.api.http.HttpRequest;
import com.force.api.http.HttpRequest.ResponseFormat;
import com.force.api.http.HttpResponse;

public class ForceRestApi {

    // make sure you set your "Session Timeout" in Salesforce higher than this:
    private static final int TOKEN_EXPIRY_IN_HOURS = 11;

    // key for the entry in the Play cache where the Force Authentication object is cached
    // (to avoid authenticating on each request, until the token expiry time set above)
    private static final String FORCE_AUTH_CACHE_KEY = "force_auth";

    private static final String SERVICES_DATA_PATH = "services/data/v20.0";

    private final String objectName;

    public ForceRestApi(String objectName) {
        this.objectName = objectName;
    }

    /**
     * Find the object with the specified id.
     *
     * @return a Map representing the fields of the object found.
     */
    public Map<String, Object> findById(String id) {
        Authentication forceAuth = getForceAuth();

        String url = baseObjectUrl(forceAuth.apiEndpoint) + "/" + id;

        HttpResponse res = get(url, forceAuth.accessToken, 200);
        String json = res.getString();

        Map<String, Object> fields = deserializeJson(json);
        Logger.info("Lookup object fields: %s", fields);
        return fields;
    }

    public InputStream getBodyById(String id) {
        Authentication forceAuth = getForceAuth();

        String url = baseObjectUrl(forceAuth.apiEndpoint) + "/" + id + "/body";

        HttpResponse res = get(url, forceAuth.accessToken, 200, ResponseFormat.STREAM);
        InputStream stream = res.getStream();
        return new BufferedInputStream(stream);
    }

    /**
     * Find the objects with the specified value for the specified field.
     *
     * @return a List of desired result field values of the objects found.
     */
    public List<String> findAllByField(String fieldName, String fieldValue,
            String resultFieldName) {
        List<String> resultFieldValues = new ArrayList<String>();
        String query = "SELECT " + resultFieldName + " FROM " + objectName
            + " WHERE " + fieldName + " = '" + fieldValue + "'";

        Authentication forceAuth = getForceAuth();
        String url = baseQueryUrl(forceAuth.apiEndpoint) + "/?q=" + urlEncode(query);

        HttpResponse res = get(url, forceAuth.accessToken, 200);
        String json = res.getString();

        Map<String, Object> results = deserializeJson(json);
        List<Map<String, Object>> records = (List<Map<String, Object>>) results.get("records");
        for (Map<String, Object> record : records) {
            String resultFieldValue = (String) record.get(resultFieldName);
            resultFieldValues.add(resultFieldValue);
        }

        return resultFieldValues;
    }

    private String baseObjectUrl(String apiEndpoint) {
        return apiEndpoint + "/" + SERVICES_DATA_PATH + "/sobjects/" + objectName;
    }

    private String baseQueryUrl(String apiEndpoint) {
        return apiEndpoint + "/" + SERVICES_DATA_PATH + "/query";
    }

    private Map<String, Object> deserializeJson(String json) {
        try {
            return new ObjectMapper().readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HttpResponse get(String url, String accessToken, int expectedStatusCode) {
        return get(url, accessToken, expectedStatusCode, ResponseFormat.STRING);
    }

    private HttpResponse get(String url, String accessToken, int expectedStatusCode, ResponseFormat format) {
        HttpResponse res = apiRequest(
            accessToken,
            new HttpRequest()
                .method("GET")
                .url(url)
                .header("Accept", "application/json")
                .responseFormat(format)
                .expectsCode(expectedStatusCode)
        );
        return res;
    }

    private synchronized Authentication getForceAuth() {
        Authentication forceAuth = (Authentication) Cache.get(FORCE_AUTH_CACHE_KEY);
        if (forceAuth == null) {
            forceAuth = new ForceAuthenticator().authenticate();
            Cache.add(FORCE_AUTH_CACHE_KEY, forceAuth, TOKEN_EXPIRY_IN_HOURS + "h");
        }
        return forceAuth;
    }

    private HttpResponse apiRequest(String accessToken, HttpRequest req) {
        req.setAuthorization("OAuth " + accessToken);
        HttpResponse res = Http.send(req);

        if (res.getResponseCode() > 299) {
            if (res.getResponseCode() == 401)
                throw new ApiTokenException(res.getString());

            throw new ApiException(res.getResponseCode(), res.getString());
        }

        if (req.getExpectedCode() != -1 && res.getResponseCode() != req.getExpectedCode())
            throw new RuntimeException("Unexpected response from Force API. Got response code " + res.getResponseCode()
                    + ". Was expecting " + req.getExpectedCode());

        return res;
    }

    private static String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, Charset.defaultCharset().name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
