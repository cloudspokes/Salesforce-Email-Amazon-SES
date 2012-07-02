package main.java;

public class AttachmentRecord {
	String attachmentName;
	String attachmentBodyURL;
	String attachmentContentType;

	public AttachmentRecord() {
		super();
	}

	public AttachmentRecord(String attachmentName, String attachmentBodyURL,String attachmentContentType) {
		super();
		this.attachmentName = attachmentName;
		this.attachmentBodyURL = attachmentBodyURL;
		this.attachmentContentType = attachmentContentType;
	}

	public String getAttachmentName() {
		return attachmentName;
	}

	public void setAttachmentName(String attachmentName) {
		this.attachmentName = attachmentName;
	}

	public String getAttachmentBodyURL() {
		return attachmentBodyURL;
	}

	public void setAttachmentBodyURL(String attachmentBodyURL) {
		this.attachmentBodyURL = attachmentBodyURL;
	}

	public String getAttachmentContentType() {
		return attachmentContentType;
	}

	public void setAttachmentContentType(String attachmentContentType) {
		this.attachmentContentType = attachmentContentType;
	}
	
}
