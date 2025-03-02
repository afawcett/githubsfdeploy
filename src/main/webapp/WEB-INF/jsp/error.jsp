<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<jsp:include page="header.jsp" />

<main class="container my-5">
    <div class="row justify-content-center">
        <div class="col-lg-8 col-md-10 col-sm-12">
            <div class="page-header">
                <h1 class="display-4">Oops, something went wrong...</h1>
            </div>

            <div class="alert alert-warning">
                <%
                    String errorMessage = (String) request.getAttribute("errorMessage");
                    if (errorMessage == null) {
                        errorMessage = (String) session.getAttribute("errorMessage");
                    }
                    if (errorMessage == null) {
                        errorMessage = "No specific error message available.";
                    }
                %>
                <p><strong>Error Details:</strong> <%= errorMessage %></p>
                
                <% if (errorMessage != null && errorMessage.contains("OAuth login invalid or expired access token")) { %>
                    <p class="mt-3">Be sure your Salesforce organization does not have IP restrictions. Logout and try with another user or organization.</p>
                    <p><a class="btn btn-danger mt-3" href="/logout">Logout</a></p>
                <% } %>
            </div>
        </div>
    </div>
</main>

<jsp:include page="footer.jsp" />
