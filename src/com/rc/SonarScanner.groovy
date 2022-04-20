package com.rc;

class SonarScanner {
   

    SonarScanner() {
       
    }

    
    def runSonarScanner()
    {        
        def SQscannerHome = tool 'sonar-scanner-linux'
        withSonarQubeEnv('SonarQube') 
        {
           bat "${SQscannerHome}/bin/sonar-scanner -Dsonar.projectKey=develop -Dsonar.sources=src"
        }
    }
    
}
