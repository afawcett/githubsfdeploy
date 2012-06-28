<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
    <div class="row">
        <div class="span8 offset2">
            <div class="btn-group">
                <a href="${record.getField("id").value}/e" class="btn">Edit</a>
                <a onclick="SFDC.deleteSObjectRecord('${record.metadata.name}', '${record.getField("id").value}', '${record.getField("name").value}')" class="btn">Delete</a>
            </div>

            <table class="table table-striped table-condensed">
                <tr>
                    <td><c:out value="${record.getField('firstname').metadata.label}"/></td>
                    <td><c:out value="${record.getField('firstname').value}"/></td>
                </tr>
                <tr>
                    <td><c:out value="${record.getField('lastname').metadata.label}"/></td>
                    <td><c:out value="${record.getField('lastname').value}"/></td>
                </tr>
                <tr>
                    <td><c:out value="${record.getField('email').metadata.label}"/></td>
                    <td><c:out value="${record.getField('email').value}"/></td>
                </tr>
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
                'url': id,
                'type': 'DELETE',
                'success': function(data, textStatus, jqXHR) {
                    location.href = '../' + type
                },
                'error': function(jqXHR, textStatus, errorThrown) {
                    alert('Failed to delete record.');
                }
            })
        }
    };
</script>

<jsp:include page="footer.jsp"/>