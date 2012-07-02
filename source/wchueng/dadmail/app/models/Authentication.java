package models;

import java.io.Serializable;

public class Authentication implements Serializable {

    public String accessToken;
    public String apiEndpoint;

    public Authentication(String accessToken, String apiEndpoint) {
        this.accessToken = accessToken;
        this.apiEndpoint = apiEndpoint;
    }
}
