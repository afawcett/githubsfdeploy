<!doctype html>
<html>
<head>
    <meta charset="utf-8">
    <title>GitHub Salesforce Deploy Tool</title>

    <meta content="IE=edge,chrome=1" http-equiv="X-UA-Compatible">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link href="/resources/css/bootstrap.min.css" rel="stylesheet">
    <link href="/resources/css/bootstrap-responsive.min.css" rel="stylesheet">

    <!--
      IMPORTANT:
      This is Heroku specific styling. Remove to customize.
    -->
    <link href="/resources/css/heroku.css" rel="stylesheet">
    <!-- /// -->

    <style>
        .btn-group {
            margin-top: 10px;
            margin-bottom: 10px;
        }
        input:focus {
    		outline: none;
		}
    </style>

</head>

<script>
var appName = ''
function githubdeploy()
{
	var sfdeployurl =
		$('#production').attr('checked') ?
			'https://githubsfdeploy.herokuapp.com/app/githubdeploy' :
			'https://githubsfdeploy-sandbox.herokuapp.com/app/githubdeploy';
	sfdeployurl+= '/' + $('#owner').val() + '/' + $('#repo').val();
	window.location = sfdeployurl;
}
function togglebuttoncode()
{
	if($('#showbuttoncode').attr('checked') == 'checked')
		$('#buttoncodepanel').show();
	else
		$('#buttoncodepanel').hide();
}
function updatebuttonhtml()
{
	var repoOwner = $('#owner').val();
	var repoName = $('#repo').val();
	var buttonhtml =
		( $('#blogpaste').attr('checked') == 'checked' ? 
			'<a href="https://githubsfdeploy.herokuapp.com?owner=' + repoOwner +'&repo=' + repoName + '">\n' :
			'<a href="https://githubsfdeploy.herokuapp.com">\n') +					
			'  <img alt="Deploy to Salesforce"\n' +
			'       src="https://raw.githubusercontent.com/afawcett/githubsfdeploy/master/src/main/webapp/resources/img/deploy.png">\n' +
		'</a>';
	$('#buttonhtml').text(buttonhtml);
}
function load()
{
	// Default from URL
	var owner = $.url().param('owner');
	var repo = $.url().param('repo');

	// Check for GitHub referrer?			
	if(owner==null && repo==null) {
		var referrer = document.referrer;
		// Note this is not passed from private repos or to http://localhost
		if(referrer!=null && referrer.startsWith('https://github.com')) {		
			var parts = referrer.split('/');
			if(parts.length >= 5) {
				owner = parts[3];
				repo = parts[4];
			}
		}		
	}
	
	// Default fields
	$('#owner').val(owner);
	$('#repo').val(repo);
	
	
	$('#login').focus();
	updatebuttonhtml();
}
</script>

<body onload="load();">
<div class="navbar navbar-fixed-top">
    <div class="navbar-inner">
        <div class="container">
            <a href="/" class="brand">GitHub Salesforce Deploy Tool</a>
            <a target="_new" href="http://andyinthecloud.com" class="brand" id="heroku">by <strong>andyinthecloud</strong></a>
        </div>
    </div>
</div>

<div class="container">
	<br/>
	<form onsubmit="loginToSalesforce();return false;">
		<label><input type="radio" id="production" name="environment" checked="true" value="production">&nbsp;Production / Developer</label>
		<label><input type="radio" id="sandbox" name="environment" value="sandbox">&nbsp;Sandbox</label>
		<table>
			<tr>
				<td>
					<label>Owner</label>
				</td>
				<td>
					<label>Repository</label>
				</td>
			</tr>
			<tr>
				<td>
					<input id="owner" oninput="updatebuttonhtml();"/>&nbsp;
				</td>
				<td>
					<input id="repo" oninput="updatebuttonhtml();"/>
				</td>
			</tr>
		</table>
		<br/>
		<p><input type="submit" id="login" value="Login to Salesforce" onclick="githubdeploy();return false;"/></p>
		<p><label for="showbuttoncode" style="color:grey">Show GitHub README button code&nbsp;<input id="showbuttoncode" type="checkbox" onclick="togglebuttoncode();"/></label></p>
		<div id="buttoncodepanel" style="display:none">
			<p>Copy paste the HTML code below and insert into your <b>GitHub README</b> to display the button, <u>if pasting into any other place see the note below</u>.</p>
			<p><img src="/resources/img/deploy.png"/></p>
			<span style="border: 1px grey"><pre id="buttonhtml"></pre></span>
			<p><label for="blogpaste" style="color:grey"><b>IMPORTANT:</b> If your pasting into a blog or article, the button needs to know the repository name, check this box!&nbsp;<input id="blogpaste" type="checkbox" onclick="updatebuttonhtml();"/></label></p>
		</div>
	</form>
</div>

<script src="/resources/js/jquery-1.7.1.min.js"></script>
<script src="/resources/js/purl.js"></script>
</body>
</html>