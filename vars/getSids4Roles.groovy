// Returns a list of users and groups for the role(s)
//
// Arguments map:
// roles				- a hashmap of roles. Only global and project keys are accepted
// roles.global			- a list of global roles
// roles.project		- a list of project roles  
//
// Dependencies:
// + Must have Role Based Authorization Strategy plugin

import com.michelin.cio.hudson.plugins.rolestrategy.RoleBasedAuthorizationStrategy

// Must use @NonCPS since RoleBasedAuthorizationStrategy is not serializable
@NonCPS
def call(Map roles = [:]) {
	// Set defaults
	def effectiveRoles = [global: ['user'], project: [env.JOB_NAME]]
	effectiveRoles << roles
	// Verify that we have the "RoleBasedAuthorizationStrategy" plugin
	def authStrategy = Jenkins.instance.getAuthorizationStrategy()
	if(authStrategy instanceof RoleBasedAuthorizationStrategy){
		// Retrieve users/groups that have role(s) granted
		def sids = []
		effectiveRoles.global.each {role ->
			sids += authStrategy.roleMaps.globalRoles.getSidsForRole(role) ?: []
		}
		effectiveRoles.project.each {role ->
			sids += authStrategy.roleMaps.projectRoles.getSidsForRole(role) ?: []
		}
		return sids
	} else {
		error("Role Strategy Plugin not in use.  Please enable to retrieve users for a role")
	}
}