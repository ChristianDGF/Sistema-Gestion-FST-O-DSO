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
        // Same sibling-container issue for the actual test containers (e.g. Postgres):
        // Testcontainers auto-detects 172.17.0.1 as the reachable host, which isn't
        // routable from inside this container's own network namespace. Docker Desktop
        // exposes host.docker.internal precisely for this - overriding it here makes
        // Testcontainers report that as the host for every container it spawns.
        TESTCONTAINERS_HOST_OVERRIDE = 'host.docker.internal'
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
                    junit 'build/test-results/**/TEST-*.xml'
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
                    // Pre-existing lint debt, never enforced before (GitHub Actions doesn't
                    // run this at all) - mark the stage unstable instead of failing the
                    // whole pipeline. Fixing the 37 current violations is separate work.
                    catchError(buildResult: null, stageResult: 'UNSTABLE') {
                        sh 'npm run lint'
                    }
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
                // The NVD 2.0 API itself is flaky (widely-reported outages/throttling,
                // independent of having a valid key) - don't let an external NVD failure
                // block the whole pipeline. failBuildOnCVSS still gates on real findings
                // whenever the scan does complete.
                //
                // The timeout sits INSIDE catchError on purpose: as a stage option it
                // aborted the whole build when the cold-cache NVD download exceeded
                // 20min. Inside catchError the pipeline continues after a timeout
                // (note: Jenkins still latches the overall result to ABORTED when the
                // timeout fires - nothing can override that latch, it simply signals
                // the scan did not complete).
                // The NVD cache lives in GRADLE_USER_HOME (persistent volume), so the
                // slow full download (~1-2h, throttled by NVD servers) only happens
                // once per Jenkins home; incremental updates take seconds. Pre-populate
                // the cache before the first build instead of relying on this timeout.
                catchError(buildResult: 'UNSTABLE', stageResult: 'UNSTABLE') {
                    timeout(time: 30, unit: 'MINUTES') {
                        withCredentials([string(credentialsId: 'NVD_API_KEY', variable: 'NVD_API_KEY')]) {
                            sh './gradlew dependencyCheckAnalyze --no-daemon'
                        }
                    }
                }
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
