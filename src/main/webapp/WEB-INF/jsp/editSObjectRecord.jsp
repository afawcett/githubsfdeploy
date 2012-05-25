<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
    <div class="row">
        <div class="span8 offset2">
            <div class="page-header">
                <h1>${record.metadata.label}: ${record.get("Name").value}</h1>
            </div>

            <c:if test="${error != null}">
                <div class="alert">${error}</div>
            </c:if>

            <form method="POST" action="">
                <table class="table table-striped table-condensed">
                    <c:forEach items="${record.fields}" var="field">
                        <tr>
                            <td><label for="${field.metadata.name}"><c:out value="${field.metadata.label}"/></label></td>
                            <td>
                                <input id="${field.metadata.name}"
                                       name="${field.metadata.name}"
                                       value="<c:out value="${field.value}"/>"/>
                            </td>
                        </tr>
                    </c:forEach>
                </table>
                <input type="submit" value="Save" class="btn btn-primary">
            </form>
        </div>
    </div>
<jsp:include page="footer.jsp"/>