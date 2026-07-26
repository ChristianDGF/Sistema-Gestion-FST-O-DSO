pipeline {
    agent any

    tools {
        jdk 'jdk21'
        nodejs 'node22'
    }

    environment {
        SPRING_DATASOURCE_URL = 'jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1'
        SPRING_DATASOURCE_USERNAME = 'sa'
        SPRING_DATASOURCE_PASSWORD = ''
        SPRING_PROFILES_ACTIVE = 'test'
        GRADLE_OPTS = '-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true'
        FRONTEND_DIR = "${WORKSPACE}/frontend"
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

        stage('Run Unit, Contract & Data Tests') {
            steps {
                // 'test' also includes proyecto.sistemaGestion.data.* (migrations, DB constraints,
                // referential integrity, duplicate data and seeds), run with Flyway enabled
                // against a real Postgres Testcontainer instead of the H2/ddl-auto used above.
                sh './gradlew generateContractTests test --no-daemon --parallel'
            }
            post {
                always {
                    junit 'build/reports/tests/**/TEST-*.xml'
                    publishHTML(
                        target: [
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'build/reports/tests/test',
                            reportFiles: 'index.html',
                            reportName: 'Unit & Data Test Report'
                        ]
                    )
                }
            }
        }

        stage('SonarCloud Analysis') {
            steps {
                withCredentials([string(credentialsId: 'SONAR_TOKEN', variable: 'SONAR_TOKEN')]) {
                    sh './gradlew generateContractTests test jacocoTestReport sonar -Dsonar.token=${SONAR_TOKEN} --no-daemon'
                }
            }
        }

        stage('Frontend Install & Lint') {
            steps {
                dir("${FRONTEND_DIR}") {
                    sh 'npm install'
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

        stage('Backend Full Build') {
            steps {
                sh './gradlew build -x test --no-daemon --parallel'
            }
        }

        stage('Run E2E Tests') {
            steps {
                script {
                    try {
                        sh 'docker-compose up -d db keycloak'
                        sleep 15
                        sh 'docker-compose up -d app frontend'
                        sleep 15
                        dir("${FRONTEND_DIR}") {
                            sh 'npx playwright install --with-deps chromium'
                            sh 'npm run test:e2e'
                        }
                    } finally {
                        sh 'docker-compose down'
                    }
                }
            }
        }

        stage('Performance Testing (k6)') {
            steps {
                script {
                    try {
                        sh 'docker-compose up -d db keycloak'
                        sleep 15
                        sh 'docker-compose up -d app frontend'
                        sleep 15
                        sh 'mkdir -p performance-tests/reports'

                        sh '''
                            docker run --rm --network host \
                                -v "$WORKSPACE/performance-tests:/scripts:ro" \
                                -v "$WORKSPACE/performance-tests/reports:/scripts/reports:rw" \
                                -e BASE_URL=http://localhost:8081 \
                                -e KEYCLOAK_URL=http://localhost:8080 \
                                -e REPORTS_DIR=/scripts/reports \
                                grafana/k6:latest run /scripts/scenarios/smoke.js
                        '''
                        sh '''
                            docker run --rm --network host \
                                -v "$WORKSPACE/performance-tests:/scripts:ro" \
                                -v "$WORKSPACE/performance-tests/reports:/scripts/reports:rw" \
                                -e BASE_URL=http://localhost:8081 \
                                -e KEYCLOAK_URL=http://localhost:8080 \
                                -e REPORTS_DIR=/scripts/reports \
                                grafana/k6:latest run /scripts/scenarios/load.js
                        '''

                        // Stress testing is resource-heavy and not meant to run on every build;
                        // enable it explicitly via the RUN_STRESS_TEST job parameter/env var.
                        if (env.RUN_STRESS_TEST == 'true') {
                            sh '''
                                docker run --rm --network host \
                                    -v "$WORKSPACE/performance-tests:/scripts:ro" \
                                    -v "$WORKSPACE/performance-tests/reports:/scripts/reports:rw" \
                                    -e BASE_URL=http://localhost:8081 \
                                    -e KEYCLOAK_URL=http://localhost:8080 \
                                    -e REPORTS_DIR=/scripts/reports \
                                    grafana/k6:latest run /scripts/scenarios/stress.js
                            '''
                        }
                    } finally {
                        sh 'docker-compose down'
                    }
                }
            }
            post {
                always {
                    publishHTML(
                        target: [
                            allowMissing: true,
                            alwaysLinkToLastBuild: true,
                            keepAll: true,
                            reportDir: 'performance-tests/reports',
                            reportFiles: 'load-summary.html',
                            reportName: 'k6 Load Test Report'
                        ]
                    )
                    archiveArtifacts(
                        artifacts: 'performance-tests/reports/**',
                        fingerprint: true,
                        allowEmptyArchive: true
                    )
                }
            }
        }

        stage('Security Scan') {
            steps {
                script {
                    // Requires the NVD_API_KEY env var / credential to avoid NVD rate limiting.
                    sh './gradlew dependencyCheckAnalyze --no-daemon'

                    dir("${FRONTEND_DIR}") {
                        sh 'npm run audit'
                    }

                    try {
                        sh 'docker-compose up -d db keycloak'
                        sleep 15
                        sh 'docker-compose up -d app frontend'
                        sleep 15
                        sh 'mkdir -p zap-reports'
                        sh '''
                            docker run --rm \
                                -v "$WORKSPACE/.zap:/zap/wrk/config:ro" \
                                -v "$WORKSPACE/zap-reports:/zap/wrk/:rw" \
                                --network host ghcr.io/zaproxy/zaproxy:stable \
                                zap-baseline.py -t http://localhost:5173 \
                                -r zap-frontend-report.html -J zap-frontend-report.json \
                                -c config/rules.tsv -a || true
                            docker run --rm \
                                -v "$WORKSPACE/.zap:/zap/wrk/config:ro" \
                                -v "$WORKSPACE/zap-reports:/zap/wrk/:rw" \
                                --network host ghcr.io/zaproxy/zaproxy:stable \
                                zap-baseline.py -t http://localhost:8081/swagger-ui.html \
                                -r zap-backend-report.html -J zap-backend-report.json \
                                -c config/rules.tsv -a || true
                        '''
                    } finally {
                        sh 'docker-compose down'
                    }
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
                    archiveArtifacts(
                        artifacts: 'zap-reports/**',
                        fingerprint: true,
                        allowEmptyArchive: true
                    )
                }
            }
        }

        stage('Docker Build') {
            when {
                expression { env.BUILD_DOCKER == 'true' }
            }
            steps {
                script {
                    docker.build('sistema-gestion-backend:latest', '.')
                    docker.build('sistema-gestion-frontend:latest', './frontend')
                }
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
