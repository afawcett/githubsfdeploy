<!doctype html>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<html>
<head>
    <meta charset="utf-8">
    <title>Salesforce Spring MVC Template</title>

    <meta content="IE=edge,chrome=1" http-equiv="X-UA-Compatible">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link href="http://twitter.github.com/bootstrap/assets/css/bootstrap.css" rel="stylesheet">
    <link href="http://twitter.github.com/bootstrap/assets/css/bootstrap-responsive.css" rel="stylesheet">

    <!--
      IMPORTANT:
      This is Heroku specific styling. Remove to customize.
    -->
    <link href="http://heroku.github.com/template-app-bootstrap/heroku.css" rel="stylesheet">
    <!-- /// -->

</head>

<body>
<div class="navbar navbar-fixed-top">
    <div class="navbar-inner">
        <div class="container">
            <a href="/" class="brand">Salesforce Spring MVC Template</a>
            <a href="/" class="brand" id="heroku">by <strong>heroku</strong></a>
        </div>
    </div>
</div>

<div class="container">
    <div class="row">
        <div class="span8 offset2">
            <div class="page-header">
                <h1>${record.metadata.label}: ${record.get("name").value}</h1>
            </div>

            <div class="btn-group" style="margin: 10px">
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
</div>

<script src="http://twitter.github.com/bootstrap/assets/js/jquery.js"></script>

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

</body>
</html>
