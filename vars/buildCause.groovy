// Gets the cause(s) of the build - what/who triggered it
// Needs to be defined as global in shared library due to the use of currentBuild.rawBuild.getCauses

def call() {
	def causes = currentBuild.rawBuild.getCauses()
	def result = ''
	for (cause in causes) {
		result += cause.getShortDescription() + ';'
	}
	return result
}