<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<jsp:include page="header.jsp"/>
	    <c:if test="${githubcontents != null}">
	   	<div class="slds-col slds-no-flex slds-align-bottom">
	      <div class="slds-button-group" role="group">
	        <button id="deploy" class="slds-button slds-button--neutral" onclick="GitHubDeploy.deploy();">Deploy</button>
	      </div>
	    </div>		
	    </c:if>
	</div>
</div>


<c:if test="${error != null}">
	<div class="slds-notify_container">
		<div class="slds-notify slds-notify--alert slds-theme--alert-texture" role="alert">
			<h2>${error}</h2>
		</div>
	</div>
</c:if>
&nbsp;

<div class="slds-card">
	<div class="slds-card__header slds-grid">
		<div class="slds-media slds-media--center slds-has-flexi-truncate">
			<div class="slds-media__figure">
				<svg aria-hidden="true"
					class="slds-icon slds-icon-action-share slds-icon--small">
            	<use
						xlink:href="/resources/assets/icons/action-sprite/svg/symbols.svg#share"></use>
          	</svg>
			</div>
			<div class="slds-media__body">
				<h2 class="slds-text-heading--small slds-truncate">From GitHub
					Repository</h2>
			</div>
		</div>
	</div>
	<div class="slds-card__body">
		<ul>
			<li class="slds-tile slds-hint-parent">
				<div class="slds-tile__detail">
					<dl class="slds-dl--horizontal slds-text-body--small">
						<c:if test="${githuburl != null}">
							<dt class="slds-dl--horizontal__label">
								<p class="slds-truncate">Manage GitHub Permissions:</p>
							</dt>
							<dd class="slds-dl--horizontal__detail slds-tile__meta">
								<p class="slds-truncate">
									<a href="${githuburl}" target="_new">${githuburl}</a>
								</p>
							</dd>
						</c:if>
						<dt class="slds-dl--horizontal__label">
							<p class="slds-truncate">Name:</p>
						</dt>
						<dd class="slds-dl--horizontal__detail slds-tile__meta">
							<p class="slds-truncate">${repositoryName}</p>
						</dd>
                        <dt class="slds-dl--horizontal__label">
                            <p class="slds-truncate">Branch/Tag/Commit:</p>
                        </dt>
                        <dd class="slds-dl--horizontal__detail slds-tile__meta">
                            <p class="slds-truncate">${ref}</p>
                        </dd>                        
						<c:if test="${repo != null}">
							<dt class="slds-dl--horizontal__label">
								<p class="slds-truncate">Description:</p>
							</dt>
							<dd class="slds-dl--horizontal__detail slds-tile__meta">
								<p class="slds-truncate">${repo.getDescription()}</p>
							</dd>
							<dt class="slds-dl--horizontal__label">
								<p class="slds-truncate">URL:</p>
							</dt>
							<dd class="slds-dl--horizontal__detail slds-tile__meta">
								<p class="slds-truncate">
                                    <a href="${repo.getHtmlUrl()}/tree/${ref}" target="_new">${repo.getHtmlUrl()}/tree/${ref}</a>
								</p>
							</dd>
						</c:if>
					</dl>
				</div>
			</li>
		</ul>
	</div>
</div>
<div class="slds-card">
	<div class="slds-card__header slds-grid">
		<div class="slds-media slds-media--center slds-has-flexi-truncate">
			<div class="slds-media__figure">
				<svg aria-hidden="true"
					class="slds-icon icon-utility-salesforce-1 slds-icon-text-default slds-icon--small">
            		<use
						xlink:href="/resources/assets/icons/utility-sprite/svg/symbols.svg#salesforce1"></use>
          		</svg>
			</div>
			<div class="slds-media__body">
				<h2 class="slds-text-heading--small slds-truncate">To
					Salesforce Org</h2>
			</div>
		</div>
	</div>
	<div class="slds-card__body">
		<ul>
			<li class="slds-tile slds-hint-parent">
				<div class="slds-tile__detail">
					<dl class="slds-dl--horizontal slds-text-body--small">
						<dt class="slds-dl--horizontal__label">
							<p class="slds-truncate">Organization Name:</p>
						</dt>
						<dd class="slds-dl--horizontal__detail slds-tile__meta">
							<p class="slds-truncate">
								<c:out value="${organizationName}" />
							</p>
						</dd>
						<dt class="slds-dl--horizontal__label">
							<p class="slds-truncate">User Name:</p>
						</dt>
						<dd class="slds-dl--horizontal__detail slds-tile__meta">
							<p class="slds-truncate">
								<c:out value="${userName}" />
							</p>
						</dd>
					</dl>
				</div>
			</li>
		</ul>
	</div>
</div>

<c:if test="${githubcontents != null}">
	<pre id="deploystatus" style="display: none"></pre>
	<div id="githubcontents"></div>
</c:if>

<script src="/resources/js/jquery-1.7.1.min.js"></script>
<c:if test="${githubcontents != null}">
	<script type="text/javascript">

		var GitHubDeploy = {

			// Contents of the GitHub repository
			contents: ${githubcontents},

			// Async result from Salesforce Metadata API
			deployResult : null,

			// Client timer Id used to poll Salesforce Metadata API
			intervalId : null,

			// Render GitHub repository contents
			render: function(container) {
					if(container.repositoryItem!=null)
						$('#githubcontents').append(
							'<div><a target="_new" href="${repo.getHtmlUrl()}/blob/${ref}/' +
								container.repositoryItem.path + '">' + container.repositoryItem.path + '</a></div>');
					for(fileIdx in container.repositoryItems) {
						if(container.repositoryItems[fileIdx].repositoryItem.type == 'dir') {
							GitHubDeploy.render(container.repositoryItems[fileIdx]);
						}  else {
							if(GitHubDeploy.contents.downloadId!=null) {
								// Since SFDX repos go through conversion there is no mapping back to original repo file
								$('#githubcontents').append(
									'<div>' + container.repositoryItems[fileIdx].repositoryItem.path + '</div>');	
							} else {
								$('#githubcontents').append(
									'<div><a target="_new" href="${repo.getHtmlUrl()}/blob/${ref}/' +
										container.repositoryItems[fileIdx].repositoryItem.path + '">' +
										container.repositoryItems[fileIdx].repositoryItem.path + '</a></div>');	
							}
						}
					}
				},

			// Check Status
			checkStatus: function() {
				$.ajax({
					type: 'GET',
					url: window.pathname + '/checkstatus/' + GitHubDeploy.deployResult.id,
					contentType : 'application/json; charset=utf-8',
					dataType : 'json',
					success: function(data, textStatus, jqXHR) {
						GitHubDeploy.deployResult = data;
						GitHubDeploy.renderDeploy();
						if(GitHubDeploy.deployResult.completedDate)
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
		                    GitHubDeploy.deployResult = data;
							$('#deploystatus').append(
								'<div>Status: '+
									GitHubDeploy.deployResult.state + ' ' +
									(GitHubDeploy.deployResult.message != null ? GitHubDeploy.deployResult.message : '') +
								'</div>');		
		                    if(GitHubDeploy.deployResult.completedDate)
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
			renderDeploy: function() {
					$('#deploystatus').append(
						'<div>Status: '+
							GitHubDeploy.deployResult.status + ' ' +
							(GitHubDeploy.deployResult.message != null ? GitHubDeploy.deployResult.message : '') +
						'</div>');
				},


			// Check Deploy
			checkDeploy: function() {
					$('#deploystatus').append('Deployment Complete');
					$('#deploy').attr('disabled', null);
		            $.ajax({
		                type: 'GET',
		                url: window.pathname + '/checkdeploy/' + GitHubDeploy.deployResult.id,
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