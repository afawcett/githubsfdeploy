<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
	</p>
	<pre>${deploymentFiles}</pre>
	<pre>${deployProgress}</pre>
	<pre>${resultErrorMessages}</pre>
<jsp:include page="footer.jsp"/>