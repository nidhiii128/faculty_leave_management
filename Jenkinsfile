// Faculty Leave Management System — CI/CD pipeline (pipeline as code).
// Requires on the agent: JDK 17+, Maven, curl.
// Quality gate: any failing unit or Selenium test fails the build, so the deploy stages never run.  Credentials: "tomcat-manager" (username/password).

pipeline {
    agent any

    parameters {
        choice(name: 'APP_ENV', choices: ['dev', 'staging', 'prod'],
               description: 'Environment name baked into the build and shown in the app header')
        choice(name: 'DEPLOY_TARGET', choices: ['tomcat', 'none'],
               description: 'Where to deploy after a successful build')
        choice(name: 'BROWSER', choices: ['htmlunit', 'chrome', 'firefox'],
               description: 'Browser for the Selenium suite (chrome/firefox must be installed on the agent)')
        string(name: 'TOMCAT_URL', defaultValue: 'http://tomcat:8080',
               description: 'Base URL of the Tomcat server (manager app enabled)')
    }

    triggers {
        // Poll GitHub every ~5 minutes; replace with a GitHub webhook when Jenkins is reachable from the internet
        pollSCM('H/5 * * * *')
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
        timeout(time: 30, unit: 'MINUTES')
    }

    environment {
        MVN = 'mvn -B -ntp'
        CONTEXT_PATH = '/flms'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
            }
        }

        stage('Build') {
            steps {
                sh "${MVN} clean compile -Dapp.env=${params.APP_ENV}"
            }
        }

        stage('Unit tests') {
            steps {
                sh "${MVN} test -Dapp.env=${params.APP_ENV}"
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Selenium quality gate') {
            steps {
                // Starts the app on a random port and runs the WebDriver journeys (headless HtmlUnit by default)
                sh "${MVN} verify -DskipUnitTests -Dbrowser=${params.BROWSER} -Dapp.env=${params.APP_ENV}"
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml', allowEmptyResults: false
                    archiveArtifacts artifacts: 'target/selenium-screenshots/**', allowEmptyArchive: true
                }
            }
        }

        stage('Package') {
            steps {
                sh "${MVN} package -DskipTests -Dapp.env=${params.APP_ENV}"
                archiveArtifacts artifacts: 'target/flms.war', fingerprint: true
            }
        }

        stage('Deploy to Tomcat') {
            when { expression { params.DEPLOY_TARGET == 'tomcat' } }
            steps {
                withCredentials([usernamePassword(credentialsId: 'tomcat-manager',
                                                  usernameVariable: 'TC_USER', passwordVariable: 'TC_PASS')]) {
                    sh '''
                        curl --fail -sS -u "$TC_USER:$TC_PASS" -T target/flms.war \
                          "$TOMCAT_URL/manager/text/deploy?path=$CONTEXT_PATH&update=true"
                    '''
                }
            }
        }

        stage('Smoke test') {
            when { expression { params.DEPLOY_TARGET == 'tomcat' } }
            steps {
                sh '''
                    for i in $(seq 1 30); do
                      if curl -fsS "$TOMCAT_URL$CONTEXT_PATH/actuator/health" | grep -q '"UP"'; then
                        echo "Application is UP"; curl -sS "$TOMCAT_URL$CONTEXT_PATH/actuator/info"; exit 0
                      fi
                      sleep 2
                    done
                    echo "Application did not become healthy"; exit 1
                '''
            }
        }
    }

    post {
        success { echo "Build ${env.BUILD_NUMBER} succeeded (${params.APP_ENV})" }
        failure { echo "Build ${env.BUILD_NUMBER} FAILED — check the stage logs and test reports" }
    }
}
