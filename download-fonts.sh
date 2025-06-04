#!/bin/bash

# Create directories if they don't exist
mkdir -p src/main/webapp/resources/assets/fonts/webfonts

# Download fonts
curl -L "https://www2.salesforce.com/assets/fonts/webfonts/SalesforceSans-Light.woff" -o "src/main/webapp/resources/assets/fonts/webfonts/SalesforceSans-Light.woff"
curl -L "https://www2.salesforce.com/assets/fonts/webfonts/SalesforceSans-Bold.woff" -o "src/main/webapp/resources/assets/fonts/webfonts/SalesforceSans-Bold.woff"

# Download favicon
curl -L "https://www.salesforce.com/favicon.ico" -o "src/main/webapp/resources/favicon.ico" 