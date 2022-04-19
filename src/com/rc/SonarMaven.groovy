import com.rc.ProjectType
import com.rc.Stage
import com.rc.SonarScanner
import org.apache.commons.lang.RandomStringUtils

@Stage(name = "sonar", buildTool = "maven", type = [ProjectType.APPLICATION, ProjectType.AUTOTESTS, ProjectType.LIBRARY])
class SonarMaven {
    Script script

    void run(context) {
        def codereviewAnalysisRunDir = context.workDir
        if (context.job.type == "codereview") {
            codereviewAnalysisRunDir = new File("${context.workDir}/../${RandomStringUtils.random(10, true, true)}")

            script.dir("${codereviewAnalysisRunDir}") {
                if (script.fileExists("${context.workDir}/target")) {
                    script.println("[JENKINS][DEBUG] Project with usual structure")
                    script.sh """
                      export LANG=en_US.utf-8
                      cd ${context.workDir}
                      git config --local core.quotepath false
                      IFS=\$'\\n';for i in \$(git diff --diff-filter=ACMR --name-only origin/${context.git.branch}); \
                        do cp --parents \"\$i\" ${codereviewAnalysisRunDir}/; echo "file for scanner:" \"\$i\"/; done
                      cp -f pom.xml ${codereviewAnalysisRunDir}/
                      [ -d "src/test/" ] &&  cp --parents -r src/test/ ${codereviewAnalysisRunDir} || echo "Directory src/test/ not found."
                      cp --parents -r target/ ${codereviewAnalysisRunDir}
                      """
                } else {
                    script.println("[JENKINS][DEBUG] Multi-module project")
                    script.sh """
                      mkdir -p ${codereviewAnalysisRunDir}/unittests
                      cd ${context.workDir}
                      IFS=\$'\\n';for i in \$(git diff --diff-filter=ACMR --name-only origin/${context.git.branch}); \
                        do cp --parents \"\$i\" ${codereviewAnalysisRunDir}/; echo "file for scanner:" \"\$i\"/; done
                      for directory in `find . -type d -name \'test\'`; do cp --parents -r \${directory} \
                      ${codereviewAnalysisRunDir}/unittests; done
                      for poms in `find . -type f -name \'pom.xml\'`; do cp --parents -r \${poms} \
                      ${codereviewAnalysisRunDir}; done
                      for targets in `find . -type d -name \'target\'`; do cp --parents -r \${targets} \
                      ${codereviewAnalysisRunDir}; done
                      """
                }
            }
        }
        SonarScanner sonarScanner = new SonarScanner(script);
        def buildTool = context.buildTool;
        def path = "target/sonar";
        def credentialsId = context.nexus.credentialsId;
        def codebaseName;
        def workDir;
        if (context.job.type == "codereview" && context.codebase.config.strategy != "import") {
            codebaseName = "${context.codebase.name}-${context.git.normalizedBranch}:change-${context.git.changeNumber}-${context.git.patchsetNumber}";
            workDir = codereviewAnalysisRunDir;
        } else {
            codebaseName = "${context.codebase.name}-${context.git.normalizedBranch}";
            workDir = context.workDir;
        }
        def scriptText = """ ${buildTool.command} ${buildTool.properties} -Dartifactory.username=LOGIN_REPLACE -Dartifactory.password=PASSWORD_REPLACE \
                             sonar:sonar -Dsonar.projectKey=${codebaseName} \
                             -Dsonar.projectName=${codebaseName} """;
        if (context.job.type == "build") {
            new SonarCleanup(script: script).run(context)
        }
        if (context.job.type == "codereview" && context.codebase.config.strategy != "import") {
            sonarScanner.sendSonarScanWithCredentials(workDir, credentialsId, scriptText)

            def report = script.readProperties file: "${workDir}/${path}/report-task.txt"
            def ceTaskUrl = report.ceTaskUrl
            sonarScanner.waitForSonarAnalysis(ceTaskUrl)

            sonarScanner.waitForQualityGate()
            return
        }
        sonarScanner.sendSonarScanWithCredentials(workDir, credentialsId, scriptText)
        sonarScanner.waitForQualityGate()
    }
}
