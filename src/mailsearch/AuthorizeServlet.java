package mailsearch;

import java.io.IOException;


import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.extensions.appengine.auth.oauth2.AbstractAppEngineAuthorizationCodeServlet;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

/** Servlet used to create the authorization flow and store tokens.**/
@SuppressWarnings("serial")
public class AuthorizeServlet extends AbstractAppEngineAuthorizationCodeServlet {

	@Override
	protected String getRedirectUri(HttpServletRequest req) throws ServletException, IOException {
		return Helper.getRedirectUri(req);
	}
	
	@Override
	protected AuthorizationCodeFlow initializeFlow() throws IOException {
		return Helper.newFlow();
	}
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException, ServletException {
		resp.setContentType("text/html");
		String thisURL = req.getRequestURI();
		UserService userService = UserServiceFactory.getUserService();
		// User user = userService.getCurrentUser();
		if (req.getUserPrincipal() != null) {
			req.getRequestDispatcher("search.jsp").forward(req, resp);
		} else {
			resp.getWriter().println("<p>Please <a href=\"" +
		                userService.createLoginURL(thisURL) +
		                "\">sign in</a>.</p>");
		}
	}
}
