class SonarScanner {
    Script script

    SonarScanner(script) {
        this.script = script
    }

    def sendSonarScanWithCredentials(workDir, credentialsId, scriptText) {
        script.dir("${workDir}") {
            script.withCredentials([script.usernamePassword(credentialsId: "${credentialsId}",
                passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                    script.withSonarQubeEnv('Sonar') {
                        script.sh scriptText.replace("LOGIN_REPLACE", "${script.USERNAME}")
                                            .replace("PASSWORD_REPLACE", "${script.PASSWORD}");
                    }
            }
        }
    }

    def sendSonarScanWithoutCredentials(workDir, scriptText) {
        script.dir("${workDir}") {
            script.withSonarQubeEnv('Sonar') {
                script.sh scriptText
            }
        }
    }

    def waitForQualityGate() {
        script.timeout(time: 10, unit: 'MINUTES') {
            def qualityGateResult = script.waitForQualityGate()
            if (qualityGateResult.status != 'OK')
                script.error "[JENKINS][ERROR] Sonar quality gate check has failed with status " +
                        "${qualityGateResult.status}"
        }
    }

    def waitForSonarAnalysis(ceTaskUrl) {
        script.println("[JENKINS][DEBUG] Waiting for report from Sonar")
        script.timeout(time: 10, unit: 'MINUTES') {
            while (true) {
                def status = getStatus(ceTaskUrl)
                script.println("[JENKINS][DEBUG] Current status: ${status}")

                if (status == 'FAILED') {
                    script.error "[JENKINS][ERROR] Sonar analysis finished with status: \'${status}\'"
                }

                if (status == 'SUCCESS') {
                    script.println("[JENKINS][ERROR] Sonar analysis finished with status: ${status}")
                    break
                }
            }
        }
    }

    def getStatus(ceTaskUrl) {
        script.withSonarQubeEnv('Sonar') {
            def sonarAuthHeader="${script.env.SONAR_AUTH_TOKEN}:".bytes.encodeBase64().toString()
            def response = script.httpRequest acceptType: 'APPLICATION_JSON',
                    url: ceTaskUrl,
                    httpMode: 'GET',
                    customHeaders: [[name: 'Authorization', value: "Basic ${sonarAuthHeader}"]],
                    quiet: true

            def content = script.readJSON text: response.content
            return content.task.status
        }
    }

    def cleanSonarProjectRange(patchsetNumber, sonarRoute, sonarProjectKey, sonarAuthHeader) {
        for (int i = 1; i <= (patchsetNumber as Integer) ; i++) {
            def response = script.httpRequest url: "${sonarRoute}/api/components/show?component=${sonarProjectKey}-${i}",
                    httpMode: 'GET',
                    customHeaders: [[name: 'Authorization', value: "Basic ${sonarAuthHeader}"]],
                    validResponseCodes: '100:399,404'
            if (response.status == 200) {
                script.httpRequest url: "${sonarRoute}/api/projects/delete?project=${sonarProjectKey}-${i}",
                        httpMode: 'POST',
                        customHeaders: [[name: 'Authorization', value: "Basic ${sonarAuthHeader}"]]
                script.println("[JENKINS][DEBUG] Project ${sonarProjectKey}-${i} deleted")
            }
        }
    }
}
