# GitHub Salesforce Deploy Tool - Developer Documentation

## Overview

The GitHub Salesforce Deploy Tool is a web application that enables users to deploy Salesforce metadata directly from GitHub repositories to Salesforce orgs. It provides a user-friendly interface for selecting GitHub repositories and deploying their contents to Salesforce.

## Architecture

The application is built using:
- Spring Boot for the backend
- JSP (JavaServer Pages) for the frontend
- Salesforce Metadata API for deployments
- GitHub API for repository access
- OAuth2 for authentication with both GitHub and Salesforce

## Key Components

### 1. GitHubSalesforceDeployController

The main controller class that handles all deployment operations. Key responsibilities include:

- **Authentication Management**
  - Handles OAuth2 authentication with GitHub
  - Manages Salesforce OAuth2 authentication
  - Maintains session tokens and user state

- **Repository Operations**
  - Scans GitHub repositories for Salesforce metadata
  - Handles both traditional metadata format and SFDX format
  - Processes package.xml files and metadata components

- **Deployment Operations**
  - Creates deployment packages from repository contents
  - Manages asynchronous deployments to Salesforce
  - Tracks deployment status and results
  - Handles error reporting and validation

### 2. githubdeploy.jsp

The main view template that provides the deployment interface. Key features include:

- **Repository Information Display**
  - Shows GitHub repository details
  - Displays branch/tag/commit information
  - Lists files to be deployed

- **Deployment Controls**
  - Provides deploy button and status updates
  - Shows deployment progress
  - Displays error messages and results

- **Organization Information**
  - Shows target Salesforce org details
  - Displays user information
  - Provides GitHub permissions management

## Authentication Flows

### Salesforce Authentication Flow

1. **Initial Authentication**
   ```
   GET / → index.jsp
   ↓
   Login to Salesforce button
   ↓
   Redirect to Salesforce OAuth endpoint
   ↓
   Salesforce redirects back to /app/githubdeploy/{owner}/{repo}
   ↓
   Handled by GitHubSalesforceDeployController.forwardAppPath()
   ↓
   Forward to /githubdeploy/{owner}/{repo}
   ↓
   Handled by GitHubSalesforceDeployController.confirm()
   ↓
   Store tokens in session
   ```

2. **Token Usage in Endpoints**
   - All deployment endpoints use `@AuthenticationPrincipal OAuth2User`
   - Extracts Salesforce tokens from user attributes:
     - `access_token`: For API calls
     - `instance_url`: For API endpoints
     - `urls`: Contains metadata and partner API URLs
   - Key endpoints:
     ```
     GET /githubdeploy/{owner}/{repo} → confirm()
     POST /githubdeploy/{owner}/{repo} → deploy()
     GET /githubdeploy/{owner}/{repo}/checkstatus/{asyncId} → checkStatus()
     GET /githubdeploy/{owner}/{repo}/checkdeploy/{asyncId} → checkDeploy()
     ```

### Combined Authentication and Deployment Flow

1. **Public Repository Deployment**
   ```
   GET / → index.jsp
   ↓
   Login to Salesforce
   ↓
   Redirect to /app/githubdeploy/{owner}/{repo}
   ↓
   Forward to /githubdeploy/{owner}/{repo}
   ↓
   GitHubSalesforceDeployController.confirm():
   - Uses GitHubClientOAuthServer (client ID/secret) for public repo access
   - Uses Salesforce OAuth tokens for deployment
   ↓
   User clicks Deploy button in githubdeploy.jsp
   ↓
   Client-side JavaScript (GitHubDeploy.deploy()):
   - Disables deploy button
   - Shows deployment status div
   - Makes POST request to /githubdeploy/{owner}/{repo}
   ↓
   POST /githubdeploy/{owner}/{repo} → deploy()
   ↓
   Returns AsyncResult with deployment ID
   ↓
   Client-side polling begins:
   - Every 2 seconds calls /githubdeploy/{owner}/{repo}/checkstatus/{asyncId}
   - Updates status display
   - When complete, calls /githubdeploy/{owner}/{repo}/checkdeploy/{asyncId}
   - Shows final results or errors
   ```

2. **Private Repository Deployment**
   ```
   GET / → index.jsp
   ↓
   Login to Salesforce
   ↓
   Redirect to /app/githubdeploy/{owner}/{repo}
   ↓
   Forward to /githubdeploy/{owner}/{repo}
   ↓
   GitHubSalesforceDeployController.confirm():
   - Check session for GITHUB_TOKEN
   - If no token: Redirect to GET /githubdeploy/authorizegh?code={code}&state={state}
   - Handled by GitHubSalesforceDeployController.authorize()
   - Exchange code for token via GitHub OAuth endpoint
   - Store token in session
   ↓
   User clicks Deploy button in githubdeploy.jsp
   ↓
   Client-side JavaScript (GitHubDeploy.deploy()):
   - Disables deploy button
   - Shows deployment status div
   - Makes POST request to /githubdeploy/{owner}/{repo}
   ↓
   POST /githubdeploy/{owner}/{repo} → deploy()
   ↓
   Returns AsyncResult with deployment ID
   ↓
   Client-side polling begins:
   - Every 2 seconds calls /githubdeploy/{owner}/{repo}/checkstatus/{asyncId}
   - Updates status display
   - When complete, calls /githubdeploy/{owner}/{repo}/checkdeploy/{asyncId}
   - Shows final results or errors
   ```

### Client-Side Deployment Flow

1. **Initial Deployment**
   ```javascript
   // In githubdeploy.jsp
   GitHubDeploy.deploy = function() {
       $('#deploy').attr('disabled', 'disabled');
       $('#deploystatus').empty();
       $('#deploystatus').show();
       $('#deploystatus').append('Deployment Started');
       
       $.ajax({
           type: 'POST',
           processData: false,
           data: JSON.stringify(GitHubDeploy.contents),
           contentType: "application/json; charset=utf-8",
           dataType: "json",
           success: function(data) {
               GitHubDeploy.deployResult = data;
               // Start polling for status
               GitHubDeploy.intervalId = window.setInterval(GitHubDeploy.checkStatus, 2000);
           }
       });
   }
   ```

2. **Status Checking**
   ```javascript
   GitHubDeploy.checkStatus = function() {
       $.ajax({
           type: 'GET',
           url: window.pathname + '/checkstatus/' + GitHubDeploy.deployResult.id,
           contentType: 'application/json; charset=utf-8',
           dataType: 'json',
           success: function(data) {
               GitHubDeploy.deployResult = data;
               GitHubDeploy.renderDeploy();
               if(GitHubDeploy.deployResult.completedDate) {
                   window.clearInterval(GitHubDeploy.intervalId);
                   GitHubDeploy.checkDeploy();
               }
           }
       });
   }
   ```

3. **Final Results**
   ```javascript
   GitHubDeploy.checkDeploy = function() {
       $('#deploystatus').append('Deployment Complete');
       $('#deploy').attr('disabled', null);
       
       $.ajax({
           type: 'GET',
           url: window.pathname + '/checkdeploy/' + GitHubDeploy.deployResult.id,
           contentType: 'application/json; charset=utf-8',
           dataType: 'json',
           success: function(data) {
               $('#deploystatus').append(data);
           }
       });
   }
   ```

4. **Status Display**
   ```javascript
   GitHubDeploy.renderDeploy = function() {
       $('#deploystatus').append(
           '<div>Status: '+
               GitHubDeploy.deployResult.status + ' ' +
               (GitHubDeploy.deployResult.message != null ? 
                   GitHubDeploy.deployResult.message : '') +
           '</div>');
   }
   ```

### Server-Side Status Handling

1. **Status Check Endpoint**
   ```java
   @GetMapping("/githubdeploy/{owner}/{repo}/checkstatus/{asyncId}")
   @ResponseBody
   public String checkStatus(@PathVariable String asyncId,
                           @AuthenticationPrincipal OAuth2User user)
   // Returns current deployment status
   ```

2. **Final Results Endpoint**
   ```java
   @GetMapping("/githubdeploy/{owner}/{repo}/checkdeploy/{asyncId}")
   @ResponseBody
   public String checkDeploy(@PathVariable String asyncId,
                           @AuthenticationPrincipal OAuth2User user)
   // Returns final deployment results including any errors
   ```

### Status States

1. **Deployment States**
   - `Pending`: Initial state when deployment is queued
   - `InProgress`: Deployment is currently running
   - `Completed`: Deployment has finished
   - `Failed`: Deployment encountered errors
   - `Cancelled`: Deployment was cancelled

2. **Error Handling**
   - Component failures
   - Test failures
   - Code coverage warnings
   - Validation errors

### Controller Methods

1. **Authentication Methods**
   ```java
   @GetMapping("/githubdeploy/authorizegh")
   public String authorize(@RequestParam String code, @RequestParam String state, HttpSession session)
   // Handles GitHub OAuth callback, exchanges code for token

   @GetMapping("/githubdeploy/logout")
   public String logout(HttpSession session, @RequestParam(required=false) String retUrl)
   // Handles logout, clears GitHub token
   ```

2. **Deployment Methods**
   ```java
   @GetMapping("/githubdeploy/{owner}/{repo}")
   public String confirm(HttpServletRequest request, @PathVariable String owner, 
                        @PathVariable String repo, @RequestParam String ref,
                        @AuthenticationPrincipal OAuth2User user, HttpSession session, 
                        Map<String, Object> map)
   // Handles repository confirmation and initial setup

   @PostMapping("/githubdeploy/{owner}/{repo}")
   @ResponseBody
   public String deploy(@PathVariable String owner, @PathVariable String repo,
                       @RequestBody String repoContentsJson, HttpServletResponse response,
                       Map<String,Object> map, HttpSession session,
                       @AuthenticationPrincipal OAuth2User user)
   // Handles deployment to Salesforce

   @GetMapping("/githubdeploy/{owner}/{repo}/checkstatus/{asyncId}")
   @ResponseBody
   public String checkStatus(@PathVariable String asyncId,
                           @AuthenticationPrincipal OAuth2User user)
   // Checks deployment status

   @GetMapping("/githubdeploy/{owner}/{repo}/checkdeploy/{asyncId}")
   @ResponseBody
   public String checkDeploy(@PathVariable String asyncId,
                           @AuthenticationPrincipal OAuth2User user)
   // Gets final deployment results
   ```

3. **App Path Methods**
   ```java
   @GetMapping("/app/githubdeploy/**")
   public String forwardAppPath(HttpServletRequest request)
   // Forwards app paths to main controller
   ```

### Token Management

- **GitHub Tokens**
  - Stored in session as `GITHUB_TOKEN`
  - Removed on logout or error via `logout()` method
  - Used for private repository access

- **Salesforce Tokens**
  - Managed by Spring Security OAuth2
  - Stored in user session
  - Used for all Salesforce API calls
  - Accessed via `@AuthenticationPrincipal OAuth2User`

### Error Handling

- **GitHub Token Issues**
  - Invalid/expired tokens trigger re-authentication
  - Session cleared on token errors via `logout()`
  - Redirect to GitHub OAuth flow via `authorize()`

- **Salesforce Token Issues**
  - Handled by Spring Security
  - Redirect to Salesforce login
  - Session maintained for GitHub token

## Deployment Process

1. **Repository Selection**
   - User selects GitHub repository and branch/tag
   - System authenticates with GitHub
   - Repository contents are scanned for Salesforce metadata

2. **Metadata Processing**
   - System identifies Salesforce metadata components
   - Creates appropriate package structure
   - Handles both traditional and SFDX formats

3. **Deployment Execution**
   - Creates deployment package
   - Initiates asynchronous deployment
   - Monitors deployment progress
   - Reports results and errors

## Security

- Uses OAuth2 for secure authentication
- Implements session management
- Handles token refresh and validation
- Manages GitHub and Salesforce permissions

## Error Handling

- Comprehensive error reporting
- Validation of repository contents
- Deployment status monitoring
- Detailed error messages for troubleshooting

## Configuration

The application requires the following environment variables:

### GitHub Configuration
- `GITHUB_CLIENT_ID`: GitHub OAuth application client ID
- `GITHUB_CLIENT_SECRET`: GitHub OAuth application client secret

### Salesforce Configuration
- `SFDC_OAUTH_CLIENT_ID`: Salesforce Connected App Consumer Key
- `SFDC_OAUTH_CLIENT_SECRET`: Salesforce Connected App Consumer Secret
- `SFDC_END_POINT`: Salesforce login domain (e.g., `login.salesforce.com` for production or `test.salesforce.com` for sandbox)

### Spring Security Configuration
The following properties should be configured in `application.properties` or `application.yml`:

```yaml
spring:
  security:
    oauth2:
      client:
        registration:
          salesforce:
            client-id: ${SFDC_OAUTH_CLIENT_ID}
            client-secret: ${SFDC_OAUTH_CLIENT_SECRET}
            authorization-grant-type: authorization_code
            redirect-uri: "{baseUrl}/app/githubdeploy"
            scope: api,refresh_token,offline_access
        provider:
          salesforce:
            authorization-uri: https://${SFDC_END_POINT}/services/oauth2/authorize
            token-uri: https://${SFDC_END_POINT}/services/oauth2/token
            user-info-uri: https://${SFDC_END_POINT}/services/oauth2/userinfo
            user-name-attribute: preferred_username
```

Note: The Salesforce environment (production or sandbox) is selected by the user in the UI, and the appropriate OAuth endpoints are used automatically.

## Dependencies

Key dependencies include:
- Spring Boot
- Salesforce Metadata API
- GitHub API
- OAuth2 libraries
- JSP and JSTL
- SLDS (Salesforce Lightning Design System) for UI

## Development

To contribute to the project:
1. Fork the repository
2. Set up required environment variables
3. Build using Maven
4. Run tests
5. Submit pull requests

## Support

For issues and feature requests, please use the GitHub issue tracker. 