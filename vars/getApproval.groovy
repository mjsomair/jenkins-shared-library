// Performs the notification and accept an input from someone in the role to proceed or abort  
// An HTML formatted email is sent to the approvers
//
// Arguments map:
// args.approvers			- a hashmap, CSV string, or a list of the approvers  
// args.emailPrompt 		- a message to include in the email requesting approval
// args.emailSubject		- email subject
// args.inputPrompt			- a message to include on the UI for input field requesting approval
// args.timeout				- timeout to wait for manual approval (a number)
// args.timeoutUnit			- timeout units of measure (HOURS, DAYS, etc)
//
// Dependencies:
// + Must have Email Extension plugin


def call(Map args = [:]) {
	// set args defaults and replace defaults with actuals if passed in the call
	def effectiveArgs = [
			timeout: 1, 
			timeoutUnit: 'HOURS', 
			emailSubject: "Action Required For Build ${JOB_NAME} (#${env.BUILD_NUMBER})",
			emailPrompt: "Action Required For Build ${JOB_NAME} (#${env.BUILD_NUMBER})",
			inputPrompt: "Do you approve?"
	]
	effectiveArgs << args
	if (!effectiveArgs.approvers ) {
		throw new Exception("Must provide approvers")
	}
	def csvApproverUsernames = {
		switch(effectiveArgs.approvers) {
			case String:
				// already csv
				return effectiveArgs.approvers
			case Map:
				// keys are usernames and values are names
				return effectiveArgs.approvers.keySet().join(',')
			case List:
				return effectiveArgs.approvers.join(',')
			default:
				throw new Exception("Unexpeced approver type ${effectiveArgs.approvers.class}!")
		}
	}()

	emailext(
		subject: effectiveArgs.emailSubject,
		mimeType: 'text/html',
		body: "Build: <b>${JOB_NAME}</b><br>Build Number: <b>${env.BUILD_NUMBER}</b><br/><br/>${effectiveArgs.emailPrompt}<br/><br/>Action is required at: ${env.BUILD_URL}/input ",
		to: csvApproverUsernames
	)
	
	// Set milestones and then wait for input (Approve or Abort)
	// Time out (fail, i.e. Abort) after specified amount of time	
	milestone()
	timeout(time: effectiveArgs.timeout, unit: effectiveArgs.timeoutUnit) {
		input message: effectiveArgs.inputPrompt, submitter: csvApproverUsernames, ok: 'Approve'
	}
	milestone()
}