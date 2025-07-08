/**
 * Copyright (c) 2012, Andrew Fawcett
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 *   are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *      this list of conditions and the following disclaimer in the documentation
 *      and/or other materials provided with the distribution.
 * - Neither the name of the Andrew Fawcett, inc nor the names of its contributors
 *      may be used to endorse or promote products derived from this software without
 *      specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 *  THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 *  EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 *  OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 *  OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/

package com.andyinthecloud.githubsfdeploy.controller;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.namespace.QName;

import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryContents;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.GitHubResponse;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SEGMENT_REPOS;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.ContentsService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.andyinthecloud.githubsfdeploy.config.GitHubProperties;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.GetUserInfoResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.ws.ConnectorConfig;
import com.sforce.ws.bind.TypeMapper;
import com.sforce.ws.parser.XmlOutputStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;

@Controller
public class GitHubSalesforceDeployController {

	private static final Logger logger = LoggerFactory.getLogger(GitHubSalesforceDeployController.class);
	private static final String GITHUB_TOKEN = "github_token";
	private static final String GITHUB_AUTH_ATTEMPTED = "github_auth_attempted";

	@Autowired
	private GitHubProperties githubProperties;

	@GetMapping("/app/githubdeploy/logout")
	public String logout(HttpSession session,@RequestParam(required=false) final String retUrl)
	{
		session.removeAttribute(GITHUB_TOKEN);
		return retUrl != null ? "redirect:" + retUrl : "redirect:/index.jsp";
	}

	@GetMapping("/app/githubdeploy/authorizegh")
	public String authorize(@RequestParam final  String code, @RequestParam final  String state, HttpSession session) throws Exception
	{
		logger.debug("Received GitHub OAuth callback - Code: {}, State: {}", code, state);
		// Clear the auth attempted flag when we get a new token
		session.removeAttribute(GITHUB_AUTH_ATTEMPTED);
		URI uri = new URI("https", "github.com", "/login/oauth/access_token", null);
		URL url = uri.toURL();
		HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		String urlParameters = "client_id=" + githubProperties.getId() + "&client_secret=" + githubProperties.getSecret()
					 +"&code=" + code;
		logger.debug("Exchanging code for token with parameters: client_id={}, code={}", githubProperties.getId(), code);
		// Send post request
		connection.setDoOutput(true);
		try (DataOutputStream connectionOutputStream = new DataOutputStream(connection.getOutputStream())) {
			connectionOutputStream.writeBytes(urlParameters);
			connectionOutputStream.flush();
		}

		// Read response
		int responseCode = connection.getResponseCode();
		logger.debug("GitHub token exchange response code: {}", responseCode);
		
		StringBuilder gitHubResponse = new StringBuilder();
		try (BufferedReader inputReader = new BufferedReader(
				new InputStreamReader(responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream()))) {
			String inputLine;
			while ((inputLine = inputReader.readLine()) != null) {
				gitHubResponse.append(inputLine);
			}
		}
		
		String responseBody = gitHubResponse.toString();
		logger.debug("Received response from GitHub token exchange: {}", responseBody);
		
		if (responseCode >= 400) {
			logger.error("GitHub token exchange failed with status code: {}", responseCode);
			throw new RuntimeException("GitHub token exchange failed with status code: " + responseCode);
		}
		
		ObjectMapper mapper = new ObjectMapper();
		TokenResult tokenResult = mapper.readValue(responseBody, TokenResult.class);
		if (tokenResult.error != null) {
			logger.error("GitHub token exchange failed - Error: {}, Description: {}", tokenResult.error, tokenResult.error_description);
			throw new RuntimeException("GitHub token exchange failed: " + tokenResult.error_description);
		}
		
		if (tokenResult.access_token == null || tokenResult.access_token.isEmpty()) {
			logger.error("GitHub token exchange returned null or empty access token");
			throw new RuntimeException("GitHub token exchange returned null or empty access token");
		}
		
		session.setAttribute(GITHUB_TOKEN, tokenResult.access_token);
		logger.debug("Successfully stored GitHub token in session");
		String redirectUrl = state;
		logger.debug("Redirecting to: {}", redirectUrl);
		return "redirect:" + redirectUrl;
	}
	
	@GetMapping("/app/githubdeploy/{owner}/{repo}")
	public String confirm(HttpServletRequest request,
			@PathVariable("owner") String repoOwner, 
			@PathVariable("repo") String repoName, 
			@RequestParam(defaultValue="master", required=false) String ref,			
			@AuthenticationPrincipal OAuth2User user,
			HttpSession session ,Map<String, Object> map) throws Exception
	{
		logger.debug("Entering confirm page handler");
		logger.debug("Session ID: {}", session.getId());
		logger.debug("Authentication Principal: {}", user != null ? "Present" : "Null");
		logger.debug("Request URL: {}", request.getRequestURL());
		logger.debug("Request Query String: {}", request.getQueryString());
		
		try
		{
			map.put("repo", null);
			map.put("githubcontents", null);
			String accessToken = (String)session.getAttribute(GITHUB_TOKEN);
			Boolean authAttempted = (Boolean)session.getAttribute(GITHUB_AUTH_ATTEMPTED);
			logger.debug("GitHub Token in session: {}", accessToken != null ? "Present" : "Null");
			logger.debug("GitHub Auth Attempted: {}", authAttempted != null && authAttempted ? "Yes" : "No");
			// Repository name
			RepositoryId repoId = RepositoryId.create(repoOwner, repoName);
			map.put("repositoryName", repoId.generateId());
			map.put("ref", ref);

			// Get Salesforce access token and instance URL from attributes
			Map<String, Object> attributes = user != null ? user.getAttributes() : new HashMap<>();
			logger.debug("User Attributes: {}", attributes);
			String salesforceAccessToken = (String) attributes.get("access_token");
			String instanceUrl = (String) attributes.get("instance_url");
			Object urlsObj = attributes.get("urls");
			Map<String, String> urls = null;
			if (urlsObj instanceof Map) {
				@SuppressWarnings("unchecked")
				Map<String, String> urlsMap = (Map<String, String>) urlsObj;
				urls = urlsMap;
			}
			String metadataServerUrl = urls != null ? urls.get("metadata") : null;
			String partnerServerUrl = urls != null ? urls.get("partner") : null;
			if (metadataServerUrl != null) {
				metadataServerUrl = metadataServerUrl.replace("{version}", "61");
			} else {
				logger.error("Metadata server URL is null");
				throw new IllegalStateException("Missing required OAuth attributes");
			}
		
			if (salesforceAccessToken == null || instanceUrl == null || metadataServerUrl == null || partnerServerUrl == null) {
				logger.error("Missing required OAuth attributes: access_token={}, instance_url={}, metadataServerUrl={}, partnerServerUrl={}", 
					salesforceAccessToken != null, instanceUrl != null, metadataServerUrl != null, partnerServerUrl != null);
				throw new IllegalStateException("Missing required OAuth attributes");
			}

			// Get organization name from Partner API
			ConnectorConfig partnerAPIConfig = new ConnectorConfig();
			partnerAPIConfig.setSessionId(salesforceAccessToken);
			partnerAPIConfig.setServiceEndpoint(partnerServerUrl);
			PartnerConnection partnerConnection = Connector.newConnection(partnerAPIConfig);
			GetUserInfoResult userInfo = partnerConnection.getUserInfo();
			map.put("organizationName", userInfo.getOrganizationName());
			map.put("userName", userInfo.getUserName());

			// Display repo info
			GitHubClient client;
			if(accessToken == null)
			{
				logger.debug("Creating GitHub client with OAuth server credentials - Client ID: {}", githubProperties.getId());
				client = new GitHubClientOAuthServer(githubProperties.getId(), githubProperties.getSecret());
			}
			else
			{
				logger.debug("Creating GitHub client with user access token");
				client = new GitHubClient();
				client.setOAuth2Token(accessToken);
				map.put("githuburl","https://github.com/settings/connections/applications/" + githubProperties.getId());
			}

			RepositoryService service = new RepositoryService(client);
			try
			{
			  map.put("repo", service.getRepository(repoId));
			}
			catch(RequestException e)
			{
				String clientId = githubProperties.getId();
				logger.debug("GitHub request failed - Status: {}, Client ID present: {}, Auth attempted: {}", 
					e.getStatus(), 
					!clientId.isEmpty(), 
					authAttempted != null && authAttempted);
					
				if(accessToken == null && !clientId.isEmpty() && (e.getStatus() == 401 || e.getStatus() == 403 || e.getStatus() == 404) && (authAttempted == null || !authAttempted)) {
					session.setAttribute(GITHUB_AUTH_ATTEMPTED, true);
					StringBuffer requestURL = request.getRequestURL();
					String queryString = request.getQueryString();
					String redirectUrl = queryString == null ? requestURL.toString() : requestURL.append('?').append(queryString).toString();
					String oauthUrl = "https://github.com/login/oauth/authorize?client_id=" + githubProperties.getId() + "&scope=repo&state=" + redirectUrl;
					logger.debug("Redirecting to GitHub OAuth URL: {}", oauthUrl);
					logger.debug("Request URL: {}", requestURL);
					logger.debug("Query String: {}", queryString);
					logger.debug("Redirect URL: {}", redirectUrl);
					return "redirect:" + oauthUrl;					
				}
				else {
					if (e.getStatus() == 404) {
						map.put("error", "Could not find the repository '" + repoName + "'. Ensure it is spelt correctly and that it is owned by '" + repoOwner + "'");
					} else {
						map.put("error", "Failed to retrieve GitHub repository details: " + e.getMessage() + " (Status: " + e.getStatus() + ")");
						if (e.getStatus() == 401 || e.getStatus() == 403) {
							session.removeAttribute(GITHUB_TOKEN);
							session.removeAttribute(GITHUB_AUTH_ATTEMPTED);
						}
					}
				}
			}
			catch(IOException e)
			{
				logger.error("IO error while accessing GitHub repository", e);
				map.put("error", "IO error while accessing GitHub repository: " + e.getMessage());
			}
			catch(RuntimeException e)
			{
				logger.error("Unexpected error while accessing GitHub repository", e);
				map.put("error", "Unexpected error while accessing GitHub repository: " + e.getMessage());
			}

			// Create MetadataConnection for repository scan
			ConnectorConfig metadataAPIConfig = new ConnectorConfig();
			//metadataAPIConfig.setAuthEndpoint(instanceUrl + "/services/Soap/u");
			metadataAPIConfig.setSessionId(salesforceAccessToken);
			metadataAPIConfig.setServiceEndpoint(metadataServerUrl);
			MetadataConnection metadataConnection = new MetadataConnection(metadataAPIConfig);
			
			// Prepare Salesforce metadata metadata for repository scan
			RepositoryScanResult repositoryScanResult = new RepositoryScanResult();
			RepositoryItem repositoryContainer = new RepositoryItem();
			repositoryContainer.repositoryItems = new ArrayList<>();
			repositoryScanResult.metadataDescribeBySuffix = new HashMap<>();
			repositoryScanResult.metadataDescribeByFolder = new HashMap<>();

			// Get metadata describe result using the existing connection
			DescribeMetadataResult metadataDescribeResult = metadataConnection.describeMetadata(61.0); // Match WSC version
			for(DescribeMetadataObject describeObject : metadataDescribeResult.getMetadataObjects())
			{
				if(describeObject.getSuffix()==null) {
					repositoryScanResult.metadataDescribeByFolder.put(describeObject.getDirectoryName(), describeObject);
				} else {
					repositoryScanResult.metadataDescribeBySuffix.put(describeObject.getSuffix(), describeObject);
					if(describeObject.getMetaFile())
						repositoryScanResult.metadataDescribeBySuffix.put(describeObject.getSuffix() + "-meta.xml", describeObject);					
				}
			}

			// Retrieve repository contents applicable for deploy
			ContentsServiceEx contentService = new ContentsServiceEx(client);

			try
			{
				scanRepository(
					contentService,
					repoId,
					ref,
					contentService.getContents(repoId, null, ref),
					repositoryContainer,
					repositoryScanResult
				);

				// Determine correct root to emit to the page
				RepositoryItem githubcontents = null;
				if(repositoryScanResult.pacakgeRepoDirectory!=null) {
					githubcontents = repositoryScanResult.pacakgeRepoDirectory;
				} else if(!repositoryContainer.repositoryItems.isEmpty()) {
					githubcontents = repositoryContainer;
				}
				
				// Serialize JSON to page
				if(githubcontents!=null) {
					githubcontents.ref = ref; // Remember branch/tag/commit reference
					map.put("githubcontents", new ObjectMapper().writeValueAsString(githubcontents));
				} else {
					map.put("error", "No Salesforce files found in repository.");
				}
			}
			catch (RequestException e)
			{
				if (e.getStatus() == 404) {
					map.put("error", "Could not find the repository '" + repoName + "'. Ensure it is spelt correctly and that it is owned by '" + repoOwner + "'");
				} else {
					map.put("error", "Failed to scan the repository '" + repoName + "'. Callout to Github failed with status code " + e.getStatus());
					session.removeAttribute(GITHUB_TOKEN);
				}				
			}
		}
		catch (AuthenticationException ex)
		{
			session.removeAttribute(GITHUB_TOKEN);
			return "redirect:/error";
		}
		catch (Exception e)
		{
			// Handle error
			map.put("error", "Unhandled Exception : " + e.toString());
			logger.error("Unhandled exception occurred", e);
		}
		return "githubdeploy";
	}
	
	@PostMapping("/app/githubdeploy/{owner}/{repo}")
	@ResponseBody
	public String deploy(
			@PathVariable("owner") String repoOwner, 
			@PathVariable("repo") String repoName,
			@RequestBody String repoContentsJson,
			HttpServletResponse response,
			Map<String,Object> map,
			HttpSession session,
			@AuthenticationPrincipal OAuth2User user) throws Exception
	{
		String accessToken = (String)session.getAttribute(GITHUB_TOKEN);

		GitHubClient client;

		if(accessToken == null)
		{
			// Connect via oAuth client and secret to get greater request limits
			client = new GitHubClientOAuthServer(githubProperties.getId(), githubProperties.getSecret());
		}
		else
		{
			// Connect with access token to deploy private repositories
			client = new GitHubClient();
			client.setOAuth2Token(accessToken);
		}

		// Repository files to deploy
		ObjectMapper mapper = new ObjectMapper();
		RepositoryItem repositoryContainer = (RepositoryItem) mapper.readValue(repoContentsJson, RepositoryItem.class);

		// Metadata deploy zip file
		byte[] mdDeployZipBytes;

		// Read direct from repo or preconverted deploy zip?
		if(repositoryContainer.downloadId!=null) {
			// Read from deploy zip
			Path deployZipPath = new File(repositoryContainer.downloadId).toPath();
			mdDeployZipBytes = Files.readAllBytes(deployZipPath);
		} else {
			// Performing a package deployment from a package manifest in the repository?
			String repoPackagePath = null;
			RepositoryItem firstFile = repositoryContainer.repositoryItems.get(0);
			if(firstFile.repositoryItem.getName().equals("package.xml"))
				repoPackagePath =
					firstFile.repositoryItem.getPath().substring(0,
							firstFile.repositoryItem.getPath().length() - (firstFile.repositoryItem.getName().length()));

			// Calculate a package manifest?
			String packageManifestXml = null;
			Map<String, RepositoryItem> filesToDeploy = new HashMap<>();
			Map<String, List<String>> typeMembersByType = new HashMap<>();
			if(repoPackagePath==null)
			{
				// Construct package manifest and files to deploy map by path
				Package packageManifest = new Package();
				packageManifest.setVersion("61.0"); // Using latest supported API version
				scanFilesToDeploy(filesToDeploy, typeMembersByType, repositoryContainer);
				PackageTypeMembers[] packageTypes = new PackageTypeMembers[typeMembersByType.size()];
				int i = 0;
				for(String metadataType : typeMembersByType.keySet())
				{
					PackageTypeMembers packageTypeMembers = new PackageTypeMembers();
					packageTypeMembers.setName(metadataType);
					List<String> members = typeMembersByType.get(metadataType);
					String[] memberArray = new String[members.size()];
					members.toArray(memberArray);
					packageTypeMembers.setMembers(memberArray);
					packageTypes[i++] = packageTypeMembers;
				}
				packageManifest.setTypes(packageTypes);
				// Serialise it (better way to do this?)
				TypeMapper typeMapper = new TypeMapper();
				try (ByteArrayOutputStream packageBaos = new ByteArrayOutputStream();
					 XmlOutputStream xmlOutputStream = new XmlOutputStream(packageBaos, true)) {
					QName packageQName = new QName("http://soap.sforce.com/2006/04/metadata", "Package");
					xmlOutputStream.setPrefix("", "http://soap.sforce.com/2006/04/metadata");
					xmlOutputStream.setPrefix("xsi", "http://www.w3.org/2001/XMLSchema-instance");
					packageManifest.write(packageQName, xmlOutputStream, typeMapper);
					xmlOutputStream.close();
					packageBaos.close();
					packageManifestXml = new String(packageBaos.toByteArray());
				}
			}

			// Download the Repository as an archive zip
			RepositoryId repoId = RepositoryId.create(repoOwner, repoName);
			ContentsServiceEx contentService = new ContentsServiceEx(client);
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (ZipOutputStream zipOS = new ZipOutputStream(baos)) {
				logger.info("Starting zip file creation for classic repo deployment");
				// Add package.xml first at root level
				String normalizedPath = null;
				if(packageManifestXml!=null) {
					// Use dynamically generated package manifest
					zipOS.putNextEntry(new ZipEntry("package.xml"));
					zipOS.write(packageManifestXml.getBytes());
					zipOS.closeEntry();
					logger.info("Added dynamically generated package.xml to zip file ({} bytes)", packageManifestXml.getBytes().length);
				} else if(repoPackagePath!=null) {
					// Normalize the path to avoid double slashes
					normalizedPath = repoPackagePath.replaceAll("//+", "/");
					if(normalizedPath.endsWith("/")) {
						normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
					}
					logger.info("Looking for existing package.xml in repo at path: {}", normalizedPath);
				}

				// Read the zip entries, output to the metadata deploy zip files selected
				try (ZipInputStream zipIS = contentService.getArchiveAsZip(repoId, repositoryContainer.ref)) {
					ZipEntry zipEntry;
					int totalFiles = 0;
					boolean foundPackageXml = false;
					while ((zipEntry = zipIS.getNextEntry()) != null) {
						// Determine the repository relative path (zip file contains an archive folder in root)
						String zipPath = zipEntry.getName();
						zipPath = zipPath.substring(zipPath.indexOf("/")+1);
						
						// Handle package.xml if needed
						if(!foundPackageXml && repoPackagePath != null && zipPath.equals(normalizedPath + "/package.xml")) {
							zipOS.putNextEntry(new ZipEntry("package.xml"));
							byte[] buffer = new byte[1024];
							int length;
							int totalBytes = 0;
							while((length = zipIS.read(buffer)) > 0) {
								zipOS.write(buffer, 0, length);
								totalBytes += length;
							}
							zipOS.closeEntry();
							logger.info("Added existing package.xml to zip file ({} bytes)", totalBytes);
							foundPackageXml = true;
							continue;
						}
						
						// Skip package.xml if we've already handled it
						if(zipPath.equals("package.xml")) {
							continue;
						}
						
						// Found a repository file to deploy?
						if(filesToDeploy.containsKey(zipPath))
						{
							// Create metadata file (in correct folder for its type)
							RepositoryItem repoItem = filesToDeploy.get(zipPath);
							String zipName = repoItem.metadataFolder+"/";
							if(repoItem.metadataInFolder)
							{
								String[] folders = repoItem.repositoryItem.getPath().split("/");
								String folderName = folders[folders.length-2];
								zipName+= folderName + "/";
							}
							zipName+= repoItem.repositoryItem.getName();
							logger.debug("Adding metadata file to zip: {}", zipName);
							ZipEntry metadataZipEntry = new ZipEntry(zipName);
							zipOS.putNextEntry(metadataZipEntry);
							// Copy bytes over from Github archive input stream to Metadata zip output stream
							byte[] buffer = new byte[1024];
							int length;
							int totalBytes = 0;
							while((length = zipIS.read(buffer)) > 0) {
								zipOS.write(buffer, 0, length);
								totalBytes += length;
							}
							zipOS.closeEntry();
							logger.debug("Added file {} to zip ({} bytes)", zipName, totalBytes);
							
							// Generate meta.xml for ApexClass if needed
							if(repoItem.metadataType.equals("ApexClass") && !filesToDeploy.containsKey(zipPath+"-meta.xml")) {
								String metaXmlName = zipName + "-meta.xml";
								logger.debug("Generating meta.xml for ApexClass: {}", metaXmlName);
								ZipEntry metaXmlEntry = new ZipEntry(metaXmlName);
								zipOS.putNextEntry(metaXmlEntry);
								String metaXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
									"<ApexClass xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n" +
									"    <apiVersion>61.0</apiVersion>\n" +
									"    <status>Active</status>\n" +
									"</ApexClass>";
								zipOS.write(metaXml.getBytes());
								zipOS.closeEntry();
								logger.debug("Added meta.xml file {} to zip ({} bytes)", metaXmlName, metaXml.getBytes().length);
							}
							totalFiles++;
						}
						// Found a package directory to deploy?
						else if(repoPackagePath!=null && zipPath.equals(repoPackagePath))
						{
							while(true)
							{
								// More package files to zip or dropped out of the package folder?
								zipEntry = zipIS.getNextEntry();
								if(zipEntry==null)
									break;
								zipPath = zipEntry.getName();
								zipPath = zipPath.substring(zipPath.indexOf("/")+1);
								logger.info("zipPath: {}", zipPath);
								if(!zipPath.startsWith(repoPackagePath))
									break;
								// Generate the Metadata zip entry name
								String metadataZipEntryName = zipPath.substring(repoPackagePath.length());
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
					if (!foundPackageXml && repoPackagePath != null) {
						logger.error("Could not find package.xml at path: {}", normalizedPath + "/package.xml");
					}
					logger.info("Added {} metadata files to zip", totalFiles);
				}
				mdDeployZipBytes = baos.toByteArray();
				logger.info("Created zip file with total size: {} bytes", mdDeployZipBytes.length);
			}
		}

		// Get Salesforce access token from OAuth2 user
		String salesforceAccessToken = (String) user.getAttributes().get("access_token");
		String instanceUrl = (String) user.getAttributes().get("instance_url");

		// Create MetadataConnection using OAuth2 token
		ConnectorConfig config = new ConnectorConfig();
		config.setAuthEndpoint(instanceUrl + "/services/oauth2/token");
		config.setServiceEndpoint(instanceUrl + "/services/Soap/m/41.0");
		config.setSessionId(salesforceAccessToken);
		MetadataConnection metadataConnection = new MetadataConnection(config);

		// Deploy to Salesforce
		DeployOptions deployOptions = new DeployOptions();
		deployOptions.setSinglePackage((repositoryContainer.downloadId == null));
		deployOptions.setPerformRetrieve(false);
		deployOptions.setRollbackOnError(true);
		AsyncResult asyncResult = metadataConnection.deploy(mdDeployZipBytes, deployOptions);

		// Given the client the AysncResult to poll for the result of the deploy
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.addMixIn(AsyncResult.class, AsyncResultMixIn.class);
		return objectMapper.writeValueAsString(asyncResult);
	}
	
	@GetMapping("/app/githubdeploy/{owner}/{repo}/checkstatus/{asyncId}")
	@ResponseBody
	public String checkStatus(
			@PathVariable String asyncId,
			@AuthenticationPrincipal OAuth2User user) throws Exception
	{
		// Get Salesforce access token from OAuth2 user
		String salesforceAccessToken = (String) user.getAttributes().get("access_token");
		String instanceUrl = (String) user.getAttributes().get("instance_url");

		// Create MetadataConnection using OAuth2 token
		ConnectorConfig config = new ConnectorConfig();
		config.setAuthEndpoint(instanceUrl + "/services/oauth2/token");
		config.setServiceEndpoint(instanceUrl + "/services/Soap/m/41.0");
		config.setSessionId(salesforceAccessToken);
		MetadataConnection metadataConnection = new MetadataConnection(config);

		// Check async status and return to client
		DeployResult deployResult = metadataConnection.checkDeployStatus(asyncId, true);
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(deployResult);
	}
	
	@GetMapping("/app/githubdeploy/{owner}/{repo}/checkdeploy/{asyncId}")
	@ResponseBody
	public String checkDeploy(
			@PathVariable String asyncId,
			@AuthenticationPrincipal OAuth2User user) throws Exception
	{
		// Get Salesforce access token from OAuth2 user
		String salesforceAccessToken = (String) user.getAttributes().get("access_token");
		String instanceUrl = (String) user.getAttributes().get("instance_url");

		// Create MetadataConnection using OAuth2 token
		ConnectorConfig config = new ConnectorConfig();
		config.setAuthEndpoint(instanceUrl + "/services/oauth2/token");
		config.setServiceEndpoint(instanceUrl + "/services/Soap/m/41.0");
		config.setSessionId(salesforceAccessToken);
		MetadataConnection metadataConnection = new MetadataConnection(config);

		// Check async status and return to client
		DeployResult deployResult = metadataConnection.checkDeployStatus(asyncId, true);
		ObjectMapper objectMapper = new ObjectMapper();
		return objectMapper.writeValueAsString(printErrors(deployResult));
	}

	/**
	 * Used with the Jackson JSON library to exclude conflicting getters when serialising AsyncResult
	 *   (see http://wiki.fasterxml.com/JacksonMixInAnnotations)
	 */
	public abstract class AsyncResultMixIn
	{
		@JsonIgnore 
		public abstract boolean isCheckOnly();
		
		@JsonIgnore 
		public abstract boolean isDone();
	}

	/**
	 * Container to reflect repository structure
	 */
	public static class RepositoryItem
	{
		public String downloadId;
		public String ref;
		public RepositoryContents repositoryItem;
		public ArrayList<RepositoryItem> repositoryItems = new ArrayList<>();
		public String metadataFolder;
		public String metadataType;
		public Boolean metadataFile;
		public Boolean metadataInFolder;
		public String metadataSuffix;
	}

	public static class RepositoryScanResult
	{
		public String packageRepoPath;
		public RepositoryItem pacakgeRepoDirectory;
		public HashMap<String, DescribeMetadataObject> metadataDescribeBySuffix = new HashMap<>();
		public HashMap<String, DescribeMetadataObject> metadataDescribeByFolder = new HashMap<>();
	}

	public static class TokenResult
	{
		public String access_token;
		public String scope;
		public String token_type;
		public String error;
		public String error_description;
		public String error_uri;
	}

	public static class GitSubModule {
		public String path;
		public String url;
		public String branch;
	}

	/**
	 * Extended GitHub Content Service, adds ability to retrieve the repo archive
	 */
	public static class ContentsServiceEx extends ContentsService
	{
		public ContentsServiceEx(GitHubClient client) {
			super(client);
		}

		public ZipInputStream getArchiveAsZip(IRepositoryIdProvider repository, String ref)
			throws Exception
		{
			// https://developer.github.com/v3/repos/contents/#get-archive-link
			String id = getId(repository);
			StringBuilder uri = new StringBuilder(SEGMENT_REPOS);
			uri.append('/').append(id);
			uri.append('/').append("zipball");
			if(ref!=null) {
				uri.append('/').append(ref);
			}
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
		private final String clientId;
		private final String clientSecret;

		public GitHubClientOAuthServer(String clientId, String clientSecret)
		{
			this.clientId = clientId;
			this.clientSecret = clientSecret;
		}

		@Override
		public InputStream getStream(final GitHubRequest request) throws IOException
		{
			return super.getStream(applyClientIdAndSecret(request));
		}

		@Override
		public GitHubResponse get(GitHubRequest request) throws IOException
		{
			return super.get(applyClientIdAndSecret(request));
		}

		private GitHubRequest applyClientIdAndSecret(GitHubRequest request)
		{
			Map<String, String> params = new HashMap<>();
			params.put("client_id", clientId);
			params.put("client_secret", clientSecret);
			if (request.getParams() != null) {
				params.putAll(request.getParams());
			}
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
	private static void scanRepository(ContentsServiceEx contentService, RepositoryId repoId, String ref, List<RepositoryContents> contents, RepositoryItem repositoryContainer, RepositoryScanResult repositoryScanResult)
			throws Exception
	{
		// Process files first
		Boolean convertedDXProject = false;
		List<GitSubModule> subModules = new ArrayList<>();
		for(RepositoryContents repo : contents)
		{
			// Skip directories for now, see below
			if(repo.getType().equals("dir"))
				continue;
			// Skip README.md (suffix overlaps with Custom Metadata!)
			if(repo.getName().equalsIgnoreCase("readme.md"))
				continue;
			if (repo.getName().equalsIgnoreCase(".gitmodules")) {
				// get the contents of the file so we can import the submodule
				// ONLY SUPPORTS SUBMODULES HOST ON GITHUB
				List<RepositoryContents> submodule = contentService.getContents(repoId, repo.getPath(), ref);

				// strip the newline characters out of the encoded content
				byte[] decoded = Base64.getDecoder().decode(
						submodule.get(0).getContent().replace("\n", ""));
				String decodedStr = new String(decoded, StandardCharsets.UTF_8);
				// process the .gitmodules file contents, line by line
				// this is a very naive parser implementation
				// will probably have a variety of bugs, maybe find a lib?
				GitSubModule subMod = new GitSubModule();
				for (String line : decodedStr.split("\n")) {
					// start of named submodule
					if (line.trim().startsWith("[")) {
						subMod = new GitSubModule();
						subModules.add(subMod);
					}
					// capture properties as we see them
					if (line.trim().startsWith("path")) {
						subMod.path = line.split("=", 2)[1].trim();
					}
					if (line.trim().startsWith("url")) {
						subMod.url = line.split("=", 2)[1].trim();
					}
					if (line.trim().startsWith("branch")) {
						subMod.branch = line.split("=", 2)[1].trim();
						if(".".equals(subMod.branch)){
							// https://git-scm.com/docs/git-submodule#_options
							// if branch value of "." is used, it should fallback to the
							// source repos branch name
							subMod.branch = ref;
						}
					}
				}

			}
			// Found a Salesforce DX sfdx-project.json?
			if(repo.getName().equals("sfdx-project.json")) 
			{
				// Not interested in files scanned thus far
				repositoryContainer.repositoryItems.clear();
				// Download contents to temp dir
				Path tempDir = Files.createTempDirectory(null);
				downloadRepoToPath(tempDir, contentService, repoId, ref);
				for (GitSubModule subMod : subModules) {
					String urlVal = subMod.url;
					// handling for the following module URL forms
					// that might show up in the .gitmodules declaration
					/*
						https://github.com/owner/repo
						https://github.com/owner/repo.gIt
						git@github.com:owner/repo
						git@github.com:owner/repo.git
						../../owner/repo.git
						../repo.git
					 */
					if(urlVal.toLowerCase().endsWith(".git")){
						urlVal = urlVal.substring(0, urlVal.length() -4);
					}
					String[] parts = urlVal.split("/|:");
					String ownerName = parts[parts.length-2];
					if("..".equals(ownerName)){
						// owner name is relative to the current package
						ownerName = repoId.getOwner();
					}
					String repoName = parts[parts.length-1];
					RepositoryId subRepoId = RepositoryId.create(ownerName, repoName);
					downloadRepoToPath(tempDir.resolve(subMod.path+"/"), contentService, subRepoId, subMod.branch);
				}
				// Convert to MD API Format using SFDX CLI
				ProcessBuilder processBuilder = new ProcessBuilder("sfdx", "force:source:convert", "--outputdir", "deploy");
				processBuilder.directory(tempDir.toFile());
				Process process = processBuilder.start();
				StringBuilder output = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
					 BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					String line;
					while ((line = reader.readLine()) != null) {
						output.append(line).append("\n");
					}
					while ((line = stdError.readLine()) != null) {
						output.append(line).append("\n");
					}
				}
				int exitVal = process.waitFor();
				if (exitVal != 0) {
					throw new RuntimeException("SFDX conversion failed: " + output.toString());
				}
				logger.info("SFDX conversion successful: {}", output.toString());
				// Zip up the deploy folder
				Path zipFiilePath = tempDir.resolve("deploy.zip");
				Path zipFileSourcePath = tempDir.resolve(("deploy"));
				ZipParameters params = new ZipParameters();
				params.setReadHiddenFiles(false);
				params.setReadHiddenFolders(false);
				try (net.lingala.zip4j.ZipFile zipDeploy = new net.lingala.zip4j.ZipFile(zipFiilePath.toFile())) {
					zipDeploy.addFolder(zipFileSourcePath.toFile(), params);
					List<FileHeader> fileHeaders = zipDeploy.getFileHeaders();
					for(FileHeader fileHeader : fileHeaders) {
						if(fileHeader.isDirectory()) {
							continue;
						}
						// RepositoryItem here is really just used to confirm what will be deployed (its not the repo contents)
						RepositoryItem repositoryItem = new RepositoryItem();
						repositoryItem.repositoryItem = new RepositoryContents();
						repositoryItem.repositoryItem.setPath(fileHeader.getFileName().replace("deploy/", ""));
						repositoryContainer.repositoryItems.add(repositoryItem);						
					}
				}
				// Retain zip location for deploy request 
				repositoryContainer.downloadId = zipFiilePath.toString();
				convertedDXProject = true;
				break;
			}
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
				if(extensionPosition != -1)
					fileExtension = repo.getName().substring(extensionPosition + 1);
			}
			// Is this file extension recognised by Salesforce Metadata API?
			DescribeMetadataObject metadataObject = repositoryScanResult.metadataDescribeBySuffix.get(fileExtension);
			if(metadataObject==null)
			{
				// Is this a file within a sub-directory of a metadata folder? 
				//   e.g. src/documents/Eventbrite/Eventbrite_Sync_Logo.png
				String[] folders = repo.getPath().split("/");
				if(folders.length>3)
				{
					// Metadata describe for containing folder?
					metadataObject = repositoryScanResult.metadataDescribeByFolder.get(folders[folders.length-3]);
					if(metadataObject==null)
						continue;
				}
				// Is this a metadata file for a sub-folder within the root of a metadata folder? 
				//   (such as the XML metadata file for a folder in documents) 
				//   e.g.  src/documents/Eventbrite
				//         src/documents/Eventbrite-meta.xml <<<<
				else if(folders.length>2)
				{
					// Metadata describe for metadata folder?
					metadataObject = repositoryScanResult.metadataDescribeByFolder.get(folders[folders.length-2]);
					if(metadataObject==null)
						continue;
					// If package.xml is to be generated for this repo, ensure folders are added to the package items 
					//   via special value in suffix, see scanFilesToDeploy method
					metadataObject.setSuffix("dir");
				}
				else
					continue;
			}
			// Add file
			RepositoryItem repositoryItem = new RepositoryItem();
			repositoryItem.repositoryItem = repo;
			repositoryItem.metadataFolder = metadataObject.getDirectoryName();
			repositoryItem.metadataType = metadataObject.getXmlName();
			repositoryItem.metadataFile = metadataObject.getMetaFile();
			repositoryItem.metadataInFolder = metadataObject.getInFolder();
			repositoryItem.metadataSuffix = metadataObject.getSuffix();
			repositoryContainer.repositoryItems.add(repositoryItem);
		}
		// Process directories if still figuring out the contents of an none DX formatted repo
		if(!convertedDXProject) {
			for(RepositoryContents repo : contents)
			{
				if(repo.getType().equals("dir"))
				{
					RepositoryItem repositoryItem = new RepositoryItem();
					repositoryItem.repositoryItem = repo;
					repositoryItem.repositoryItems = new ArrayList<>();
					scanRepository(contentService, repoId, ref, contentService.getContents(repoId, repo.getPath().replace(" ", "%20"), ref), repositoryItem, repositoryScanResult);
					if(repositoryScanResult.packageRepoPath!=null && repo.getPath().equals(repositoryScanResult.packageRepoPath))
						repositoryScanResult.pacakgeRepoDirectory = repositoryItem;
					if(!repositoryItem.repositoryItems.isEmpty())
						repositoryContainer.repositoryItems.add(repositoryItem);
				}
			}	
		}
	}

	private static void downloadRepoToPath(Path tempDir, ContentsServiceEx contentService, RepositoryId repoId, String ref) throws Exception {
		try (ZipInputStream zipIS = contentService.getArchiveAsZip(repoId, ref)) {
			byte[] buffer = new byte[2048];
			ZipEntry entry;
			while ((entry = zipIS.getNextEntry()) != null) {
				// Remove the repo name folder from the path
				String zipPath = entry.getName();
				zipPath = zipPath.substring(zipPath.indexOf("/")+1);
				// Skip dirs
				if(entry.isDirectory()) {
					continue;
				}
				// Write file
				Path filePath = tempDir.resolve(zipPath);
				File outputFile = filePath.toFile();
				outputFile.getParentFile().mkdirs();
				outputFile.createNewFile();
				try (FileOutputStream fos = new FileOutputStream(outputFile);
					 BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {
					int len;
					while ((len = zipIS.read(buffer)) > 0) {
						bos.write(buffer, 0, len);
					}
				}
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
				// Is this repository file a metadata file?
				Boolean isMetadataFile = repositoryItem.repositoryItem.getName().endsWith(".xml");
				Boolean isMetadataFileForFolder = "dir".equals(repositoryItem.metadataSuffix);
				if(isMetadataFile) // Skip meta files
					if(!isMetadataFileForFolder) // As long as its not a metadata file for a folder
						continue;
				// Add item to list by metadata type for package manifiest generation
				List<String> packageTypeMembers = typeMembersByType.get(repositoryItem.metadataType);
				if(packageTypeMembers==null)
					typeMembersByType.put(repositoryItem.metadataType, (packageTypeMembers = new ArrayList<>()));
				// Determine the component name
				String componentName = repositoryItem.repositoryItem.getName();
				if(componentName.indexOf(".")>0) // Strip file extension?
					componentName = componentName.substring(0, componentName.indexOf("."));
				if(componentName.indexOf("-meta")>0) // Strip any -meta suffix (on the end of folder metadata file names)?
					componentName = componentName.substring(0, componentName.indexOf("-meta"));
				// Qualify the component name by its folder?
				if(repositoryItem.metadataInFolder)
				{
					// Parse the component folder name from the path to the item
					String[] folders = repositoryItem.repositoryItem.getPath().split("/");
					String folderName = folders[folders.length-2];
					componentName = folderName + "/" + componentName;
				}
				packageTypeMembers.add(componentName);
			}
		}
	}

	/**
	 * Print out any errors, if any, related to the deploy.
	 * @param result - DeployResult
	 */
	private static String printErrors(DeployResult result)
	{
		DeployMessage messages[] = result.getDetails().getComponentFailures();
		StringBuilder buf = new StringBuilder();
		for (DeployMessage message : messages) {
			if (!message.isSuccess()) {
				if(buf.length() == 0) {
					buf.append("\nFailures:\n");
				}
				String loc = (message.getLineNumber() == 0 ? "" :
					("(" + message.getLineNumber() + "," +
							message.getColumnNumber() + ")"));
				if (loc.length() == 0
						&& !message.getFileName().equals(message.getFullName())) {
					loc = "(" + message.getFullName() + ")";
				}
				buf.append(message.getFileName()).append(loc).append(":").append(message.getProblem()).append('\n');
			}
		}
		RunTestsResult rtr = result.getDetails().getRunTestResult();
		if (rtr.getFailures() != null) {
			for (RunTestFailure failure : rtr.getFailures()) {
				String n = (failure.getNamespace() == null ? "" :
					(failure.getNamespace() + ".")) + failure.getName();
				buf.append("Test failure, method: ")
					.append(n)
					.append(".")
					.append(failure.getMethodName())
					.append(" -- ")
					.append(failure.getMessage())
					.append(" stack ")
					.append(failure.getStackTrace())
					.append("\n\n");
			}
		}
		if (rtr.getCodeCoverageWarnings() != null) {
			for (CodeCoverageWarning ccw : rtr.getCodeCoverageWarnings()) {
				buf.append("Code coverage issue");
				if (ccw.getName() != null) {
					String n = (ccw.getNamespace() == null ? "" :
						(ccw.getNamespace() + ".")) + ccw.getName();
					buf.append(", class: ").append(n);
				}
				buf.append(" -- ").append(ccw.getMessage()).append("\n");
			}
		}

		return buf.toString();
	}

	@ExceptionHandler({OAuth2AuthenticationException.class, AuthenticationException.class})
	public String handleAuthError(Exception ex, HttpSession session) {
		session.removeAttribute(GITHUB_TOKEN);
		return "redirect:/error";
	}

	@GetMapping("/app/githubdeploy")
	public String landing(@AuthenticationPrincipal OAuth2User user, Model model, HttpSession session) {
		logger.debug("Entering landing page handler");
		logger.debug("Session ID: {}", session.getId());
		logger.debug("Authentication Principal: {}", user != null ? "Present" : "Null");
		
		if (user != null) {
			Map<String, Object> attributes = user.getAttributes();
			logger.debug("User Attributes: {}", attributes);
			logger.debug("User ID: {}", attributes.get("user_id"));
			logger.debug("Username: {}", attributes.get("preferred_username"));
			logger.debug("Organization ID: {}", attributes.get("organization_id"));
			
			model.addAttribute("userId", attributes.get("user_id"));
			model.addAttribute("username", attributes.get("preferred_username"));
			model.addAttribute("email", attributes.get("email"));
			model.addAttribute("name", attributes.get("name"));
		} else {
			logger.debug("No authenticated user found, will redirect to OAuth");
		}
		return "githubdeploy";
	}
}
