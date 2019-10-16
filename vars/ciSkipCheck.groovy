// Aborts a build if it was triggered by code change which was done by Jenkins CI itself
// Example would be maven release/hotfix builds that automatically change the pom file(s) and commit them back to the repo

def call(Map args) {
	if (args.action == 'check') {
		return check()
	}
	if (args.action == 'postProcess') {
		return postProcess()
	}
	error 'ciSkip has been called without valid arguments'
}

def check() {
	env.CI_SKIP = "false"
	def isCiSkip = false
	if (isUnix()) {
		isCiSkip = sh (script: "git log -1 | grep '.*\\[ci-skip\\].*'", returnStatus: true) == 0
	} else {
		isCiSkip = bat (script: 'git log -1 | findstr /C:"[ci-skip]"', returnStatus: true) == 0
	}
	def isStartedByUser = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause) != null
	if (isCiSkip && !isStartedByUser) {
		env.CI_SKIP = "true"
		error "'[ci-skip]' found in git commit message. Aborting."
	}
}

def postProcess() {
	if (env.CI_SKIP == "true") {
		currentBuild.result = 'NOT_BUILT'
	}
}