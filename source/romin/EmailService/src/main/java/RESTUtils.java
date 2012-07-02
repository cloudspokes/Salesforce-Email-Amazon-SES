package main.java;


import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class RESTUtils {
	public static StatusRecord doPost(String hostname, String url, String accessToken, String jsonRequest) throws Exception {
		String restURL = hostname + url;
		HttpPost post = new HttpPost(restURL);
		post.setHeader(HttpHeaders.AUTHORIZATION, "OAuth " + accessToken);
		post.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

		HttpEntity entity = new StringEntity(jsonRequest, HTTP.UTF_8);
		post.setEntity(entity);
		HttpClient client = new DefaultHttpClient();
		HttpResponse RESTResponse = client.execute(post);
		int statusCode = RESTResponse.getStatusLine().getStatusCode();
		String statusMessage = EntityUtils.toString(RESTResponse.getEntity());
		return new StatusRecord(statusCode, statusMessage);
	}
	
	public static StatusRecord doGet(String hostname, String url, String accessToken) throws Exception {
		String restURL = hostname + url;
		HttpGet get = new HttpGet(restURL);
		get.setHeader(HttpHeaders.AUTHORIZATION, "OAuth " + accessToken);

		HttpClient client = new DefaultHttpClient();
		HttpResponse RESTResponse = client.execute(get);
		int statusCode = RESTResponse.getStatusLine().getStatusCode();
		String statusMessage = EntityUtils.toString(RESTResponse.getEntity());
		return new StatusRecord(statusCode, statusMessage);
	}

	public static StatusFileRecord doFileGet(String hostname, String url, String accessToken) throws Exception {
		String restURL = hostname + url;
		HttpGet get = new HttpGet(restURL);
		get.setHeader(HttpHeaders.AUTHORIZATION, "OAuth " + accessToken);

		HttpClient client = new DefaultHttpClient();
		HttpResponse RESTResponse = client.execute(get);
		int statusCode = RESTResponse.getStatusLine().getStatusCode();
		if (statusCode != org.apache.http.HttpStatus.SC_OK) {
			String statusMessage = EntityUtils.toString(RESTResponse.getEntity());
			return new StatusFileRecord(statusCode, statusMessage,null);
		}
		else {
			byte[] contents = EntityUtils.toByteArray(RESTResponse.getEntity());
			return new StatusFileRecord(statusCode, "",contents);
		}
	}

	public static StatusRecord doDelete(String hostname, String url, String accessToken) throws Exception {
		String restURL = hostname + url;
		HttpDelete delete = new HttpDelete(restURL);
		delete.setHeader(HttpHeaders.AUTHORIZATION, "OAuth " + accessToken);

		HttpClient client = new DefaultHttpClient();
		HttpResponse RESTResponse = client.execute(delete);
		int statusCode = RESTResponse.getStatusLine().getStatusCode();
		return new StatusRecord(statusCode, "");
	}
}
