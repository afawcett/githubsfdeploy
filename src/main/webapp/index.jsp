<!doctype html>

<html>
  <head>
    <meta charset="utf-8">
    <title>Spring MVC Template for Salesforce</title>

    <meta content="IE=edge,chrome=1" http-equiv="X-UA-Compatible">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <link href="/resources/css/bootstrap.min.css" rel="stylesheet" type="text/css">
    <link href="/resources/css/bootstrap-responsive.min.css" rel="stylesheet" type="text/css">
    <link href="" rel="stylesheet" type="text/css">

    <!--
    IMPORTANT:
    This is Heroku specific styling. Remove to customize.
    -->
    <link href="/resources/css/heroku.css" rel="stylesheet">
    <style type="text/css">
      .instructions { display: none; }
      .instructions li { margin-bottom: 10px; }
      .instructions h2 { margin: 18px 0;}
      .instructions blockquote { margin-top: 10px; }
      .screenshot {
        margin-top: 10px;
        display:block;
      }
      .screenshot a {
        padding: 0;
        line-height: 1;
        display: inline-block;
        text-decoration: none;
      }
      .screenshot img, .tool-choice img {
        border: 1px solid #ddd;
        -webkit-border-radius: 4px;
        -moz-border-radius: 4px;
        border-radius: 4px;
        -webkit-box-shadow: 0 1px 1px rgba(0, 0, 0, 0.075);
        -moz-box-shadow: 0 1px 1px rgba(0, 0, 0, 0.075);
        box-shadow: 0 1px 1px rgba(0, 0, 0, 0.075);
      }
    </style>
    <!-- /// -->
    <script type="text/javascript">
      <!--
      function appname() {
          return location.hostname.substring(0,location.hostname.indexOf("."));
      }
      // -->
    </script>
  </head>

  <body>
    <div class="navbar navbar-fixed-top">
      <div class="navbar-inner">
        <div class="container">
          <a href="/" class="brand">Spring MVC Template for Salesforce</a>
          <!--
          IMPORTANT:
          This is Heroku specific markup. Remove to customize.
          -->
          <a href="/" class="brand" id="heroku">by <strong>heroku</strong></a>
          <!-- /// -->
        </div>
      </div>
    </div>

    <div class="container" id="getting-started">
      <div class="row">
        <div class="span8 offset2">
          <h1 class="alert alert-success">Your app is ready!</h1>
          
          <div class="page-header">
            <h1>Get started with your Spring MVC Application for Salesforce</h1>
          </div>
          
          <div style="margin-bottom: 20px">
            This is a template for a Spring MVC web application to work with data from Salesforce.
            The sample code is a dynamic CRUD application that allows users to create, read, edit, and delete any Salesforce object.
            To try it out go to the <a href="/sfdc/sobjects">My Objects</a> page. Then use Eclipse or the Command Line to deploy some changes.
          </div>
          
        <ul id="tab" class="nav nav-tabs">
            <li class="active"><a href="#eclipse-instructions" data-toggle="tab">Use Eclipse 3.7</a></li>
            <li><a href="#cli-instructions" data-toggle="tab">Use Command Line</a></li>
        </ul>

        <div class="tab-content">


          <div id="eclipse-instructions" class="instructions tab-pane active">
            <a name="using-eclipse"></a>
            <h1>Using Eclipse 3.7:</h1>
            
            <h2>Step 1. Setup your environment</h2>
            <ol>
              <li>Ensure <a href="http://unicase.blogspot.com/2011/01/egit-tutorial-for-beginners.html">EGit</a> is installed.</li>
              <li>Ensure the <a href="http://www.eclipse.org/m2e/">Maven Eclipse Plugin</a> is installed.</li>
              <li>Create an SSH key if you haven't already:
                <ol>
                  <li>Go to <code>Window</code> <i class="icon-chevron-right"></i> <code>Preferences</code> <i class="icon-chevron-right"></i> <code>General</code> <i class="icon-chevron-right"></i> <code>Network Connections</code> <i class="icon-chevron-right"></i> <code>SSH2</code></li>
                  <li>Choose the <code>Key Management</code> tab</li>
                  <li>Click <code>Generate RSA Key...</code>
                    <div class="modal hide" id="addingSshKey">
                      <div class="modal-header">
                        <a class="close" data-dismiss="modal">×</a>
                        <h3>Generate RSA Key</h3>
                      </div>
                      <div class="modal-body">
                        <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/2-3-adding-ssh-key.png" alt="SSH Eclipse Preferences Window" />
                      </div>
                    </div>
                    <span class="screenshot">
                      <a href="#addingSshKey" data-toggle="modal">
                        <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/2-3-adding-ssh-key.png" alt="SSH Eclipse Preferences Window" width="100" />
                        <i class="icon-zoom-in"></i>
                      </a>
                    </span>
                  </li>
                  <li>Copy the generated public key in the text box and <a href="https://api.heroku.com/account/ssh" class="btn btn-primary btn-mini">add it to your account</a></li>
                  <li>Click <code>Save Private Key...</code>, accepting the defaults</li>
                  <li>Click <code>Ok</code></li>
                </ol>
              </li>
            </ol>

            <h2>Step 2. Clone the App</h2>
            <ol>
              <li>Go to <code>File</code> <i class="icon-chevron-right"></i> <code>Import...</code> <i class="icon-chevron-right"></i> <code>Git</code> <i class="icon-chevron-right"></i> <code>Projects from Git</code>
                <div class="modal hide" id="importFromGit">
                  <div class="modal-header">
                    <a class="close" data-dismiss="modal">×</a>
                    <h3>Import</h3>
                  </div>
                  <div class="modal-body">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/2-1-import.png" alt="Import" />
                  </div>
                </div>
                <span class="screenshot">
                  <a href="#importFromGit" data-toggle="modal">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/2-1-import.png" alt="Import" width="100" />
                    <i class="icon-zoom-in"></i>
                  </a>
                </span>
              </li>
              <li>Choose <code>URI</code> and click <code>Next</code></li>
              <li>Enter <code>git@heroku.com:<script>document.write(appname());</script>.git</code> in the <code>URI</code> field.
                <div class="modal hide" id="cloneGitRepository">
                  <div class="modal-header">
                    <a class="close" data-dismiss="modal">×</a>
                    <h3>Clone Git Repository</h3>
                  </div>
                  <div class="modal-body">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/3-4-clone-git-repo.png" alt="Clone Git Repository" />
                  </div>
                </div>
                <span class="screenshot">
                  <a href="#cloneGitRepository" data-toggle="modal">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/3-4-clone-git-repo.png" alt="Clone Git Repository" width="100" />
                    <i class="icon-zoom-in"></i>
                  </a>
                </span>
              </li>
              <li>Click <code>Next</code> three times
                <blockquote>
                Click <code>Yes</code> to the question of authenticity if the question appears.
                </blockquote>
              </li>
              <li>Choose <code>Import as general project</code>
                <div class="modal hide" id="importProjectsFromGit">
                  <div class="modal-header">
                    <a class="close" data-dismiss="modal">×</a>
                    <h3>Import Projects from Git</h3>
                  </div>
                  <div class="modal-body">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/2-6-import-general.png" alt="Import Projects from Git" />
                  </div>
                </div>
                <span class="screenshot">
                  <a href="#importProjectsFromGit" data-toggle="modal">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/2-6-import-general.png" alt="Import Projects from Git" width="100" />
                    <i class="icon-zoom-in"></i>
                  </a>
                </span>
              </li>
              <li>Click <code>Finish</code></li>
            </ol>

            <h2>Step 3. Configure the App</h2>
            <ol>
              <li>Right-click the project root</li>
              <li>Choose <code>Configure</code> <i class="icon-chevron-right"></i> <code>Convert to Maven Project</code> </li>
            </ol>

            <h2>Step 4. Makes some changes to the app</h2>
            <ol>
              <li>Open <code>listSObjectTypes.jsp</code></li>
              <li>Display the list of objects by their plural label instead of their standard, singular label
                  by replacing <code>type.label</code> on line 13 with <code>type.labelPlural</code></li>
            </ol>

            <h2>Step 5. Deploy to Heroku</h2>
            <ol>
              <li>Right-click the project root and choose <code>Team</code> <i class="icon-chevron-right"></i> <code>Commit</code></li>
              <li>Enter a commit message and click <code>Commit</code>
                <div class="modal hide" id="commitChanges">
                  <div class="modal-header">
                    <a class="close" data-dismiss="modal">×</a>
                    <h3>Commit Changes</h3>
                  </div>
                  <div class="modal-body">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/6-5-commit.png" alt="Commit Changes" />
                  </div>
                </div>
                <span class="screenshot">
                  <a href="#commitChanges" data-toggle="modal">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/6-5-commit.png" alt="Commit Changes" width="100" />
                    <i class="icon-zoom-in"></i>
                  </a>
                </span>
              </li>
              <li>Right-click the project root and choose <code>Team</code> <i class="icon-chevron-right"></i> <code>Push to Upstream</code></li>
              <li>Review the push results. At the bottom, a "... deployed to Heroku" message will appear.
                <div class="modal hide" id="pushResults">
                  <div class="modal-header">
                    <a class="close" data-dismiss="modal">×</a>
                    <h3>Push Results</h3>
                  </div>
                  <div class="modal-body">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/6-8-push-result.png" alt="Push Results" />
                  </div>
                </div>
                <span class="screenshot">
                  <a href="#pushResults" data-toggle="modal">
                    <img src="https://s3.amazonaws.com/template-app-instructions-screenshots/eclipse/6-8-push-result.png" alt="Push Results" width="100" />
                    <i class="icon-zoom-in"></i>
                  </a>
                </span>
              </li>
            </ol>

            <h2>Step 6. Provision an add-on</h2>
            <ol>
                This app includes optional caching support for Memcache, which can greatly increase the performance when loading data from Salesforce.
                Caching is optional, but highly recommended.
                To add caching support, <a class="appAppendable" href="https://api.heroku.com/v3/resources/memcache?selected=">provision the Memcache add-on</a>.
                Note, provisioning add-ons requires your <a href="https://api.heroku.com/verify">Heroku account be verified</a>.
            </ol>

            <div class="hero-unit">
              <h1>Done!</h1>
              <p>You've just cloned, modified, and deployed a brand new app.</p>
              <a href="/sfdc/sobjects" class="btn btn-primary btn-large">See your changes</a>
                
              <p style="margin-top: 20px">Learn more at the   
              <a href="http://devcenter.heroku.com/categories/java">Heroku Dev Center</a></p>
            </div>
          </div>






          <div id="cli-instructions" class="instructions tab-pane">
            <a name="using-cli"></a>
            <h1>Using Command Line:</h1>

            <h2>Step 1. Setup your environment</h2>
            <ol>
              <li>Install the <a href="http://toolbelt.heroku.com">Heroku Toolbelt</a>.</li>
              <li>Install <a href="http://maven.apache.org/download.html">Maven</a>.</li>
            </ol>

            <h2>Step 2. Login to Heroku</h2>
            <code>heroku login</code>
            <blockquote>
              Be sure to create, or associate an SSH key with your account.
            </blockquote>
            <pre>
$ heroku login
Enter your Heroku credentials.
Email: naaman@heroku.com
Password:
Could not find an existing public key.
Would you like to generate one? [Yn] Y
Generating new SSH public key.
Uploading SSH public key /Users/Administrator/.ssh/id_rsa.pub
Authentication successful.</pre>

            <h2>Step 3. Clone the App</h2>
            <code>git clone -o heroku git@heroku.com:<script>document.write(appname())</script>.git</code>

            <h2>Step 4. Makes some changes to the app</h2>
            <ol>
              <li>Open <code>listSObjectTypes.jsp</code> in your favorite editor</li>
              <li>Display the list of objects by their plural label instead of their standard, singular label
                  by replacing <code>type.label</code> on line 13 with <code>type.labelPlural</code></li>
            </ol>

            <h2>Step 5. Make sure the app still compiles</h2>
            <code>mvn clean package</code>

            <h2>Step 6. Deploy your changes</h2>
            <ol>
              <li><code>git commit -am "New changes to deploy"</code></li>
              <li><code>git push heroku master</code></li>
            </ol>
              
            <h2>Step 7. Provision an add-on</h2>
            <ol>
                This app includes optional caching support for Memcache, which can greatly increase the performance when loading data from Salesforce.
                Caching is optional, but highly recommended.
                To add caching support, provision the Memcache add-on:<p><code>heroku addons:add memcached</code></p>
                Note, provisioning add-ons requires your <a href="https://api.heroku.com/verify">Heroku account be verified</a>.
            </ol>
              
            <div class="hero-unit">
              <h1>Done!</h1>
              <p>You've just cloned, modified, and deployed a brand new app.</p>
              <a href="/sfdc/sobjects" class="btn btn-primary btn-large">See your changes</a>
                
              <p style="margin-top: 20px">Learn more at the   
              <a href="http://devcenter.heroku.com/categories/java">Heroku Dev Center</a></p>
            </div>
          </div>
        </div>
      </div>
    </div>
    
  <!-- end tab content -->  
  </div>

    <script src="/resources/js/jquery-1.7.1.min.js"></script>
    <script src="/resources/js/bootstrap-modal.js"></script>
    <script src="/resources/js/bootstrap-tab.js"></script>
    <script type="text/javascript">
        $('.appAppendable').each(function() {
            this.href += appname()
        });
    </script>
  </body>
</html>
