<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<jsp:include page="header.jsp"/>
    <div class="row">
        <div class="span8 offset2">
                <table class="table table-bordered table-striped">
                    <thead>
                    <tr>
                        <th>Name</th>
                        <th>Email</th>
                        <th>&nbsp;</th>
                    </tr>
                    </thead>
                    <tbody>
                    
                    <c:forEach items="${personList}" var="person">
                        <tr>
                            <td>
                            	<a href="persons/${person.getField("id").value}">
                            		${person.getField("lastname").value}, ${person.getField("firstname").value}
                            	</a>
                            </td>
                            <td>${person.getField("email").value}</td>
                            <td>
                            	<form action="delete/${person.getField("id").value}" method="post">
                            		<input type="submit" class="btn btn-danger btn-mini" value="Delete"/>
                            	</form>
                            </td>
                        </tr>
                    </c:forEach>
                    
                    </tbody>
                </table>
        </div>
    </div>
<jsp:include page="footer.jsp"/>