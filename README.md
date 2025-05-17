# GitHub Salesforce Deploy Tool

Read more about this tool at my blog post [here](http://andyinthecloud.com/2013/09/24/deploy-direct-from-github-to-salesforce/).

## Developing

* Use Maven to download dependencies: `mvn install`

* Use Maven to build the war package: `mvn package`

* Run the package locally: `mvn spring-boot:run`

* Success Test with http://localhost:5000/app/githubdeploy/apex-enterprise-patterns/fflib-apex-mocks 
* Failure Test with http://localhost:5000/app/githubdeploy/afawcett/custommetadataapi-demo
* Private Repo http://localhost:5000/app/githubdeploy/afawcett/github-sfdeploy-test 
* No package.xml http://localhost:5000/app/githubdeploy/SalesforceSFDC/Apex-Classes 
* Success Test with  http://localhost:5000/app/githubdeploy/benedwards44/Apex-for-Xero 

## Changelog

**Update: April 2025:** Major upgrade to latest Spring Boot and Salesforce API

**Update: 8 February 2024:** Added support for git submodules as part of SFDX formatted repositories.

**Update: 29 February 2020:** Added support for SFDX formatted repositories. Note currently it only supports the default package directory as defined in the sfdx-project.json file.

**Update: 3 April 2016:** Maintainance to move to Cedar-14 on Heroku, enhancements, bug fixes and a UI restlye, see [blog](http://andyinthecloud.com/2016/04/02/github-salesforce-deploy-lightning-edition/) for more details.

**Update: 21 May 2015:** Added support for private GitHub repositories. When deploying a private repository the user is redirected to GitHub authorization window. After authorizing, a private repository can be deployed exactly the same way as a public one. Contributed by [Moti Korets](https://github.com/motiko).

**Update: 27th September 2014:** Added feature to generate Deploy to Salesforce button, thanks to [Karanraj](https://twitter.com/karanrajs) for the image.

![Button](https://raw.githubusercontent.com/afawcett/githubsfdeploy/master/src/main/webapp/resources/img/deploy.png)

**Updated: 7th December 2013:** Added Sandbox support see [here](http://andyinthecloud.com/2013/12/07/updated-github-deploy-tool-sandbox-support/)

![Logo](http://andrewfawcett.files.wordpress.com/2013/09/githubsfdeploy.png)
