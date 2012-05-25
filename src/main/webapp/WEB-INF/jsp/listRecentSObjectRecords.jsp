<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
    <div class="row">
        <div class="span8 offset2">
            <div class="page-header">
                <h1><c:out value="${type.labelPlural}"/></h1>
                <h6><a href="../sobjects" style="color: gray;">Back to My Objects</a></h6>
            </div>

            <div class="btn-group">
                <a href="${type.name}/e" class="btn btn-primary">New</a>
            </div>

            <h2>Recent Items</h2>
            <c:forEach items="${recentRecords}" var="record">
                <ul>
                    <li><a href="${record.metadata.name}/${record.get("id").value}"><c:out value="${record.get('name').value}"/></a></li>
                </ul>
            </c:forEach>
        </div>
    </div>
<jsp:include page="footer.jsp"/>