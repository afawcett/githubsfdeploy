<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
    <div class="row">
        <div class="span8 offset2">
            <div class="page-header">
                <h1>
                    ${record.metadata.label}
                    <c:if test="${record.getField('name').value != null}">
                        : ${record.getField("name").value}
                    </c:if>
                </h1>
            </div>

            <c:if test="${error != null}">
                <div class="alert">${error}</div>
            </c:if>

            <form method="POST" action="">
                <div class="btn-group">
                    <input type="submit" value="Save" class="btn btn-primary">
                </div>
                <table class="table table-striped table-condensed">
   		                <tr>
		                    <td><label for="${record.getField('firstname').metadata.name}"><c:out value="${record.getField('firstname').metadata.label}"/></label></td>
		                    <td><input id="${record.getField('firstname').metadata.name}"
                                       name="${record.getField('firstname').metadata.name}"
                                       value="<c:out value="${record.getField('firstname').value}"/>"/></td>
		                </tr>
		                <tr>
		                    <td><label for="${record.getField('lastname').metadata.name}"><c:out value="${record.getField('lastname').metadata.label}"/></label></td>
		                    <td><input id="${record.getField('lastname').metadata.name}"
                                       name="${record.getField('lastname').metadata.name}"
                                       value="<c:out value="${record.getField('lastname').value}"/>"/></td>
		                </tr>
		                <tr>
		                    <td><label for="${record.getField('email').metadata.name}"><c:out value="${record.getField('email').metadata.label}"/></label></td>
		                    <td><input id="${record.getField('email').metadata.name}"
                                       name="${record.getField('email').metadata.name}"
                                       value="<c:out value="${record.getField('email').value}"/>"/></td>
		                </tr>
		                
                </table>
                <div class="btn-group">
                    <input type="submit" value="Save" class="btn btn-primary">
                </div>
            </form>
        </div>
    </div>
<jsp:include page="footer.jsp"/>