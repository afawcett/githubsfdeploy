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
                    
                    <c:forEach items="${contactList}" var="contact">
                        <tr>
                            <td>
                            	<a href="contacts/${contact.getField("id").value}">
                            		${contact.getField("lastname").value}, ${contact.getField("firstname").value}
                            	</a>
                            </td>
                            <td>${contact.getField("email").value}</td>
                            <td>
                            	
                            	<a href="#" onClick="SFDC.deleteSObjectRecord(	'${contact.metadata.name}', 
                            													'${contact.getField("id").value}', 
                            													'${contact.getField("firstname").value} ${contact.getField("lastname").value}')"
                            		class="btn btn-danger btn-mini">Delete</a>
                            	</form>
                            </td>
                        </tr>
                    </c:forEach>
                    
                    </tbody>
                </table>
        </div>
    </div>
    
    
    <script type="text/javascript">
    var SFDC = {
        deleteSObjectRecord: function(type, id, name) {
            if (!confirm("Are you sure you want to delete '" +  name + "'?")) {
                return false;
            }

            $.ajax({
                'url': 'contacts/'+id,
                'type': 'DELETE',
                'success': function(data, textStatus, jqXHR) {
                    location.href = '/sfdc/contacts'
                },
                'error': function(jqXHR, textStatus, errorThrown) {
                    alert('Failed to delete record.');
                }
            })
        }
    };
</script>
<jsp:include page="footer.jsp"/>