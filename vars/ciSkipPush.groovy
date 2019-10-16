// Commits changes made by Jenkins and pushes them to the origin with [ci-skip] marker in the commit comments
// Will do nothing if no changes were made
// The rtBuild global could change pom.xml, package.json, and other files
// The [ci-skip] comment is checked by another global "ciSkipCheck" to skip running pipeline again when triggered by scm change / scm poll trigger
// TODO: fix setting user.email 


def call() {
	def config = config()
	def isStartedByUser = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause) != null
	if (isStartedByUser) {
		echo "Build triggered manually, skipping auto-commit&push"
		return true
	}
	
	withCredentials([usernamePassword(credentialsId: config.git_provider_credentials, passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
		def gitRepoHost = ""
		if (isUnix()) {
			gitRepoHost = sh(returnStdout: true, script: 'git config remote.origin.url').trim().replaceFirst('https://','')
			sh returnStatus: true, script: """
				git config --global push.default simple
				git diff-index --quiet HEAD || 
					( 
						git commit -am '[ci-skip] : ${BRANCH_NAME} build by Jenkins' && 
						git push -u https://${GIT_USERNAME}:${GIT_PASSWORD}@${gitRepoHost} ${BRANCH_NAME}						
					)
			"""
		} else {
			gitRepoHost = bat(returnStdout: true, script: 'git config remote.origin.url').trim().replaceFirst('https://','')
			bat returnStatus: true, script: """
				git config --global push.default simple
				git diff-index --quiet HEAD || ( git commit -am '[ci-skip] : ${BRANCH_NAME} build by Jenkins' && git push -u https://${GIT_USERNAME}:${GIT_PASSWORD}@${gitRepoHost} ${BRANCH_NAME}	)
			"""
		} 
		
	}
}