package com.example.controller;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.force.sdk.connector.ForceServiceConnector;
import com.sforce.soap.metadata.AsyncRequestState;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CodeCoverageWarning;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.RunTestFailure;
import com.sforce.soap.metadata.RunTestsResult;

// http://localhost:8080/sfdc/githubdeploy/financialforcedev/fflib-apex-common

@Controller
@RequestMapping("/githubdeploy")
public class GitHubSalesforceDeployController {
    
    private static final long ONE_SECOND = 1000;
    private static final int MAX_NUM_POLL_REQUESTS = 50;     
	
    @RequestMapping("/{owner}/{repo}")
    public String listContents(@PathVariable("owner") String repoOwner, @PathVariable("repo") String repoName, Map<String, Object> map) throws Exception {
        
    	// Connect via oAuth client and secret to get greater request limits
    	GitHubClientOAuthServer client = 
    		new GitHubClientOAuthServer("c9172f8413daedef3f1f", "b53b2ecb62b7a328a6f6889edf7867426aedf4a2" );
    	
    	// Repo description
    	RepositoryService service = new RepositoryService(client);
    	RepositoryId repoId = RepositoryId.create(repoOwner, repoName);
    	Repository repo = service.getRepository(repoId);    	
        System.out.println( repo.getDescription() );
        
        // Content service
        ContentsServiceEx contentService = new ContentsServiceEx(client);
        
        // Download Zip and generate Metadata Deploy Zip!
        String salesforceDeployRoot = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zipOS = new ZipOutputStream(baos);
        ZipInputStream zipIS = contentService.getArchiveAsZip(repoId);
		while(true)
		{
			ZipEntry zipEntry = zipIS.getNextEntry();
			if(zipEntry==null)
				break;
			// Found /src folder?
			if(zipEntry.getName().endsWith("/src/"))
			{
				salesforceDeployRoot = zipEntry.getName();
				while(true)
				{
					zipEntry = zipIS.getNextEntry();
					if(zipEntry==null || !zipEntry.getName().startsWith(salesforceDeployRoot))
						break;
					// Generate the Metadata zip entry name (from regex above?)
					String metadataZipEntryName = zipEntry.getName().substring(salesforceDeployRoot.length());					
					ZipEntry metadataZipEntry = new ZipEntry(metadataZipEntryName);
					System.out.println(metadataZipEntryName);					
					zipOS.putNextEntry(metadataZipEntry);
					// Copy bytes over from Github archive input stream to Metadata zip output stream
					byte[] buffer = new byte[1024];
					int length = 0;
					while((length = zipIS.read(buffer)) > 0)
						zipOS.write(buffer, 0, length);
					zipOS.closeEntry();
				}
				break;
			}
		}     
		zipOS.close();
		if(salesforceDeployRoot==null)
			throw new Exception("Did not find the /src folder");
				
		// Connect to Salesforce Metadata API
        ForceServiceConnector connector = new ForceServiceConnector(ForceServiceConnector.getThreadLocalConnectorConfig());
        MetadataConnection metadataConnection = connector.getMetadataConnection();
		DeployOptions deployOptions = new DeployOptions();
		deployOptions.setSinglePackage(true);
		deployOptions.setPerformRetrieve(false);
		deployOptions.setRollbackOnError(true);
				
		// Deploy and handle any errors
		AsyncResult asyncResult = metadataConnection.deploy(baos.toByteArray(), deployOptions);
	    int poll = 0;
        long waitTimeMilliSecs = ONE_SECOND;
        StringBuilder deployProgress = new StringBuilder();
        while (!asyncResult.isDone()) {
            Thread.sleep(waitTimeMilliSecs);
            // double the wait time for the next iteration
            waitTimeMilliSecs *= 2;
            if (poll++ > MAX_NUM_POLL_REQUESTS) {
                throw new Exception("Request timed out. If this is a large set " +
                        "of metadata components, check that the time allowed by " +
                        "MAX_NUM_POLL_REQUESTS is sufficient.");
            }
            asyncResult = metadataConnection.checkStatus(
                    new String[] {asyncResult.getId()})[0];
            deployProgress.append("Status is: " + asyncResult.getState() + "</br>");
        }
        map.put("deployProgress", deployProgress);
        
        String resultErrorMessages = "";
        if (asyncResult.getState() != AsyncRequestState.Completed) {
        	resultErrorMessages = " msg: " + asyncResult.getMessage();
        }
        else {
	        DeployResult result = metadataConnection.checkDeployStatus(asyncResult.getId());
	        if (!result.isSuccess()) {
	        	resultErrorMessages = printErrors(result);
	        }
        }
        map.put("resultErrorMessages", resultErrorMessages);
        
    	return "githubdeploy";
    }

    public static class ContentsServiceEx extends ContentsService
    {
		public ContentsServiceEx(GitHubClient client) {
			super(client);
		}
    
		public ZipInputStream getArchiveAsZip(IRepositoryIdProvider repository)
			throws Exception
		{
			String id = getId(repository);
			StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
			uri.append('/').append(id);
			uri.append('/').append("zipball");
			GitHubRequest request = createRequest();
			request.setUri(uri);			
			return new ZipInputStream(getClient().getStream(request));
		}
    }
    
    /**
     * Adds support for OAuth Client ID and Client Secret authentication (server to server)
     * 
     * Note: Only overrides 'get' and 'getStream'
     * 
     * @author afawcett
     */
    public static class GitHubClientOAuthServer extends GitHubClient
    {
    	private String clientId;
    	private String clientSecret;
    	
    	public GitHubClientOAuthServer(String clientId, String clientSecret)
    	{
    		this.clientId = clientId;
    		this.clientSecret = clientSecret;
    	}
    	
    	public InputStream getStream(final GitHubRequest request) throws IOException 
    	{
    		return super.getStream(applyClientIdAndSecret(request));
    	}
    	
    	public GitHubResponse get(GitHubRequest request) throws IOException 
    	{
    		return super.get(applyClientIdAndSecret(request));
    	}
    	
    	private GitHubRequest applyClientIdAndSecret(GitHubRequest request)
    	{
    		Map<String, String> params = request.getParams();
    		if(params==null)
    			params = new HashMap<String, String>();
    		params.put("client_id", clientId);
    		params.put("client_secret", clientSecret);
    		request.setParams(params); 
    		return request;
    	}
    }
    
    /**
     * Print out any errors, if any, related to the deploy.
     * @param result - DeployResult
     */
    private static String printErrors(DeployResult result)
    {
        DeployMessage messages[] = result.getMessages();
        StringBuilder buf = new StringBuilder("Failures:\n");
        for (DeployMessage message : messages) {
            if (!message.isSuccess()) {
                String loc = (message.getLineNumber() == 0 ? "" :
                    ("(" + message.getLineNumber() + "," +
                            message.getColumnNumber() + ")"));
                if (loc.length() == 0
                        && !message.getFileName().equals(message.getFullName())) {
                    loc = "(" + message.getFullName() + ")";
                }
                buf.append(message.getFileName() + loc + ":" +
                        message.getProblem()).append('\n');
            }
        }
        RunTestsResult rtr = result.getRunTestResult();
        if (rtr.getFailures() != null) {
            for (RunTestFailure failure : rtr.getFailures()) {
                String n = (failure.getNamespace() == null ? "" :
                    (failure.getNamespace() + ".")) + failure.getName();
                buf.append("Test failure, method: " + n + "." +
                        failure.getMethodName() + " -- " +
                        failure.getMessage() + " stack " +
                        failure.getStackTrace() + "\n\n");
            }
        }
        if (rtr.getCodeCoverageWarnings() != null) {
            for (CodeCoverageWarning ccw : rtr.getCodeCoverageWarnings()) {
                buf.append("Code coverage issue");
                if (ccw.getName() != null) {
                    String n = (ccw.getNamespace() == null ? "" :
                        (ccw.getNamespace() + ".")) + ccw.getName();
                    buf.append(", class: " + n);
                }
                buf.append(" -- " + ccw.getMessage() + "\n");
            }
        }
        
        return buf.toString();
    }        
}
