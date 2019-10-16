// If pipleine was triggered by a user manually
//	- Checks authorization for the user executing the build against roles and permissions
// If pipleine was triggered by any other means
//	- Request manual approval from any user that has the role granting the deployment permission
// Roles and permissions are configured using the "RoleBasedAuthorizationStrategy" plugin
// Project specific role names are <job_name>.<stage_name>
// Example: api-pipeline.prod-deploy
//
// Dependencies:
// + Must have Role Based Authorization Strategy plugin

def call() {
		// Retrieve users that have role granted
		// Role could be global or project specific
		// For Global role use STAGE_NAME
		// For Project role use a combination of JOB_NAME and STAGE_NAME
		// Also take into account Global "admin" role and Global "deploy" role
		def roles = [
				global: [
					'admin',
					'deploy',
					env.STAGE_NAME
				],
				project: [env.JOB_NAME+'.'+env.STAGE_NAME]
		]
		def sids = getSids4Roles(roles)
		// Make sure that pipeline was triggered by a user
		def user = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)?.getUserId()
		if (user) {
			// Get the list of groups (authorities in Jenkins speak) for the user
			def auths =  Jenkins.instance.securityRealm.loadUserByUsername(user).authorities.collect{a -> a.authority} ?: []
			// Check if user has been granted the role directly or via group (authority)
			def authorization = sids.contains(user) || !sids.disjoint(auths)
			echo "Stage \"${STAGE_NAME}\" " + (authorization ? "Authorized" : "Unauthorized")
			return authorization
		} else {
			echo "Requesting approval for stage \"${STAGE_NAME}\" from: "+sids.join(',')
			getApproval(approvers: sids, 
				inputPrompt: "Initiate \"${STAGE_NAME}\" for ${JOB_NAME} ?", 
				emailPrompt: "Build ${JOB_NAME} (#${env.BUILD_NUMBER}) is ready for \"${STAGE_NAME}\"",
				emailSubject: "Approval Requested For Stage \"${STAGE_NAME}\" Build ${JOB_NAME} (#${env.BUILD_NUMBER})",
				timeout: 1,
				timeoutUnit: 'HOURS'
			)
			// If we reach this point - it was approved
			return true
		}
}