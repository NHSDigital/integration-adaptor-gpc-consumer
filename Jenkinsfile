pipeline {
    agent{
        label 'jenkins-workers'
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: "10"))
    }

    environment {
        BUILD_TAG = sh label: 'Generating build tag', returnStdout: true, script: 'python3 scripts/tag.py ${GIT_BRANCH} ${BUILD_NUMBER} ${GIT_COMMIT}'
        ECR_REPO_DIR = "gpc-consumer"
        DOCKER_IMAGE = "${DOCKER_REGISTRY}/${ECR_REPO_DIR}:${BUILD_TAG}"
    }

    stages {
        stage('Build') {
            stages {
                stage('Tests') {
                    steps {
                        script {
                            sh '''
                                source docker/vars.local.sh
                                docker network create commonforgpc || true
                                docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml build
                                docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml up --exit-code-from gpc-consumer
                            '''
                        }
                    }
                    post {
                        always {
                            sh "docker cp tests:/home/gradle/service/build ."
                            archiveArtifacts artifacts: 'build/reports/**/*.*', fingerprint: true
                            junit '**/build/test-results/**/*.xml'
                            recordIssues(
                                enabledForFailure: true,
                                tools: [
                                    checkStyle(pattern: 'build/reports/checkstyle/*.xml'),
                                    spotBugs(pattern: 'build/reports/spotbugs/*.xml')
                                ]
                            )
                            // Disable JacocoPublisher for now, as our Jenkins doesn't support Java 17
                            // See NIAD-3022 for more details
                            // step([
                            //     $class : 'JacocoPublisher',
                            //     execPattern : '**/build/jacoco/*.exec',
                            //     classPattern : '**/build/classes/java',
                            //     sourcePattern : 'src/main/java',
                            //     exclusionPattern : '**/*Test.class'
                            // ])
                            sh "rm -rf build"
                            sh "docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml down"
                            sh "docker network rm commonforgpc || true"
                        }
                    }
                }

                stage('Build Docker Images') {
                    steps {
                        script {
                            if (sh(label: 'Running gpc-consumer docker build', script: 'docker build -f docker/service/Dockerfile -t ${DOCKER_IMAGE} .', returnStatus: true) != 0) {error("Failed to build gpc-consumer Docker image")}
                        }
                    }
                }

                stage('Push Image') {
                    when {
                        expression { currentBuild.resultIsBetterOrEqualTo('SUCCESS') }
                    }
                    steps {
                        script {
                            if (ecrLogin(TF_STATE_BUCKET_REGION) != 0 )  { error("Docker login to ECR failed") }
                            String dockerPushCommand = "docker push ${DOCKER_IMAGE}"
                            if (sh (label: "Pushing image", script: dockerPushCommand, returnStatus: true) !=0) { error("Docker push gpc-consumer image failed") }
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            sh label: 'Remove images created by docker-compose', script: 'docker-compose -f docker/docker-compose.yml -f docker/docker-compose-tests.yml down --rmi local'
            sh label: 'Remove exited containers', script: 'docker rm $(docker ps -a -f status=exited -q)'
            sh label: 'Remove images tagged with current BUILD_TAG', script: 'docker image rm -f $(docker images "*/*:*${BUILD_TAG}" -q) $(docker images "*/*/*:*${BUILD_TAG}" -q) || true'
        }
    }
}

int ecrLogin(String aws_region) {
    String dockerLogin = "aws ecr get-login-password --region ${aws_region} | docker login -u AWS --password-stdin \"https://\$(aws sts get-caller-identity --query 'Account' --output text).dkr.ecr.${aws_region}.amazonaws.com\""
    return sh(label: "Logging in with Docker", script: dockerLogin, returnStatus: true)
}
