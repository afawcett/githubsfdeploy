<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
<div class="row">
    <div class="span8 offset2">
        <div class="page-header">
            <h1>Oops, something went wrong...</h1>
        </div>

        <div class="alert">
            <p><c:out value="${requestScope['javax.servlet.error.message']}"/></p>

            <c:if test="${requestScope['javax.servlet.error.message'] == 'Authentication Failed: OAuth login invalid or expired access token'}">
                <p>Be sure your Salesforce organization does not have IP restrictions. Logout and try with another user or organization.</p>
                <p><a class="btn btn-inverse" href="/logout">Logout</a></p>
            </c:if>
        </div>

    </div>
</div>
<jsp:include page="footer.jsp"/>