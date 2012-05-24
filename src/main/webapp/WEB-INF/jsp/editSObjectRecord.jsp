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
                <h1>${record.metadata.label}: ${record.get("Name").value}</h1>
            </div>
            <form method="POST" action="">
                <table class="table table-striped table-condensed">
                    <c:forEach items="${record.fields}" var="field">
                        <tr>
                            <td><label for="${field.metadata.name}"><c:out value="${field.metadata.label}"/></label></td>
                            <td><input name="${field.metadata.name}" value="<c:out value="${field.value}"/>"/></td>
                        </tr>
                    </c:forEach>
                </table>
                <input type="submit" value="Save" class="btn btn-primary">
            </form>
        </div>
    </div>
</div>

</body>
</html>
