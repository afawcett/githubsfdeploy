# Spring MVC and Hibernate template application

This is a template for a web application that uses Spring MVC and Hibernate. The sample code is a simple CRUD page that manipulates records for a single model object.

## Running the application locally

First build with:

    $ mvn clean install

Add environment variablesfor authenticating to Salesforce.com:

- On Linux/Mac:

        $ export FORCE_USERNAME=blah@foo.com
        $ export FORCE_PASSWORD=asdf1234

- On Windows:

        $ set FORCE_USERNAME=blah@foo.com
        $ set FORCE_PASSWORD=asdf1234

Then run it with:

    $ java -jar target/dependency/webapp-runner.jar target/*.war

## Running on Heroku

Clone this project locally:

    $ git clone git://github.com/jamesward/hello-java-spring-force_dot_com.git

Create a new app on Heroku (make sure you have the [Heroku Toolbelt](http://toolbelt.heroku.com) installed):

    $ heroku login
    $ heroku create -s cedar

Add config params for authenticating to Salesforce.com:

    $ heroku config:add FORCE_USERNAME=blah@foo.com FORCE_PASSWORD=asdf1234

Upload the app to Heroku:

    $ git push heroku master

Open the app in your browser:

    $ heroku open

