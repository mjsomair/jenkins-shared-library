// Common Build Wrapper Using JFrog Artifactory Plugin
//
// Arguments Map:
//
// args				- Either a map of options for supported build types or a String specifying build type
//
// args.maven					- Map of options for Maven type of build
// args.maven.pom 				- path and filename to pom.xml, normally it will be just 'pom.xml'
// args.maven.goals 			- maven goals string, i.e. something like 'clean install'
// args.maven.failOnSnapshot	- whether or not to fail build with any SNAPSHOT dependencies (optional, default false, will be overwritten for some branch types)
//
// args.grunt					- Map of options for Node.js type of build
// args.grunt.script 			- Unix/Linux sh script to execute to bild and produce artifact(s) - normally a set of npm & grunt commands
// args.grunt.artifacts 		- a file pattern of artifact(s) to publish to Artifactory (defaults to *.tgz)
//
// args.sh						- Map of options for generic sh/bat/powershell type of build
// args.sh.script 				- Unix/Linux sh script or Windows batch script to execute to bild and produce artifact(s) - any valid sh or batch script
// args.sh.artifacts 			- a file pattern of artifact(s) to publish to Artifactory
//
// args.gradle					- Map of options for Gradle type of build
// args.gradle.tasks			- Gradle tasks string, i.e. something like 'clean assembleRelease'
// args.gradle.buildFile		- Gradle build file (default build.gradle)
// args.gradle.rootDir			- Root directory for the project (defaults to current ./ directory, i.e. workspace root)
// args.gradle.usesPlugin		- Whether or not the "com.jfrog.artifactory" Gradle Plugin is already applied in Gradle build script (default true)
//									NOTE: usesPlugin=false is broken as of version 2.16.2 of Artifactory plugin
//										  If you have to use that - good luck
// args.gradle.useWrapper		- Whether or not to use the Gradle Wrapper for this build (default false)
//
// args.dotnet							- Map of options for dotnet core type of build
// args.dotnet.projectfile				- dotnet project file to build, default empty (uses current directory - workspace root - as starting point)
// args.dotnet.configuration			- Defines the dotnet build configuration. The default value is Debug
// args.dotnet.nuget_config				- Nuget config file (contains encrypted keys to communicate with Artifactory), default "${NUGET_HOME}/nuget.config"
// args.dotnet.build_options			- dotnet build command line options, default empty
// args.dotnet.test_options				- dotnet test command line options, default empty
// args.dotnet.restore_options			- dotnet restore command line options, default '--no-cache'
// args.dotnet.publish_options			- dotnet publish command line options, default '--no-build --no-restore'
// args.dotnet.sonar					- SonarQube settings for the project
// args.dotnet.sonar.key				- SonarQube key for the project, defaults to 'com.acme.'+env.JOB_NAME.split("/").first().replace(" ", "")
// args.dotnet.sonar.name				- SonarQube name for the project, defaults to env.JOB_NAME.split("/").first()
//
// NOTE: If multiple build types were passed in - only the first one will be processed and the rest will be ignored
//
// Common Options for all types:
//
// args.maxBuilds				- number of builds to keep in Artifactory (optional, default 10)
// args.publishArtifacts		- whether or not publish built artifacts to Artifactory (optional, default true, will be overwritten for some branch types)
// args.ciSkipPush				- whether or not commit & push files modified by build process to origin git repo (optional, default true, will be overwritten for some branch types)
// args.config					- your specific environment config from config.groovy global
//
// Dependencies:
// + Must have Artifactory plugin
// + Must have all build tools installed and available either via Jenkins Global Tool Configuration or directly on the nodes (master and/or slaves as necessary) or combination of both methods

def call(String type) {
	def args = [:]
	args[type] = [:]
	call args
}

def call(Map args = [:]) {
	// initialize : determine build type, get effective args by combining defaults with actuals if passed in the call, create/set required objects
	def effectiveArgs = init(args)
	// branch out on branch :)
	branchout(effectiveArgs)
}

def getDefaults(String type) {
	def args = [
		maxBuilds: 10,
		publishArtifacts: true,
		ciSkipPush: true,
		config: config()
	]
	switch (type) {
		case ~/^maven$/:
			args.maven = [
				pom: 'pom.xml', 
				goals: 'clean install',
				failOnSnapshot: false
			]
			break
		case ~/^grunt$/:
			args.grunt = [
				script: '''
					npm install
					npm update
					grunt build
					grunt package
				''',
				artifacts: '*.tgz'
			]
			break
		case ~/^sh$/:
			args.sh = [
				script: 'echo "Your Script Here"',
				artifacts: 'your-artifacts-here',
				defaultVersion: '0-SNAPSHOT',
				windows_type: 'bat'
			]
			break
		case ~/^gradle$/:
			args.gradle = [
				tasks: 'clean assembleRelease',
				buildFile: 'build.gradle',
				rootDir: './',
				usesPlugin: true,
				useWrapper: false
			]
			break
		case ~/^dotnet$/:
			args.dotnet = [
				projectfile: '',
				configuration: 'Debug',
				nuget_config: "${NUGET_HOME}/nuget.config",
				restore_options: '--no-cache',
				build_options: '',
				test_options: '',
				publish_options: '--no-build --no-restore',
				sonar: [
					key: 'com.acme.'+env.JOB_NAME.split("/").first().replace(" ", ""),
					name: env.JOB_NAME.split("/").first()
				]
			]
			break
		default:
			ansiColor('xterm') {
				error "\u001B[1;31mUsupported build type: ${type}\u001B[0m"
			}
			break
	}
	return args 
}

def init(Map args = [:]) {
	// determine build type
	// Only process first build type passed in the arguments, ignore the rest if any
	def buildType = args.find{ ['maven','grunt','sh','gradle','dotnet'].contains(it.key) }?.key

	// Get effective Args, starting from default ones as a base and applying passed in arguments if any
	def effectiveArgs = mergeMaps(getDefaults(buildType), args)
	
	effectiveArgs.artifactory = Artifactory.server(effectiveArgs.config.artifactory_server_id)
	effectiveArgs.buildInfo = Artifactory.newBuildInfo()
	effectiveArgs.buildInfo.retention maxBuilds: effectiveArgs.maxBuilds, deleteBuildArtifacts: true, async: true
	
	effectiveArgs?.maven && {
		effectiveArgs.maven.mavenBuild = Artifactory.newMavenBuild()
		effectiveArgs.maven.descriptor = Artifactory.mavenDescriptor()
		effectiveArgs.maven.mavenBuild.resolver server: effectiveArgs.artifactory, releaseRepo: effectiveArgs.config.artifactory_resolver_maven_release_repo, snapshotRepo: effectiveArgs.config.artifactory_resolver_maven_snapshot_repo
		effectiveArgs.maven.mavenBuild.deployer server: effectiveArgs.artifactory, releaseRepo: effectiveArgs.config.artifactory_deployer_maven_release_repo, snapshotRepo: effectiveArgs.config.artifactory_deployer_maven_snapshot_repo
	}()
	
	effectiveArgs?.grunt && {
		// default to shapshot, some branches will change it to release
		effectiveArgs.grunt.uploadSpec = getUploadSpec type: 'snapshot', pattern: effectiveArgs.grunt.artifacts, config: effectiveArgs.config
	}()
	
	effectiveArgs?.sh && {
		// default to shapshot, some branches will change it to release
		effectiveArgs.sh.uploadSpec = getUploadSpec type: 'snapshot', pattern: effectiveArgs.sh.artifacts, config: effectiveArgs.config
	}()
	
	effectiveArgs?.gradle && {
		effectiveArgs.gradle.gradleBuild = Artifactory.newGradleBuild()
		effectiveArgs.gradle.gradleBuild.resolver server: effectiveArgs.artifactory, repo: effectiveArgs.config.artifactory_resolver_gradle_repo
		// snapshot by default, will be chnaged to release repo for release/hotfix branches and tags on master
		effectiveArgs.gradle.gradleBuild.deployer server: effectiveArgs.artifactory, repo: effectiveArgs.config.artifactory_deployer_gradle_snapshot_repo
		effectiveArgs.gradle.gradleBuild.usesPlugin = effectiveArgs.gradle.usesPlugin
		effectiveArgs.gradle.gradleBuild.useWrapper = effectiveArgs.gradle.useWrapper
	}()
	
	effectiveArgs?.dotnet && {
		// must run on Windows only (for now at least)
		// TODO - add support for Unix/Linux
		if(isUnix()) {
			error "dotnet is not supported on this node: ${NODE_NAME}"
		} else {
			// setup nuget
			if (!fileExists(file: effectiveArgs.dotnet.nuget_config)) {
				// not found - get or create one
				effectiveArgs.dotnet.nuget_config = configure_nuget(effectiveArgs)
			}
			// setup sonar-scanner for dotnet
			bat """
				IF NOT EXIST "%BASE%\\tools" mkdir "%BASE%\\tools"
		        IF NOT EXIST "%BASE%\\tools\\dotnet-sonarscanner" mkdir "%BASE%\\tools\\dotnet-sonarscanner"
		        set PATH="%BASE%\\tools\\dotnet-sonarscanner";%PATH%
		        dotnet tool install --tool-path "%BASE%\\tools\\dotnet-sonarscanner" dotnet-sonarscanner --configfile "${effectiveArgs.dotnet.nuget_config}" 
			"""
			// initialize SKIP_CODE_ANALYSIS=false environment variable if not present, otherwise build script will fail
			// this is specific to dotnet only as the SonarQube scanner must be executed as part of the build and not as a separate stage after 
			env.SKIP_CODE_ANALYSIS = env.SKIP_CODE_ANALYSIS ?: "false"
		}
	}()
	
	return effectiveArgs
}

def branchout(Map args) {
	switch (env.BRANCH_NAME) {
		case ~/^develop$/:
			return develop(args)
			break
		case ~/^master$/:
			return master(args)
			break
		case ~/^release-.*$/:
			return release(args)
			break
		case ~/^hotfix-.*$/:
			return hotfix(args)
			break
		case ~/^\d+\.\d+\.\d+$/:
			// assume tag
			return tag(args)
			break
		default:
			// assume feature branch
			return feature(args)
			break
	}
}

def develop(Map args) {
	args.ciSkipPush = false
	build(args)
}

def feature(Map args) {
	disableTriggers()  // disable all triggers in feature branches
	args.publishArtifacts = false
	build(args)
}

def master(Map args) {
	disableTriggers()  // just in case. should have been disabled in release/hotfix branch and then merged to master
	args.publishArtifacts = false
	build(args)
}

def tag(Map args) {
	// verify that we've got a "good" tag, i.e. something like 1.4.0 or 1.4.1, etc
	if ( env.TAG_NAME ==~ /^\d+\.\d+\.\d+$/ ) {
		args.publishArtifacts = false // should we? TODO: re-evaluate this
		releaseNhotfix(args)
	} else {
		error "Invalid tag name ${TAG_NAME}"
	}
}

def release(Map args) {
	// verify that we've got a "good" release version, i.e. something like 1.4.0 (zero at the end) for release
	if ( env.BRANCH_NAME ==~ /^release-\d+\.\d+\.0$/ ) {
		releaseNhotfix(args)
	} else {
		error "Invalid version format in release branch name ${BRANCH_NAME}"
	}
}

def hotfix(Map args) {
	// verify that we've got a "good" hotfix version, i.e. something like 1.4.1 (not a zero at the end) for hotfix
	if ( env.BRANCH_NAME ==~ /^hotfix-\d+\.\d+\.[1-9]+$/ ) {
		releaseNhotfix(args)
	} else {
		error "Invalid version format in hotfix branch name ${BRANCH_NAME}"
	}
}

def releaseNhotfix(Map args) {
	// disable triggers{} section in Jenkinsfile. Dont need any triggers for release, hotfix branches or tags on master
	disableTriggers()
	
	// extract version from branch or tag name
	if ( env.BRANCH_NAME ==~ /\w-(.*)$/ ) {
		// release or hotfix branch
		args.appVersion = (env.BRANCH_NAME =~ /\w-(.*)$/)[ 0 ][ 1 ]
	} else {
		// tag (must be on master)
		args.appVersion = (env.BRANCH_NAME =~ /^(.*)$/)[ 0 ][ 1 ]
	}
	// set upload spec to release type for grunt and sh builds
	args?.grunt && { args.grunt.uploadSpec = getUploadSpec([type:'release', pattern:args.grunt.artifacts], config: args.config) }()
	args?.sh && { args.sh.uploadSpec = getUploadSpec([type:'release', pattern:args.sh.artifacts], config: args.config) }()

	// do NOT allow any dependencies for the release/hotfix build to be at SNAPSHOT versions for Maven builds
	args?.maven && { args.maven.failOnSnapshot = true }()

	// change the deployer repository for gradle to release one	
	args?.gradle && { 
		args.gradle.gradleBuild.deployer server: args.artifactory, repo: 'gradle-release-local'
		// Gradle resolver and deployer are broken as of version 2.16.2, so we have to use environment variables to pass repository name for release builds to the Gradle build script
		// see: https://www.jfrog.com/jira/browse/HAP-881
		env.GRADLE_PUBLISH_REPO = 'gradle-release-local'
	}()
	
	// enforce Configuration=Release for dotnet
	args?.dotnet && { args.dotnet.configuration = "Release"	}()
	
	// build
	build(args)
}

def build(Map args) {
	// build
	args?.maven && build_maven(args)
	args?.grunt && build_grunt(args)
	args?.sh && build_sh(args)
	args?.gradle && build_gradle(args)
	args?.dotnet && build_dotnet(args)
	// commit & push changes made by the build to source files to to origin git repo if enabled
	args.ciSkipPush && ciSkipPush()
}

def getUploadSpec(Map args) {
	def repository = args.type == 'release' ? args.config.artifactory_deployer_generic_release_repo : args.config.artifactory_deployer_generic_snapshot_repo 
	
	return 	"""{
				"files": 
				[
			    	{
			      		"pattern": "${args.pattern}",
						"target": "${repository}/${JOB_NAME}/"
					}
				]
			}"""
}

def configure_nuget(Map args) {
	// configure nuget if not already configured
	// TODO - make it work on Linux/Unix as well
	def nuget_config = ""
	if (!fileExists(file: "${NUGET_HOME}/nuget.config")) {
		// could not find that either - do we have environment set?
		if(env.NUGET_HOME) {
			// environment set, lets create default nuget.config
			echo "Could not find neither requested Nuget config file, or default ${NUGET_HOME}/nuget.config"
			echo "Creating default ${NUGET_HOME}/nuget.config"
			withCredentials([usernamePassword(credentialsId: args.config.artifactory_credentials_id, passwordVariable: 'ARTIFACTORY_KEY', usernameVariable: 'ARTIFACTORY_USER')]) {
				bat """
						(
						echo ^<?xml version="1.0" encoding="utf-8"?^>
						echo ^<configuration^>
						echo ^<packageSources^>
						echo ^<add key="Artifactory" value="${args.artifactory.url}/api/nuget/nuget" /^>
						echo ^</packageSources^>
						echo ^</configuration^>
						) > ${NUGET_HOME}/nuget.config
						nuget sources Update -Name Artifactory -username %ARTIFACTORY_USER% -password %ARTIFACTORY_KEY% -ConfigFile ${NUGET_HOME}/nuget.config -Verbosity quiet
						nuget setapikey %ARTIFACTORY_USER%:%ARTIFACTORY_KEY% -Source Artifactory -ConfigFile ${NUGET_HOME}/nuget.config -Verbosity quiet
					""" 
			}
			// set the args to the default nuget.config just created
			nuget_config = "${NUGET_HOME}/nuget.config"
		} else {
			error("Environment variable NUGET_HOME is undefined")
		}
	} else {
		// found default one - lets use it
		echo "Could not find requested Nuget config file, using default ${NUGET_HOME}/nuget.config instead"
		nuget_config = "${NUGET_HOME}/nuget.config"
	}
	return nuget_config
}

def build_maven(Map args) {
	args.maven.descriptor.failOnSnapshot = args.maven.failOnSnapshot
	args.maven.mavenBuild.deployer.deployArtifacts = args.publishArtifacts
	// set maven version (will change the pom file)
	if (args.appVersion) {
		args.maven.descriptor.version = args.appVersion
		args.maven.descriptor.transform()
	}
	// maven build
	def buildInfo = args.maven.mavenBuild.run pom: args.maven.pom, goals: args.maven.goals
	buildInfo.append args.buildInfo
	// publish build info to Artifactory
	args.publishArtifacts && { args.artifactory.publishBuildInfo buildInfo }()
}

def build_grunt(Map args) {
	if (args.appVersion) {
		// set buildInfo.number for Artifactory to appVersion
		args.buildInfo.number = args.appVersion
		// set version inside package.json file(s) (if any present) to appVersion
		pkgJsonVer(args.appVersion)
	}
	// execute build script
	if(isUnix()) {
		sh args.grunt.script
	} else {
		bat args.grunt.script
	}
	if (args.publishArtifacts) {
		// publish artifact(s) to Artifactory
		args.artifactory.upload spec: args.grunt.uploadSpec, buildInfo: args.buildInfo
		// publish build info
		args.artifactory.publishBuildInfo args.buildInfo
	}
}

def build_sh(Map args) {
	args.appVersion = args.appVersion ?: args.sh.defaultVersion
	// set buildInfo.number for Artifactory to appVersion
	args.buildInfo.number = args.appVersion
	env.BUILD_APP_VERSION = args.appVersion
	
	// execute script
	if(isUnix()) {
		sh args.sh.script
	} else {
		if (args.sh.windows_type == 'bat') {
			bat args.sh.script
		} else {
			powershell args.sh.script
		}
	}
	
	if (args.publishArtifacts) {
		// publish artifact(s) to Artifactory
		args.artifactory.upload spec: args.sh.uploadSpec, buildInfo: args.buildInfo
		// publish build info
		args.artifactory.publishBuildInfo args.buildInfo
	}
}

def build_gradle(Map args) {
	// For gradle we need to add artifactoryPublish task to the task list if publishing of artifacts is requested
	// unless already there or usesPlugin=false, since in that case it will be added automatically
	if (args.publishArtifacts && args.gradle.gradleBuild.usesPlugin) {
		args.gradle.gradleBuild.deployer.deployArtifacts = args.publishArtifacts
		if (args.gradle.tasks !=~ /.*artifactoryPublish.*/ ) {
			args.gradle.tasks += " artifactoryPublish"
		}
	}
	// set gradle version
	if (args.appVersion) {
		// set buildInfo.number for Artifactory to appVersion
		args.buildInfo.number = args.appVersion
		// set appVersion to environment variable
		env.BUILD_APP_VERSION = args.appVersion
	}
	// Since Artifactory plugin is broken for gradle as of version 2.16.2, we have to set username and password into environment variables
	withCredentials([usernamePassword(credentialsId: args.config.artifactory_credentials_id, passwordVariable: 'GRADLE_REPO_PWD', usernameVariable: 'GRADLE_REPO_USER')]) {
		// gradle build
		def buildInfo = args.gradle.gradleBuild.run rootDir: args.gradle.rootDir, buildFile: args.gradle.buildFile, tasks: args.gradle.tasks
		buildInfo.append args.buildInfo
		// publish build info to Artifactory
		args.publishArtifacts && { args.artifactory.publishBuildInfo buildInfo }()
	}
}

def build_dotnet(Map args) {
	// TODO - add support for Unix/Linux
	// set the app/build version
	if (args.appVersion) {
		// set buildInfo.number for Artifactory to appVersion
		args.buildInfo.number = args.appVersion
		// set Version option unless already set for some bizzare reason (should never happen, but still)
		if (args.dotnet.build_options !=~ /.*-p:Version.*/ ) {
			args.dotnet.build_options += " -p:Version=" + args.appVersion
		}
	}

	// build
	// this is specific to dotnet only as the SonarQube scanner must be executed as part of the build and not as a separate stage after
	withSonarQubeEnv(args.config.sonarqube_installation_name) { 
		// TODO - add code coverage
		bat """
			IF /I NOT %SKIP_CODE_ANALYSIS%==true dotnet sonarscanner begin /k:${args.dotnet.sonar.key} /n:${args.dotnet.sonar.name} /v:"${env.BRANCH_NAME}-${env.BUILD_NUMBER}" /d:sonar.branch.name=${env.BRANCH_NAME} /d:sonar.host.url=${env.SONAR_HOST_URL} /d:sonar.login=${env.SONAR_AUTH_TOKEN}
			dotnet restore ${args.dotnet.projectfile} --configfile "${args.dotnet.nuget_config}"
			dotnet build ${args.dotnet.projectfile} -c ${args.dotnet.configuration} --configfile "${args.dotnet.nuget_config}" ${args.dotnet.build_options}
			dotnet test ${args.dotnet.projectfile} -c ${args.dotnet.configuration} ${args.dotnet.test_options}
			IF /I NOT %SKIP_CODE_ANALYSIS%==true dotnet sonarscanner end /d:sonar.login=${env.SONAR_AUTH_TOKEN}
		""" 
	}
	// pack and publish
	bat """
		dotnet publish ${args.dotnet.projectfile} -c ${args.dotnet.configuration} ${args.dotnet.publish_options}
	"""
	// TODO - nuget push
}