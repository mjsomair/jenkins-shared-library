class SonarScanner {
   

    SonarScanner() {
       
    }

    
    def runSonarScanner()
    {
        def config = config()
        def SQscannerHome = tool config.sonarqube_installation_tool
        withSonarQubeEnv(config.sonarqube_installation_name) 
        {
           bat "${SQscannerHome}/bin/sonar-scanner -Dsonar.projectKey=develop -Dsonar.sources=src"
        }
    }
    
}
