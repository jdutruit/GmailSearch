package mailsearch;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import com.google.api.client.extensions.appengine.datastore.AppEngineDataStoreFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;

/** Class that has helper functions **/
public class Helper {
	// Check https://developers.google.com/gmail/api/auth/scopes for all available scopes
	private static final String SCOPE_1 = "https://www.googleapis.com/auth/gmail.readonly";
	private static final String SCOPE_2 = "https://www.googleapis.com/auth/gmail.modify";
	private static final String SCOPE_3 = "https://mail.google.com/";
	private static final String APP_NAME = "Gmail Searcher";
	
	private static final AppEngineDataStoreFactory DATA_STORE_FACTORY = AppEngineDataStoreFactory.getDefaultInstance();
	
	static HttpTransport httpTransport = new NetHttpTransport();
    static JsonFactory jsonFactory = new JacksonFactory();
	
	static String getRedirectUri(HttpServletRequest req) {
		GenericUrl url = new GenericUrl(req.getRequestURL().toString());
		url.setRawPath("/oauth2callback");
		return url.build();
	}
	
	static GoogleAuthorizationCodeFlow newFlow() throws IOException {
				
		/*Builder(HttpTransport transport, JsonFactory jsonFactory, String clientId,
        String clientSecret, Collection<String> scopes)*/
		
		return new GoogleAuthorizationCodeFlow.Builder(httpTransport, jsonFactory,
				"[ your client_id ]",
				"[ your client_secret ]", Arrays.asList(SCOPE_1,SCOPE_2,SCOPE_3)).setDataStoreFactory(
				getDataStoreFactory()).setAccessType("offline").setApprovalPrompt("force").build();
		
	}


	public static AppEngineDataStoreFactory getDataStoreFactory() {
		return DATA_STORE_FACTORY;
	}

	public static String getAppName() {
		return APP_NAME;
	}
	
	// Simply to show the sender properly in the HTML page
	public static String ProcessSender(String s) {
		return s.replace("<", "").replace(">", "");		
	}
}
