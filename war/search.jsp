<!DOCTYPE HTML>

<%@ page import="com.google.appengine.api.users.User" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>
<%@ page import="com.google.appengine.api.datastore.DatastoreServiceFactory" %>
<%@ page import=" com.google.appengine.api.datastore.DatastoreService" %>
<%@ page import=" com.google.appengine.api.datastore.Query.Filter" %>
<%@ page import=" com.google.appengine.api.datastore.Query.FilterPredicate" %>
<%@ page import=" com.google.appengine.api.datastore.Query.FilterOperator" %>
<%@ page import=" com.google.appengine.api.datastore.Query" %>
<%@ page import=" com.google.appengine.api.datastore.PreparedQuery" %>
<%@ page import=" com.google.appengine.api.datastore.Entity" %>
<%@ page import=" com.google.appengine.api.datastore.FetchOptions" %>
<%@ page import=" java.util.List" %>

<html>
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8">
    <title>Gmail Search App</title>
    <link type="text/css" rel="stylesheet" href="/stylesheets/main.css"/>
    <script src="//code.jquery.com/jquery-1.11.2.min.js"></script>
  </head>

  <body>
    <h1>Gmail Search App</h1>
	
   	<jsp:include page="login.jsp"/>
   	<%
		UserService userService = UserServiceFactory.getUserService();
		User user = userService.getCurrentUser();
		if (user != null) {
			// get user id
			String id = user.getUserId();
			// query db with filter
			// Get the Datastore Service
			DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
			Filter userIdFilter =
					  new FilterPredicate("userid",
					                      FilterOperator.EQUAL,
					                      id);
			Query q = new Query("Customer").setFilter(userIdFilter);
			PreparedQuery pq = datastore.prepare(q);
			List<Entity> results = pq.asList(FetchOptions.Builder.withLimit(3));
			if (results.size() == 0) {
				// add user if not found in db
				Entity customer = new Entity("Customer");
				customer.setProperty("userid", id);
				datastore.put(customer);
			}
			
	%>
		   	
    <table>
    	<tr>
        	<td><input name="searchterm" id="searchInputField"/></td>        
        	<td><button id="searchButton">Search Mail Box</button></td>
      	</tr>
    </table>
    <p id="result">
    <%
		} else {		
	%>
		You can only search a mailbox after signing in!
	<%
		}	
	%>
	</p>
  </body>
  <script>
  	$("#searchButton").on("click", function() {
  		$.get( "mailsearch?s="+$( "#searchInputField" ).val(), function( data ) {
  			$( "#result" ).html( data );
  		});
  	});
  	$("#previousSearches").on("click", function() {
  		$.get( "/previous", function( data ) {
  			$( "#result" ).html( data );
  		});
  	});
  </script>
</html>
