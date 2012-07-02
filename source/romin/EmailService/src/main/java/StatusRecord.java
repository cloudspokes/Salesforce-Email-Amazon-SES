package main.java;

public class StatusRecord {
	int statusCode;
	String statusMessage;

	public StatusRecord() {
		super();
	}

	public StatusRecord(int statusCode, String statusMessage) {
		super();
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getStatusMessage() {
		return statusMessage;
	}

	public void setStatusMessage(String statusMessage) {
		this.statusMessage = statusMessage;
	}

}
