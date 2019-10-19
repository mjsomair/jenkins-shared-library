// Modifies Jenkinsfile to comment out triggers{} section
// Useful for release-*/hotfix-*/master/feature/tags branches especially if Jenkinsfile in develop branch had polling/cron triggers
// This global will NOT commit/push changes back to the repository
// Assumption is that ciSkipPush will be used to commit/push the changes
// TODO: fix adding infinite // in front 

def call() {
	// Replaces content and comments out the triggers{} section (add // at the beginning of each line o fthe section)
	// This assumes of course that the section will start with
	// 					triggers {
	// and will end with
	//							}

	// universal Groovy implementation
	def jenkinsfile = readFile file: "Jenkinsfile"
	def new_jenkinsfile = jenkinsfile.normalize().replaceFirst($/(?m)^((?!//).)*(?s)(\s?)(triggers\s?\{.*?\})/$,'$1// TRIGGERS ARE DISABLED FOR THIS BRANCH OR TAG')
	writeFile file: "Jenkinsfile", text: new_jenkinsfile
}