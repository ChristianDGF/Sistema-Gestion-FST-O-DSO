pipeline {
    agent any

    environment {
        GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true'
        FRONTEND_DIR = "${WORKSPACE}/frontend"
        IMAGE_TAG = "jenkins-${env.BUILD_NUMBER}"
        // Testcontainers' Ryuk reaper can't reconnect when the containers it spawns are
        // siblings of this one (Jenkins talks to the host's Docker via a mounted socket,
        // not real Docker-in-Docker). Testcontainers still stops/removes containers
        // normally on JVM exit - Ryuk is only a safety net for abrupt crashes.
        TESTCONTAINERS_RYUK_DISABLED = 'true'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Backend Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew compileJava --no-daemon --parallel'
            }
        }

        stage('Backend Tests (Unit, Contract, Integration, Data)') {
            steps {
                // Data tests (migrations, DB constraints, referential integrity, duplicate
                // data, seeds) run against a real Postgres Testcontainer, same as CI.
                sh './gradlew generateContractTests test jacocoTestReport jacocoTestCoverageVerification --no-daemon --parallel'
            }
            post {
                always {
                    junit 'build/reports/tests/**/TEST-*.xml'
                    publishHTML(
                        target: [
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'build/reports/jacoco/test/html',
                            reportFiles: 'index.html',
                            reportName: 'JaCoCo Coverage Report'
                        ]
                    )
                }
            }
        }

        stage('SonarCloud Analysis (Quality Gate)') {
            steps {
                // sonar.qualitygate.wait=true (build.gradle) makes this task itself fail if
                // the Quality Gate doesn't pass - no Jenkins-specific webhook/SonarQube
                // server setup needed, it's a property of the Gradle scanner.
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                    sh './gradlew sonar -Dsonar.token=${SONAR_TOKEN} --no-daemon'
                }
            }
        }

        stage('Frontend Install & Lint') {
            steps {
                dir("${FRONTEND_DIR}") {
                    sh 'npm ci --ignore-scripts'
                    sh 'npm run lint'
                }
            }
        }

        stage('Frontend Build') {
            steps {
                dir("${FRONTEND_DIR}") {
                    sh 'npm run build'
                }
            }
        }

        stage('Security Scan (SCA)') {
            steps {
                // NVD_API_KEY avoids rate limiting against the NVD; not fatal if absent.
                sh './gradlew dependencyCheckAnalyze --no-daemon'
                dir("${FRONTEND_DIR}") {
                    sh 'npm run audit'
                }
            }
            post {
                always {
                    publishHTML(
                        target: [
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'build/reports/dependency-check',
                            reportFiles: 'dependency-check-report.html',
                            reportName: 'Dependency-Check Report'
                        ]
                    )
                }
            }
        }

        stage('Docker Build & Push') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'ghcr-credentials', usernameVariable: 'GHCR_USER', passwordVariable: 'GHCR_TOKEN')]) {
                    sh 'echo $GHCR_TOKEN | docker login ghcr.io -u $GHCR_USER --password-stdin'
                }
                sh "docker build -t ghcr.io/christiandgf/sistema-gestion-backend:${IMAGE_TAG} ."
                sh "docker push ghcr.io/christiandgf/sistema-gestion-backend:${IMAGE_TAG}"
                sh "docker build -t ghcr.io/christiandgf/sistema-gestion-frontend:${IMAGE_TAG} ./frontend"
                sh "docker push ghcr.io/christiandgf/sistema-gestion-frontend:${IMAGE_TAG}"
            }
        }
    }

    post {
        always {
            archiveArtifacts(
                artifacts: 'build/libs/*.jar',
                fingerprint: true,
                allowEmptyArchive: true
            )
            archiveArtifacts(
                artifacts: 'frontend/dist/**',
                fingerprint: true,
                allowEmptyArchive: true
            )
            cleanWs()
        }
        success {
            echo 'Pipeline completed successfully.'
        }
        failure {
            echo 'Pipeline failed. Check the logs above for details.'
        }
    }
}
