<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
    <div class="row">
        <div class="span8 offset2">
            <div class="page-header">
                <h1>${record.metadata.label}: ${record.get("name").value}</h1>
                <h6><a href="../${record.metadata.name}" style="color: gray;">Back to ${record.metadata.labelPlural}</a></h6>
            </div>

            <div class="btn-group">
                <a href="${record.get("id").value}/e" class="btn">Edit</a>
                <a onclick="SFDC.deleteSObjectRecord('${record.metadata.name}', '${record.get("id").value}', '${record.get("name").value}')" class="btn">Delete</a>
            </div>

            <table class="table table-striped table-condensed">
            <c:forEach items="${record.fields}" var="field">
                <tr>
                    <td><c:out value="${field.metadata.label}"/></td>
                    <td><c:out value="${field.value}"/></td>
                </tr>
            </c:forEach>
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