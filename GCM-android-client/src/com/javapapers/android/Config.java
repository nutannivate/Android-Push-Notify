package com.javapapers.android;

public interface Config {

	// used to share GCM regId with application server - using php app server
	//replace 'localhost' with your public IP address
	static final String APP_SERVER_URL = "http://localhost/gcm_server_files/gcm.php?shareRegId=1";
 
	// GCM server using java
	// static final String APP_SERVER_URL =
	// "http://192.168.1.17:8080/GCM-App-Server/GCMNotification?shareRegId=1";

	// replace 'YOUR-PROJECT-ID' with your Google Project Number
	static final String GOOGLE_PROJECT_ID = "YOUR-PROJECT-ID";
	static final String MESSAGE_KEY = "message";

}
