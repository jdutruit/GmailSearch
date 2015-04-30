<%@ page contentType="text/html;charset=UTF-8" language="java"%>
<%@ page import="com.google.appengine.api.users.User"%>
<%@ page import="com.google.appengine.api.users.UserService"%>
<%@ page import="com.google.appengine.api.users.UserServiceFactory"%>
<%@ page import="java.util.List"%>
<%@ taglib uri="http://java.sun.com/jstl/core" prefix="c" %>

<%
	UserService userService = UserServiceFactory.getUserService();
	User user = userService.getCurrentUser();
	if (user != null) {
		pageContext.setAttribute("user", user);
%>

<p>
	Hello, ${user.nickname}! (You can <a
		href="<%=userService.createLogoutURL(request.getRequestURI())%>">sign
		out here</a>.) <br>
	To have a look at your previous searches: <button id="previousSearches">Click Here</button>
</p>
<%
	} else {
%>
<p>
	<a href="<%=userService.createLoginURL(request.getRequestURI())%>">Sign
		in here.</a>
</p>
<%
	}
%>
