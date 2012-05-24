<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
    <div class="row">
        <div class="span8 offset2">
            <div class="page-header">
                <h1>SObjects</h1>
            </div>

            <c:forEach items="${types}" var="type">
                <ul>
                    <li><a href="sobjects/${type.name}"><c:out value="${type.label}"/></a></li>
                </ul>
            </c:forEach>
        </div>
    </div>
<jsp:include page="footer.jsp"/>