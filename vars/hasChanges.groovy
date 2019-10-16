// Checks if there are any actual changes in the source code repository for this build compared to previous build
// Useful for situations when the build is triggered manually instead of by SCM change and some stages should be skipped
// I.e. no point doing SonarQube scan if there are no actual code changes


def call() {
	def gitlog = ""
	def loc_changed = 0
	if (isUnix()) {
		gitlog = sh (script: '''git log --numstat --pretty="%H" $GIT_PREVIOUS_COMMIT..$GIT_COMMIT''', returnStdout: true).trim()
	} else {
		gitlog = bat (script: '''git log --numstat --pretty="%%H" %GIT_PREVIOUS_COMMIT%..%GIT_COMMIT%''', returnStdout: true).trim()
	}
	gitlog.splitEachLine('\t') { items ->
		if( items.size() == 3 && items[0].isInteger() && items[1].isInteger() ) {
			loc_changed += items[0].toInteger() + items[1].toInteger()
		}
	}
	return loc_changed > 0
}