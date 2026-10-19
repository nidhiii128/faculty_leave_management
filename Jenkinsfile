// Faculty Leave Management System — CI/CD pipeline (pipeline as code).
// Requires on the agent: JDK 17+, Maven, curl, Docker CLI (for the Docker stages).
// Quality gate: any failing unit or Selenium test fails the build, so the deploy stages never run.
// Credentials: "tomcat-manager" (Tomcat deploy), "dockerhub-creds" (registry push) — both Username with password.

pipeline {
    agent any

    parameters {
        choice(name: 'APP_ENV', choices: ['dev', 'staging', 'prod'],
               description: 'Environment name baked into the build and shown in the app header')
        choice(name: 'DEPLOY_TARGET', choices: ['docker', 'tomcat', 'none'],
               description: 'Where to deploy after the tests pass')
        choice(name: 'BROWSER', choices: ['htmlunit', 'chrome', 'firefox'],
               description: 'Browser for the Selenium suite (chrome/firefox must be installed on the agent)')
        string(name: 'REGISTRY', defaultValue: 'localhost:5000',
               description: 'Image registry: localhost:5000 (local registry) or docker.io/<dockerhub-user>')
        booleanParam(name: 'PUSH_IMAGE', defaultValue: false,
                     description: 'Push the versioned image to REGISTRY')
        string(name: 'APP_PORT', defaultValue: '8090',
               description: 'Host port for the deployed container')
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
        IMAGE = "${params.REGISTRY}/flms"
        CONTAINER = "flms-${params.APP_ENV}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log -1 --oneline'
                script {
                    def pomVersion = sh(returnStdout: true,
                        script: "${MVN} -q help:evaluate -Dexpression=project.version -DforceStdout").trim()
                    // e.g. 1.1.0-42: semantic version from pom + Jenkins build number
                    env.IMAGE_TAG = "${pomVersion}-${env.BUILD_NUMBER}"
                    currentBuild.displayName = "#${env.BUILD_NUMBER} ${env.IMAGE_TAG} ${params.APP_ENV}"
                }
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

        stage('Docker image') {
            when { expression { params.DEPLOY_TARGET == 'docker' || params.PUSH_IMAGE } }
            steps {
                sh '''
                    docker build --build-arg VERSION="$IMAGE_TAG" -t "$IMAGE:$IMAGE_TAG" -t "$IMAGE:latest" .
                    docker image inspect "$IMAGE:$IMAGE_TAG" --format 'Built {{.RepoTags}} {{.Size}} bytes'
                '''
            }
        }

        stage('Push image') {
            when { expression { params.PUSH_IMAGE } }
            steps {
                script {
                    if (params.REGISTRY.startsWith('localhost')) {
                        sh 'docker push "$IMAGE:$IMAGE_TAG" && docker push "$IMAGE:latest"'
                    } else {
                        withCredentials([usernamePassword(credentialsId: 'dockerhub-creds',
                                                          usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
                            sh '''
                                echo "$REG_PASS" | docker login -u "$REG_USER" --password-stdin
                                docker push "$IMAGE:$IMAGE_TAG"
                                docker push "$IMAGE:latest"
                                docker logout
                            '''
                        }
                    }
                }
            }
        }

        stage('Deploy container') {
            when { expression { params.DEPLOY_TARGET == 'docker' } }
            steps {
                sh '''
                    docker rm -f "$CONTAINER" 2>/dev/null || true
                    docker run -d --name "$CONTAINER" \
                      -p "$APP_PORT:8080" \
                      -e APP_ENV="$APP_ENV" \
                      -v "$CONTAINER-data:/app/data" \
                      --restart unless-stopped \
                      "$IMAGE:$IMAGE_TAG"
                '''
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
            when { expression { params.DEPLOY_TARGET != 'none' } }
            steps {
                script {
                    if (params.DEPLOY_TARGET == 'docker') {
                        // Uses the image's HEALTHCHECK, so it works whatever the Docker network layout is
                        sh '''
                            for i in $(seq 1 40); do
                              status=$(docker inspect -f '{{.State.Health.Status}}' "$CONTAINER")
                              echo "health: $status"
                              if [ "$status" = "healthy" ]; then
                                docker exec "$CONTAINER" wget -qO- http://localhost:8080/actuator/info; echo
                                exit 0
                              fi
                              sleep 3
                            done
                            docker logs --tail 80 "$CONTAINER"; exit 1
                        '''
                    } else {
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
        }
    }

    post {
        success {
            echo "Build ${env.IMAGE_TAG} succeeded (${params.APP_ENV}, target=${params.DEPLOY_TARGET})"
        }
        failure {
            echo "Build ${env.BUILD_NUMBER} FAILED — check the stage logs, test reports and archived Selenium evidence"
        }
    }
}
