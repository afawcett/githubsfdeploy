package com.example.controller;

import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.force.sdk.connector.ForceServiceConnector;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.CodeCoverageWarning;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.RunTestFailure;
import com.sforce.soap.metadata.RunTestsResult;

// https://githubsfdeploy.herokuapp.com/sfdc/githubdeploy/financialforcedev/fflib-apex-common

// http://localhost:8080/sfdc/githubdeploy/financialforcedev/fflib-apex-common

@Controller
@RequestMapping("/githubdeploy")
public class GitHubSalesforceDeployController {
    
    @RequestMapping(method = RequestMethod.GET, value = "/{owner}/{repo}")
    public String confirm(@PathVariable("owner") String repoOwner, @PathVariable("repo") String repoName, Map<String, Object> map) throws Exception 
    {
    	try
    	{	    	
    		// Repository name
	    	RepositoryId repoId = RepositoryId.create(repoOwner, repoName);
    		map.put("repositoryName", repoId.generateId());
    		
	    	// Display user info
	    	map.put("userContext", new ForceServiceConnector(ForceServiceConnector.getThreadLocalConnectorConfig()).getConnection().getUserInfo());
	    	
	    	// Display repo info
	    	GitHubClientOAuthServer client = new GitHubClientOAuthServer("c9172f8413daedef3f1f", "b53b2ecb62b7a328a6f6889edf7867426aedf4a2" );
	    	map.put("repo", null);
	    	map.put("githubcontents", null);
	    	RepositoryService service = new RepositoryService(client);
	    	map.put("repo", service.getRepository(repoId));
	    	
	    	// Retrieve repository contents applicable for deploy
	    	RepositoryItem repositoryContainer = new RepositoryItem();
	    	repositoryContainer.repositoryItems = new ArrayList<RepositoryItem>();
	    	ContentsServiceEx contentService = new ContentsServiceEx(client);
	    	processContents(contentService, repoId, contentService.getContents(repoId), repositoryContainer);
	    	ObjectMapper mapper = new ObjectMapper();	    	
	    	map.put("githubcontents", mapper.writeValueAsString(repositoryContainer));
	    	
    	}
    	catch (Exception e)
    	{
    		// Handle error
    		map.put("error", "Failed to retrive GitHub repository details : " + e.toString());
    		e.printStackTrace();
    	}
    	return "githubdeploy";
    }
    
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST, value = "/{owner}/{repo}")
    public String deploy(@PathVariable("owner") String repoOwner, @PathVariable("repo") String repoName, @RequestBody String repoContentsJson) throws Exception 
    {
    	// Connect via oAuth client and secret to get greater request limits
    	GitHubClientOAuthServer client = 
    		new GitHubClientOAuthServer("c9172f8413daedef3f1f", "b53b2ecb62b7a328a6f6889edf7867426aedf4a2" );
   
    	// Repository container (files to deploy)
    	ObjectMapper mapper = new ObjectMapper();
    	RepositoryItem repositoryContainer = (RepositoryItem) mapper.readValue(repoContentsJson, RepositoryItem.class);

    	// Repository description
    	RepositoryService service = new RepositoryService(client);
    	RepositoryId repoId = RepositoryId.create(repoOwner, repoName);
    	Repository repo = service.getRepository(repoId); 
        
        // Content service
        ContentsServiceEx contentService = new ContentsServiceEx(client);
        ZipInputStream zipIS = contentService.getArchiveAsZip(repoId);
        
        // Download Zip and generate Metadata Deploy Zip!
        String salesforceDeployRoot = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zipOS = new ZipOutputStream(baos);
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
				
		// Deploy to Salesforce
		DeployOptions deployOptions = new DeployOptions();
		deployOptions.setSinglePackage(true);
		deployOptions.setPerformRetrieve(false);
		deployOptions.setRollbackOnError(true);
		deployOptions.setRunAllTests(true);
		AsyncResult asyncResult = metadataConnection.deploy(baos.toByteArray(), deployOptions);

		// Given the client the AysncResult to poll for the result of the deploy
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.getSerializationConfig().addMixInAnnotations(AsyncResult.class, AsyncResultMixIn.class);
		return objectMapper.writeValueAsString(asyncResult);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/{owner}/{repo}/checkstatus/{asyncId}")
    public String checkStatus(@PathVariable("asyncId") String asyncId) throws Exception 
    {
    	// Connect to Metadata API, check async status and return to client
        ForceServiceConnector connector = new ForceServiceConnector(ForceServiceConnector.getThreadLocalConnectorConfig());
        MetadataConnection metadataConnection = connector.getMetadataConnection();
        AsyncResult asyncResult =  metadataConnection.checkStatus(new String[] { asyncId })[0];
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.getSerializationConfig().addMixInAnnotations(AsyncResult.class, AsyncResultMixIn.class);    	
		return objectMapper.writeValueAsString(asyncResult);
    }

    @ResponseBody
    @RequestMapping(method = RequestMethod.GET, value = "/{owner}/{repo}/checkdeploy/{asyncId}")
    public String checkDeploy(@PathVariable("asyncId") String asyncId) throws Exception 
    {
    	// Connect to Metadata API, check async status and return to client
        ForceServiceConnector connector = new ForceServiceConnector(ForceServiceConnector.getThreadLocalConnectorConfig());
        MetadataConnection metadataConnection = connector.getMetadataConnection();
        DeployResult deployResult = metadataConnection.checkDeployStatus(asyncId);
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(printErrors(deployResult));
    }

    /**
     * Used with the Jackson JSON library to exclude conflicting getters when serialising AsyncResult 
     *   (see http://wiki.fasterxml.com/JacksonMixInAnnotations)
     */
    public abstract class AsyncResultMixIn
    {
    	@JsonIgnore abstract boolean isCheckOnly();
    	@JsonIgnore abstract boolean isDone();
    }
       
    /**
     * Container to reflect repository structure
     */
    public static class RepositoryItem
    {
    	public RepositoryContents repositoryItem;
    	public ArrayList<RepositoryItem> repositoryItems;
    }
    
    /**
     * Extended GitHub Content Service, adds ability to retrieve the repo archive  
     */
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
     * Discovers the contents of a GitHub repository
     * @param contentService
     * @param repoId
     * @param contents
     * @param repositoryContainer
     * @throws Exception
     */
    private static void processContents(ContentsService contentService, RepositoryId repoId, List<RepositoryContents> contents, RepositoryItem repositoryContainer)
    		throws Exception
	{
    	for(RepositoryContents repo : contents)
    	{
			RepositoryItem repositoryItem = new RepositoryItem();
			repositoryItem.repositoryItem = repo;
			repositoryContainer.repositoryItems.add(repositoryItem);
    		if(repo.getType().equals("dir"))
    		{
    			repositoryItem.repositoryItems = new ArrayList<RepositoryItem>();
    			processContents(contentService, repoId, contentService.getContents(repoId, repo.getPath()), repositoryItem);
    		}
    	}
	}
    
    /**
     * Print out any errors, if any, related to the deploy.
     * @param result - DeployResult
     */
    private static String printErrors(DeployResult result)
    {
        DeployMessage messages[] = result.getMessages();
        StringBuilder buf = new StringBuilder();
        for (DeployMessage message : messages) {
            if (!message.isSuccess()) {
            	if(buf.length()==0)
            		buf = new StringBuilder("\nFailures:\n");
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