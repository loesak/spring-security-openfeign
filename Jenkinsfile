pipeline {
    agent any
    stages {
        stage('Maven Clean Build & Deploy') {
            steps {
                withMaven(
                        maven: 'maven-3.6.1',
                        mavenOpts: '-Xmx256m',
                        globalMavenSettingsConfig: 'fbc6b0e6-dd00-4a30-8ffd-f3ac375e5fbf',
                        jdk: 'openjdk-11') {
                    sh "mvn clean deploy"
                }
            }
            post {
                success {
                    slackSend channel: '#jenkins',
                            color: 'good',
                            message: "BUILD SUCCESS: ${currentBuild.fullDisplayName}\n${currentBuild.absoluteUrl}"
                }
                unstable {
                    slackSend channel: '#jenkins',
                            color: 'warning',
                            message: "BUILD UNSTABLE: ${currentBuild.fullDisplayName}\n${currentBuild.absoluteUrl}"
                }
                failure {
                    slackSend channel: '#jenkins',
                            color: 'danger',
                            message: "BUILD FAILURE: ${currentBuild.fullDisplayName}\n${currentBuild.absoluteUrl}"
                }
            }
        }
    }
}