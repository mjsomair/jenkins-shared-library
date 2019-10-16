// Wrapper for Groovy Post Build plugin
// Allows the use of "unsafe"/"non-whitelisted" methods in the pipelines, since globals from libraries are trusted by default
// Especialy useful when manipulating older/other builds
//
// Arguments map:
// args	- a map of actions and associated arguments
//	Currently supported:
//		action: "addBadge"
//		args[]
//			icon:			"path/and/filename/of_badge_icon.png"					(technically you can skip it, but it will look weird)
//			text:			"text to show as a tooltip/hint over the badge icon" 	(optional)
//			buildNumber:	"build number - can also pass older/other build number" (optional, if skipped uses current build)
//				
//
// Dependencies:
// + Must have Groovy Post Build plugin

def call(Map args = [:]) {
	switch (args.action) {
		case ~/^addBadge$/:
			return addbadge(args.args)
			break
		default:
			error 'Usupported Action: '+args.action
			break
	}
}

def addbadge(Map args = [:]) {
	// Groovy is awesome! below line demostrates the null safe and Elvis operators in action
	def buildnumber = args?.buildNumber?.toInteger() ?: manager.build.number
	def mng = manager
	if (mng.setBuildNumber(buildnumber)) {
		echo "Adding Badge to build number ${mng.build.number}"
		mng.addBadge(args?.icon,args?.text)
	}
}

