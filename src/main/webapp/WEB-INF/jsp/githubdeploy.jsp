<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
	</p>
    <c:if test="${error != null}">
        <div class="alert">${error}</div>
    </c:if>	
    <table class="table table-striped table-condensed">
        <tr>
            <td><b>Repository Name:</b></td>
            <td><c:out value="${repositoryName}"/></td>
        </tr>
	    <c:if test="${repo != null}">
	        <tr>
	            <td><b>Repository Description:</b></td>
	            <td>${repo.getDescription()}</td>
	        </tr>
	        <tr>
	            <td><b>Repository URL:</b></td>
	            <td><a href="${repo.getHtmlUrl()}" target="_new">${repo.getHtmlUrl()}</td>
	        </tr>
	    </c:if>
        <tr>
            <td><b>Salesforce Organization Name:</b></td>
            <td><c:out value="${userContext.getOrganizationName()}"/></td>
        </tr>
        <tr>
            <td><b>Salesforce User Name:</b></td>
            <td><c:out value="${userContext.getUserName()}"/></td>
        </tr>
    </table>    
    <c:if test="${githubcontents != null}">
	    <div class="btn-group">
	        <input id="deploy" value="Deploy" type="button" onclick="GitHubDeploy.deploy();" class="btn"/>
	    </div>	    
	    <pre id="deploystatus" style="display:none">
	    </pre>
		<pre id="githubcontents"></pre>		
    </c:if>			
</div>
<script src="/resources/js/jquery-1.7.1.min.js"></script>
<c:if test="${githubcontents != null}">
	<script type="text/javascript">
	
		var GitHubDeploy = { 
		
			// Contents of the GitHub repository
			contents: ${githubcontents},
			
			// Async result from Salesforce Metadata API
			asyncResult : null,
			
			// Client timer Id used to poll Salesforce Metadata API
			intervalId : null,
			
			// Render GitHub repository contents
			render: function(container) {
					if(container.repositoryItem!=null)
						$('#githubcontents').append(
							'<div><a target="_new" href="${repo.getHtmlUrl()}/blob/master/' + 
								container.repositoryItem.path + '">' + container.repositoryItem.path + '</a></div>');
					for(fileIdx in container.repositoryItems)
						if(container.repositoryItems[fileIdx].repositoryItem.type == 'dir')
							GitHubDeploy.render(container.repositoryItems[fileIdx]);
						else
							$('#githubcontents').append(
								'<div><a target="_new" href="${repo.getHtmlUrl()}/blob/master/' + 
									container.repositoryItems[fileIdx].repositoryItem.path + '">' + 
									container.repositoryItems[fileIdx].repositoryItem.path + '</a></div>');
				},
				
			// Deploy
			deploy: function() {
					$('#deploy').attr('disabled', 'disabled');
					$('#deploystatus').empty();
					$('#deploystatus').show();
					$('#deploystatus').append('Deployment Started');
		            $.ajax({
		                type: 'POST',
		                processData : false,
		                data : JSON.stringify(GitHubDeploy.contents),
		                contentType : "application/json; charset=utf-8",
		                dataType : "json",
		                success: function(data, textStatus, jqXHR) {
		                    GitHubDeploy.asyncResult = data;
		                    GitHubDeploy.renderAsync();
		                    if(GitHubDeploy.asyncResult.state == 'Completed')
		                    	GitHubDeploy.checkDeploy();
		                    else
		                    	GitHubDeploy.intervalId = window.setInterval(GitHubDeploy.checkStatus, 2000);
		                },
		                error: function(jqXHR, textStatus, errorThrown) {
		                    alert('Failed ' + textStatus + errorThrown);
		                }
		            });			
				},
				
			// Render Async
			renderAsync: function() {
					$('#deploystatus').append(
						'<div>Status: '+ 
							GitHubDeploy.asyncResult.state + ' ' + 
							(GitHubDeploy.asyncResult.message != null ? GitHubDeploy.asyncResult.message : '') +
						'</div>');
				},
				
			// Check Status
			checkStatus: function() {
		            $.ajax({
		                type: 'GET',
		                url: window.location + '/checkstatus/' + GitHubDeploy.asyncResult.id,
		                contentType : 'application/json; charset=utf-8',
		                dataType : 'json',
		                success: function(data, textStatus, jqXHR) {
		                    GitHubDeploy.asyncResult = data;
		                    GitHubDeploy.renderAsync();
		                    if(GitHubDeploy.asyncResult.state == 'Completed')
		                    {
		                    	window.clearInterval(GitHubDeploy.intervalId);
		                    	GitHubDeploy.checkDeploy();
		                    }
		                },
		                error: function(jqXHR, textStatus, errorThrown) {
		                	$('#deploystatus').append('<div>Error: ' + textStatus + errorThrown + '</div>');
		                }
		            });					
				},
			
			// Check Deploy
			checkDeploy: function() {
					$('#deploystatus').append('Deployment Complete');		
					$('#deploy').attr('disabled', null);
		            $.ajax({
		                type: 'GET',
		                url: window.location + '/checkdeploy/' + GitHubDeploy.asyncResult.id,
		                contentType : 'application/json; charset=utf-8',
		                dataType : 'json',
		                success: function(data, textStatus, jqXHR) {
		                	$('#deploystatus').append(data);
		                },
		                error: function(jqXHR, textStatus, errorThrown) {
		                	$('#deploystatus').append('<div>Error: ' + textStatus + errorThrown + '</div>');
		                }
		            });													
				}
		}
		
		// Render files selected to deploy
		GitHubDeploy.render(GitHubDeploy.contents);
		
	</script>
</c:if>	
</body>
</html>