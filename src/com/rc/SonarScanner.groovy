class SonarScanner 
{
    public static runSonarScanner()
        {        
          sonar:sonar -Dsonar.projectKey=develop -Dsonar.sources=src
        }
  }
