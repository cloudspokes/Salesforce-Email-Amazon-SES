package config;

public class Config {

    public static final String FORCE_LOGIN_SERVER = System.getenv("FORCE_LOGIN_SERVER");

    public static final String FORCE_USERNAME = System.getenv("FORCE_USERNAME");
    public static final String FORCE_PASSWORD = System.getenv("FORCE_PASSWORD");
    public static final String FORCE_SECURITY_TOKEN = System.getenv("FORCE_SECURITY_TOKEN");

    public static final String FORCE_CLIENT_ID = System.getenv("FORCE_CLIENT_ID");
    public static final String FORCE_CLIENT_SECRET = System.getenv("FORCE_CLIENT_SECRET");

    public static final String AWS_ACCESS_KEY = System.getenv("AWS_ACCESS_KEY");
    public static final String AWS_SECRET_KEY = System.getenv("AWS_SECRET_KEY");
    public static final String AWS_SES_VERIFIED_SENDER = System.getenv("AWS_SES_VERIFIED_SENDER");

    private Config() {
    }
}
