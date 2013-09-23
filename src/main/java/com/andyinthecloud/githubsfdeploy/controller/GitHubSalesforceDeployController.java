package com.andyinthecloud.githubsfdeploy.controller;

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

import javax.xml.namespace.QName;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
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
import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Package;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RunTestFailure;
import com.sforce.soap.metadata.RunTestsResult;
import com.sforce.ws.bind.TypeMapper;
import com.sforce.ws.parser.XmlOutputStream;

@Controller
@RequestMapping("/githubdeploy")
public class GitHubSalesforceDeployController {
    
	// Allocated via your GitHub Account Settings, set as environment vars, provides increased limits per hour for GitHub API calls
	private static String GITHUB_CLIENT_ID = "GITHUB_CLIENT_ID";
	private static String GITHUB_CLIENT_SECRET = "GITHUB_CLIENT_SECRET";
		
    @RequestMapping(method = RequestMethod.GET, value = "/{owner}/{repo}")
    public String confirm(@PathVariable("owner") String repoOwner, @PathVariable("repo") String repoName, Map<String, Object> map) throws Exception 
    {
    	try
    	{	    	
    		// Repository name
	    	RepositoryId repoId = RepositoryId.create(repoOwner, repoName);
    		map.put("repositoryName", repoId.generateId());
    		
	    	// Display user info
    		ForceServiceConnector forceConnector = new ForceServiceConnector(ForceServiceConnector.getThreadLocalConnectorConfig());
	    	map.put("userContext", forceConnector.getConnection().getUserInfo());
	    		    	
	    	// Display repo info
	    	GitHubClientOAuthServer client = 
	    		new GitHubClientOAuthServer(System.getenv(GITHUB_CLIENT_ID), System.getenv(GITHUB_CLIENT_SECRET) );
	    	map.put("repo", null);
	    	map.put("githubcontents", null);
	    	RepositoryService service = new RepositoryService(client);
	    	map.put("repo", service.getRepository(repoId));
	    	
	    	// Prepare Salesforce metadata metadata for repository scan
	    	RepositoryScanResult repositoryScanResult = new RepositoryScanResult();
	    	RepositoryItem repositoryContainer = new RepositoryItem();
	    	repositoryContainer.repositoryItems = new ArrayList<RepositoryItem>();
	    	repositoryScanResult.metadataFolderBySuffix = new HashMap<String, DescribeMetadataObject>();
	    	DescribeMetadataResult metadataDescribeResult = forceConnector.getMetadataConnection().describeMetadata(28.0); // TODO: Make version configurable / auto
	    	for(DescribeMetadataObject describeObject : metadataDescribeResult.getMetadataObjects())
	    	{
	    		repositoryScanResult.metadataFolderBySuffix.put(describeObject.getSuffix(), describeObject);
	    		if(describeObject.getMetaFile())
	    			repositoryScanResult.metadataFolderBySuffix.put(describeObject.getSuffix() + "-meta.xml", describeObject);
	    	}
	    	
	    	// Retrieve repository contents applicable for deploy
	    	ContentsServiceEx contentService = new ContentsServiceEx(client);
	    	scanRepository(contentService, repoId, contentService.getContents(repoId), repositoryContainer, repositoryScanResult);	    		    	
	    	ObjectMapper mapper = new ObjectMapper();	    	
	    	if(repositoryScanResult.pacakgeRepoDirectory!=null)
	    		map.put("githubcontents", mapper.writeValueAsString(repositoryScanResult.pacakgeRepoDirectory));
	    	else if(repositoryContainer.repositoryItems.size()>0)
	    		map.put("githubcontents", mapper.writeValueAsString(repositoryContainer));
	    	else
	    		map.put("error", "No Salesforce files found in repository.");
	    	
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
	    		new GitHubClientOAuthServer(System.getenv(GITHUB_CLIENT_ID), System.getenv(GITHUB_CLIENT_SECRET) );
   
    	// Repository files to deploy
    	ObjectMapper mapper = new ObjectMapper();
    	RepositoryItem repositoryContainer = (RepositoryItem) mapper.readValue(repoContentsJson, RepositoryItem.class);
    	
    	// Performing a package deployment from a package manifest in the repository?
        String repoPackagePath = null;
        RepositoryItem firstFile = repositoryContainer.repositoryItems.get(0);
        if(firstFile.repositoryItem.getName().equals("package.xml"))
        	repoPackagePath = 
        		firstFile.repositoryItem.getPath().substring(0, 
        				firstFile.repositoryItem.getPath().length() - (firstFile.repositoryItem.getName().length()));
        
        // Calculate a package manifest?
        String packageManifestXml = null;
        Map<String, RepositoryItem> filesToDeploy = new HashMap<String, RepositoryItem>();
        Map<String, List<String>> typeMembersByType = new HashMap<String, List<String>>(); 
        if(repoPackagePath==null)
        {
        	// Construct package manifest and files to deploy map by path
        	Package packageManifest = new Package();    	
        	packageManifest.setVersion("28.0"); // TODO: Make version configurable / auto
        	List<PackageTypeMembers> packageTypeMembersList = new ArrayList<PackageTypeMembers>(); 
        	scanFilesToDeploy(filesToDeploy, typeMembersByType, repositoryContainer);
	    	for(String metadataType : typeMembersByType.keySet())
	    	{
	    		PackageTypeMembers packageTypeMembers = new PackageTypeMembers();
	    		packageTypeMembers.setName(metadataType);
	    		packageTypeMembers.setMembers((String[])typeMembersByType.get(metadataType).toArray(new String[0]));
	    		packageTypeMembersList.add(packageTypeMembers);
	    	}
	    	packageManifest.setTypes((PackageTypeMembers[]) packageTypeMembersList.toArray(new PackageTypeMembers[0]));	    	
	    	// Serialise it (better way to do this?)
	        TypeMapper typeMapper = new TypeMapper();
	        ByteArrayOutputStream packageBaos = new ByteArrayOutputStream();
	        QName packageQName = new QName("http://soap.sforce.com/2006/04/metadata", "Package");
	        XmlOutputStream xmlOutputStream = new XmlOutputStream(packageBaos, true);
	        xmlOutputStream.setPrefix("", "http://soap.sforce.com/2006/04/metadata");
	        xmlOutputStream.setPrefix("xsi", "http://www.w3.org/2001/XMLSchema-instance");
	        packageManifest.write(packageQName, xmlOutputStream, typeMapper);
	        xmlOutputStream.close();
	        packageManifestXml = new String(packageBaos.toByteArray());
        }
        
        // Download the Repository as an archive zip
    	RepositoryId repoId = RepositoryId.create(repoOwner, repoName);
        ContentsServiceEx contentService = new ContentsServiceEx(client);
        ZipInputStream zipIS = contentService.getArchiveAsZip(repoId);
        
		// Dynamically generated package manifest?
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ZipOutputStream zipOS = new ZipOutputStream(baos);
		if(packageManifestXml!=null)
		{
			ZipEntry metadataZipEntry = new ZipEntry("package.xml");
			zipOS.putNextEntry(metadataZipEntry);
			zipOS.write(packageManifestXml.getBytes());
			zipOS.closeEntry();
		}
        // Read the zip entries, output to the metadata deploy zip files selected
		while(true)
		{
			ZipEntry zipEntry = zipIS.getNextEntry();
			if(zipEntry==null)
				break;
			// Determine the repository relative path (zip file contains an archive folder in root)
			String zipPath = zipEntry.getName();
			String repoPath = zipPath.substring(zipPath.indexOf("/") + 1);
			// Found a repository file to deploy?
			if(filesToDeploy.containsKey(repoPath))
			{
				// Create metadata file (in correct folder for its type)
				RepositoryItem repoItem = filesToDeploy.get(repoPath);
				ZipEntry metadataZipEntry = new ZipEntry(repoItem.metadataFolder+"/"+repoItem.repositoryItem.getName());
				zipOS.putNextEntry(metadataZipEntry);
				// Copy bytes over from Github archive input stream to Metadata zip output stream
				byte[] buffer = new byte[1024];
				int length = 0;
				while((length = zipIS.read(buffer)) > 0)
					zipOS.write(buffer, 0, length);
				zipOS.closeEntry();
				// Missing metadata file for Apex classes?
				if(repoItem.metadataType.equals("ApexClass") && !filesToDeploy.containsKey(repoPath+"-meta.xml"))
				{
					StringBuilder sb = new StringBuilder();
					sb.append("<ApexClass xmlns=\"http://soap.sforce.com/2006/04/metadata\">");
					sb.append("<apiVersion>27.0</apiVersion>"); // TODO: Make version configurable / auto
					sb.append("<status>Active</status>");
					sb.append("</ApexClass>");
					ZipEntry missingMetadataZipEntry = new ZipEntry(repoItem.metadataFolder+"/"+repoItem.repositoryItem.getName()+"-meta.xml");
					zipOS.putNextEntry(missingMetadataZipEntry);
					zipOS.write(sb.toString().getBytes());
					zipOS.closeEntry();					
				}
			}
			// Found a package directory to deploy?
			else if(repoPackagePath!=null && repoPath.equals(repoPackagePath))
			{				
				while(true)
				{
					// More package files to zip or dropped out of the package folder? 
					zipEntry = zipIS.getNextEntry();
					if(zipEntry==null || !zipEntry.getName().startsWith(zipPath))
						break;
					// Generate the Metadata zip entry name
					String metadataZipEntryName = zipEntry.getName().substring(zipPath.length());					
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
				
		// Connect to Salesforce Metadata API
        ForceServiceConnector connector = new ForceServiceConnector(ForceServiceConnector.getThreadLocalConnectorConfig());
        MetadataConnection metadataConnection = connector.getMetadataConnection();
				
		// Deploy to Salesforce
		DeployOptions deployOptions = new DeployOptions();
		deployOptions.setSinglePackage(true);
		deployOptions.setPerformRetrieve(false);
		deployOptions.setRollbackOnError(true);
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
    	public String metadataFolder;
		public String metadataType;
		public Boolean metadataFile;
    }
    
    public static class RepositoryScanResult
    {
		public String packageRepoPath;
    	public RepositoryItem pacakgeRepoDirectory;
    	public HashMap<String, DescribeMetadataObject> metadataFolderBySuffix;
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
    private static void scanRepository(ContentsService contentService, RepositoryId repoId, List<RepositoryContents> contents, RepositoryItem repositoryContainer, RepositoryScanResult repositoryScanResult)
    		throws Exception
	{
    	// Process files first
    	for(RepositoryContents repo : contents)
    	{
    		// Skip directories for now, see below
    		if(repo.getType().equals("dir"))
    			continue;
    		// Found a Salesforce package manifest?
    		if(repo.getName().equals("package.xml"))
    		{
    			repositoryScanResult.packageRepoPath = repo.getPath().substring(0, repo.getPath().length() - (repo.getName().length() ));
    			if(repositoryScanResult.packageRepoPath.endsWith("/"))
    				repositoryScanResult.packageRepoPath = repositoryScanResult.packageRepoPath.substring(0, repositoryScanResult.packageRepoPath.length() - 1);
    			RepositoryItem repositoryItem = new RepositoryItem();
    			repositoryItem.repositoryItem = repo;
    			repositoryContainer.repositoryItems.add(repositoryItem);   
    			continue;
    		}
    		// Could this be a Salesforce file?
    		int extensionPosition = repo.getName().lastIndexOf(".");
    		if(extensionPosition == -1) // File extension?
    			continue;
    		String fileExtension = repo.getName().substring(extensionPosition+1);
    		String fileNameWithoutExtension = repo.getName().substring(0, extensionPosition); 
    		// Could this be Salesforce metadata file?
    		if(fileExtension.equals("xml"))
    		{
    			// Adjust to look for a Salesforce metadata file extension?
    			extensionPosition = fileNameWithoutExtension.lastIndexOf(".");
    			if(extensionPosition == -1)
    				continue;
    			fileExtension = repo.getName().substring(extensionPosition + 1);
    		}    			
    		if(!repositoryScanResult.metadataFolderBySuffix.containsKey(fileExtension))
    			continue;
    		// Add file
    		DescribeMetadataObject metadataObject = repositoryScanResult.metadataFolderBySuffix.get(fileExtension); 
			RepositoryItem repositoryItem = new RepositoryItem();
			repositoryItem.repositoryItem = repo;
			repositoryItem.metadataFolder = metadataObject.getDirectoryName();
			repositoryItem.metadataType = metadataObject.getXmlName();
			repositoryItem.metadataFile = metadataObject.getMetaFile();
			repositoryContainer.repositoryItems.add(repositoryItem);
    	}    		
    	// Process directories
    	for(RepositoryContents repo : contents)
    	{
    		if(repo.getType().equals("dir"))
    		{
    			RepositoryItem repositoryItem = new RepositoryItem();
    			repositoryItem.repositoryItem = repo;
    			repositoryItem.repositoryItems = new ArrayList<RepositoryItem>();
				repositoryContainer.repositoryItems.add(repositoryItem);    			
    			scanRepository(contentService, repoId, contentService.getContents(repoId, repo.getPath().replace(" ", "%20")), repositoryItem, repositoryScanResult);
    			if(repositoryScanResult.packageRepoPath!=null && repo.getPath().equals(repositoryScanResult.packageRepoPath))
    				repositoryScanResult.pacakgeRepoDirectory = repositoryItem; 
    		}
    	}
	}
    
    /**
     * Scans the files the user selected they want to deploy and maps the paths and metadata types
     * @param filesToDeploy
     * @param typeMembersByType
     * @param repositoryContainer
     */
    private void scanFilesToDeploy(Map<String, RepositoryItem> filesToDeploy, Map<String, List<String>> typeMembersByType, RepositoryItem repositoryContainer)
    {
    	for(RepositoryItem repositoryItem : repositoryContainer.repositoryItems)
    	{
    		if(repositoryItem.repositoryItem.getType().equals("dir"))
    		{
    			// Scan into directory
    			scanFilesToDeploy(filesToDeploy, typeMembersByType, repositoryItem);
    		}
    		else
    		{
    			// Map path to repository item
    			filesToDeploy.put(repositoryItem.repositoryItem.getPath(), repositoryItem);
    			// Skip metadata files
    			if(repositoryItem.repositoryItem.getName().endsWith(".xml"))
    				continue;
    			// Add item to list by metadata type for package manifiest generation
    			List<String> packageTypeMembers = typeMembersByType.get(repositoryItem.metadataType);
    			if(packageTypeMembers==null)
    				typeMembersByType.put(repositoryItem.metadataType, (packageTypeMembers = new ArrayList<String>()));
    			String repoFileName = repositoryItem.repositoryItem.getName();
    			packageTypeMembers.add(repoFileName.substring(0, repoFileName.indexOf(".")));
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