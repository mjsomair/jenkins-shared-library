# Work in progress ...

# Jenkins Shared Library : Artifactory + gitflow + SonarQube #

This library provides a set (hopefully complete) of globals centered around JFrog Artifactory to automate end-to-end CI/CD process with Jenkins pipelines.
It assumes you are following the gitflow process for source code management.

Required Jenkins plugins:
- Artifactory
- Role Based Authorization Strategy
- Email Extension
- Groovy Post Build

### Jenkins Shared Library Reference ###

https://jenkins.io/doc/book/pipeline/shared-libraries/

### Why Gitflow ### 

https://nvie.com/posts/a-successful-git-branching-model/

### JFrog Artifactory Reference ###

https://www.jfrog.com/confluence/pages/

# Available Globals #

| Global        	| Short Description  |
| -----------------	| :----------------- |
| authorizeStage	| Requests/verifies authorization for pipeline stage based on roles|
| buildCause		| Gets the cause for the build|
| ciSkipCheck	 	| Skips the build if last commit had [ci-skip] in the message|
| ciSkipPush		| Commits and pushes to origin git repo any changed files (the ones already tracked, will ignore any new files)|
| config			| Configuration used by most of the included globals in this library. Edit this for your environment|
| disableTriggers	| Disables (comments out) triggers{} section in Jenkinsfile|
| getApproval		| Requests approval based on the roles. Will send email and wait for manual action to approve/abort (within given timeout)|
| getSids4Roles		| Retrieves system IDs (User IDs and Group IDs) for a given role|
| hasChanges		| Checks if there are any actual changes in the source code repository between this build and previous build|
| mergeMaps			| Custom Merge of 2 multi-level Groovy maps. Takes base map and then updates values based on the diff map - up to 3 levels deep|
| pkgJsonVer		| Changes version attribute in all package.json files (recursively from current dir to all subdirs) to a given version| 
| postBuild			| Post-build actions wrapper (add a badge to the build, etc)|
| rtBuild			| Common Build wrapper Using JFrog Artifactory Plugin. Currently supports Maven, Gradle, Grunt, dotnet and free-form shell script based builds|