class SonarScanner 
{
    public static runSonarScanner()
        {        
          mvn sonar:sonar -Dsonar.projectKey=develop -Dsonar.sources=src
        }
  }
