<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<jsp:include page="header.jsp" />

<main class="container my-5">
    <div class="row justify-content-center">
        <div class="col-lg-8 col-md-10 col-sm-12">
            <div class="page-header">
                <h1 class="display-4">Login Required</h1>
            </div>

            <div class="alert alert-info">
                <p>You need to authenticate with Salesforce to continue.</p>
                <p><a class="btn btn-primary mt-3" href="/oauth2/authorization/salesforce">Login with Salesforce</a></p>
            </div>
        </div>
    </div>
</main>

<jsp:include page="footer.jsp" /> 