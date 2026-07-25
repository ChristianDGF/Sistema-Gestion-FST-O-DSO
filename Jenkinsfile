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

        stage('Run Unit & Contract Tests') {
            steps {
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
                            reportName: 'Unit Test Report'
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
                            sh 'npm ci'
                            sh 'npx playwright install --with-deps chromium'
                            sh 'npm run test:e2e'
                        }
                    } finally {
                        sh 'docker-compose down'
                    }
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
