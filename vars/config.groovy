// Configuration used by most of the included globals in this library
// This is where you define your environment specific settings like URLs, Jenkins credential IDs, etc.
// It does not take any arguments
// Returns a map with config values

def call() {
	def config = [
		
		git_provider_credentials_id: 'git-provider-credentials',
		artifactory_credentials_id: 'artifactory-credentials',
		
		artifactory_server_id: 'Artifactory',
		
		sonarqube_installation_name: 'SonarQube',
		sonarqube_installation_tool: 'sonar-scanner-linux',
		
		artifactory_resolver_maven_release_repo: 'libs-release', 
		artifactory_resolver_maven_snapshot_repo: 'libs-snapshot',
		
		artifactory_deployer_maven_release_repo: 'libs-release-local',
		artifactory_deployer_maven_snapshot_repo: 'libs-snapshot-local',
		
		artifactory_resolver_gradle_repo: 'gradle',
		
		artifactory_deployer_gradle_snapshot_repo: 'gradle-snapshot-local',
		artifactory_deployer_gradle_release_repo: 'gradle-release-local',
		
		artifactory_deployer_generic_release_repo: 'generic-release-local',
		artifactory_deployer_generic_snapshot_repo: 'generic-snapshot-local',
		
	]
	return config
}