package main.java;

public class StatusFileRecord {
	int statusCode;
	String statusMessage;
	byte[] contents;

	public StatusFileRecord() {
		super();
	}

	public StatusFileRecord(int statusCode, String statusMessage, byte[] contents) {
		super();
		this.statusCode = statusCode;
		this.statusMessage = statusMessage;
		this.contents = contents;
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

	public byte[] getContents() {
		return contents;
	}

	public void setContents(byte[] contents) {
		this.contents = contents;
	}
	
}
