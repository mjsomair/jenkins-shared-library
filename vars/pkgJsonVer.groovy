// Searches for and if found modifies package.json file(s) to set "version" attribute 
// Useful for node.js based builds
// This global will NOT commit/push chnages back to the repository
// Assumption is that ciSkipPush will be used to commit/push the changes
//
// Arguments:
// version		- a value to set version attribute in package.json file(s)

def call(version) {
	// version must be in the format of number.number.number
	if (version ==~ /^\d+\.\d+\.\d+$/) {
		if (fileExists(file: 'package.json')) {
			def package_json = readJSON file: 'package.json'
			package_json.version = version
			writeJSON file: 'package.json', json: package_json, pretty: 2
		}
	} else {
		error "Invalid version format"
	}
}