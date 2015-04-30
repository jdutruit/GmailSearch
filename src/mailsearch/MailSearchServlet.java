package mailsearch;

import java.io.IOException;

import javax.servlet.http.*;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.users.User;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePartHeader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/** Servlet that looks through a user's Gmail and finds the messages that match a query given as a parameter.
 * URL parameter "s" = the query. 
 */
@SuppressWarnings("serial")
public class MailSearchServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		String thisURL = req.getRequestURI();
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		String query = req.getParameter("s");
		if (req.getUserPrincipal() != null) {
			// load Credential (user should already be known)
			String userid = user.getUserId();
			Credential credential = Helper.newFlow().loadCredential(userid);
			//create service
			HttpTransport httpTransport = new NetHttpTransport();
		    JsonFactory jsonFactory = new JacksonFactory();
			Gmail service = new Gmail.Builder(httpTransport, jsonFactory, credential)
				.setApplicationName(Helper.getAppName()).build();
			// list messages
			List<Message> messages = listMessagesMatchingQuery(service, "me", query);
			
			// load the datastore
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			
			// find the user in the datastore
			Filter userIdFilter =
					  new FilterPredicate("userid",
					                      FilterOperator.EQUAL,
					                      userid);
			Query q = new Query("Customer").setFilter(userIdFilter);
			PreparedQuery pq = datastore.prepare(q);
			List<Entity> results = pq.asList(FetchOptions.Builder.withLimit(3));
			// if there is only one user with this id (...there should only be one)
			if (results.size() == 1) {
				// create the search entity with the user key 
				// (will not store searches for the same query multiple times, instead will just update)
				Entity search = new Entity("Search", query, results.get(0).getKey());
				
				// go through the search results
				ArrayList<EmbeddedEntity> detailedMessages = new ArrayList<EmbeddedEntity>();
				String searchResultsString = "";
				if (messages.size() > 0) {
					searchResultsString = "Your search query <i>"+query+
							"</i> found the following messages:<br><table id=\"resultsTable\">";
					searchResultsString += "<tr><th>From</th><th>Snippet</th></tr>";
					for (Message message : messages) {
						// store the message with its details into the datastore
						Entity detailedMsg = GetMessageInformation(service, userid, message.getId());
						datastore.put(detailedMsg);
						// add the message to the search itself 
						EmbeddedEntity embeddedMsg = new EmbeddedEntity();
						embeddedMsg.setKey(detailedMsg.getKey());
						embeddedMsg.setPropertiesFrom(detailedMsg);
						detailedMessages.add(embeddedMsg);
						// add the necessary strings for result output to the user
						String snippet = (String) detailedMsg.getProperty("snippet");
						String sender = (String) detailedMsg.getProperty("sender");
						searchResultsString += "<tr><td>"+Helper.ProcessSender(sender)+"</td><td>"+snippet+"</td></tr>";
					}
					searchResultsString += "</table>";
				} else {
					searchResultsString = "There were no results for <i>"+query+"</i>.";
				}
				// store the search properties
				search.setProperty("userid", userid);
				search.setProperty("searchquery", query);
				search.setProperty("searchresults", detailedMessages);
				DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
				Date date = new Date();
				search.setProperty("timestamp", dateFormat.format(date));
				datastore.put(search);
				
				// output the results
				resp.getWriter().println(searchResultsString);
			} else {
				// things have gone wrong
				System.out.println("There are "+results.size()+" users with userid:"+userid);
				resp.getWriter().println("<p>Something went wrong while trying to store the search results. Contact admin.</p>");
			}
		} else {
			resp.getWriter().println("<p>Please <a href=\"" +
		                userService.createLoginURL(thisURL) +
		                "\">sign in</a>.</p>");
		}
	}
	

	/**
	 *** Taken from: https://developers.google.com/gmail/api/v1/reference/users/messages/list ***
	 * 
	 * List all Messages of the user's mailbox matching the query.
	 *
	 * @param service Authorized Gmail API instance.
	 * @param userId User's email address. The special value "me"
	 * can be used to indicate the authenticated user.
	 * @param query String used to filter the Messages listed.
	 * @throws IOException
	 */
	public static List<Message> listMessagesMatchingQuery(Gmail service, String userId,
	     String query) throws IOException {
		ListMessagesResponse response = service.users().messages().list(userId).setQ(query).execute();
		
		List<Message> messages = new ArrayList<Message>();
		while (response.getMessages() != null) {
		  messages.addAll(response.getMessages());
		  if (response.getNextPageToken() != null) {
		    String pageToken = response.getNextPageToken();
		    response = service.users().messages().list(userId).setQ(query)
		        .setPageToken(pageToken).execute();
		  } else {
		    break;
		  }
		}
		
		return messages;
	}
	
	/** Function that reads a Message and stores the ID, the sender's email and a snippet of text in the database.
	 * 
	 * @return an Entity that has the message information
	 * @throws IOException 
	 */
	public Entity GetMessageInformation(Gmail service, String userId,  String msgid) throws IOException {
		Message msg = service.users().messages().get("me", msgid).execute();
		String snippet = msg.getSnippet();
		String sender = "";
		List<MessagePartHeader> headers = msg.getPayload().getHeaders();
		for(MessagePartHeader h :headers) {
			if(h.getName().toLowerCase().compareTo("from") == 0) {
				sender = h.getValue();
			}
		}
		Entity message = new Entity("Message", msgid);
		message.setProperty("userid", userId);
		message.setProperty("snippet", snippet);
		message.setProperty("sender", sender);
		return message;
	}
	
	
	
}
