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
import com.google.appengine.api.datastore.Query.SortDirection;

import java.util.ArrayList;
import java.util.List;


/** Servlet that is used to show the previous searches a user has made. Gets info from the datastore and outputs HTML. **/
@SuppressWarnings("serial")
public class DatastoreRetrieveServlet extends HttpServlet {
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		resp.setContentType("text/html");
		String thisURL = req.getRequestURI();
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		if (req.getUserPrincipal() != null) {
			
			String userid = user.getUserId();
			
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
				// Prepare the html code to return
				String html = "<h2>Previous searches</h2>";
				
				// find user's searches
				Query searchQuery = new Query("Search").setAncestor(results.get(0).getKey()).addSort("timestamp", SortDirection.DESCENDING);
				PreparedQuery searchPq = datastore.prepare(searchQuery);
				List<Entity> searchResults = searchPq.asList(FetchOptions.Builder.withDefaults());
				if (searchResults.size() > 0) {
					html += "<table id=searchResultTable><tr><th>Query</th><th>Results</th><th>Timestamp</th></tr>";
					for (Entity sr : searchResults) {
						String query = (String) sr.getProperty("searchquery");
						String timestamp = (String) sr.getProperty("timestamp");
						ArrayList<EmbeddedEntity> sResultsList = (ArrayList<EmbeddedEntity>) sr.getProperty("searchresults");
						if (sResultsList != null) {
							html += "<tr><td>"+query+"</td><td><table>";
							for (EmbeddedEntity sResults: sResultsList) {
								String snippet = (String) sResults.getProperty("snippet");
								String sender = (String) sResults.getProperty("sender");
								html += "<tr><td>"+Helper.ProcessSender(sender)+"</td><td>"+snippet+"</td></tr>";
							}
							html += "</table></td><td>"+timestamp+"</td></tr>";
						} else {
							html += "<tr><td>"+query+"</td><td>No results.</td><td>"+timestamp+"</td></tr>";
						}
					}
					html += "</table>";
				} else {
					html += "You have no previous searches.";
				}
				// output the results
				resp.getWriter().println(html);
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
	
}
