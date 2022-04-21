class SonarScanner 
{
    public static runSonarScanner()
        {     
          mvn clean install  
          mvn sonar:sonar -Dsonar.projectKey=develop -Dsonar.sources=src
        }
  }
