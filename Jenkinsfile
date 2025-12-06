pipeline {
    agent any
    
    tools {
        maven 'Maven 3.9.0' // Maven 버전은 Jenkins에 설정된 이름과 일치해야 합니다
        jdk 'JDK 17'        // JDK 버전은 Jenkins에 설정된 이름과 일치해야 합니다
    }
    
    environment {
        GITHUB_CREDENTIALS_ID = '48080057-1269-4ee5-9a46-39d78e211306'  // GitHub credentials ID
        SSH_CREDENTIALS_ID = 'a799cfa1-be51-4ad2-8173-493f6d42cf5f'     // Jenkins SSH credentials ID
        DEPLOY_SERVER = 'windfury@192.168.0.100'                        // 운영 서버 SSH 정보
        DEPLOY_PATH = '/srv/keycloak/providers'                         // docker-compose.yml 위치

        // 빌드 정보
        JAR_NAME = 'keycloak.auth-0.0.5.jar'
        BUILD_PATH = 'target'
    }
    
    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout([$class: 'GitSCM', 
                    branches: [[name: '*/main']], 
                    userRemoteConfigs: [[
                        url: 'https://github.com/devfury/Keycloak-custom-auth-spi.git',
                        credentialsId: env.GITHUB_CREDENTIALS_ID
                    ]]
                ])
            }
        }
        
        stage('Build') {
            steps {
                echo 'Building with Maven...'
                sh 'mvn clean package -DskipTests'
            }
        }
        
        stage('Test') {
            steps {
                echo 'Running tests...'
                sh 'mvn test'
            }
            post {
                always {
                    // 테스트 결과 수집 (선택사항)
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }
        
        stage('Archive Artifacts') {
            steps {
                echo 'Archiving JAR file...'
                archiveArtifacts artifacts: "${BUILD_PATH}/${JAR_NAME}", fingerprint: true
            }
        }
        
        stage('SCP Transfer') {
            steps {
                echo "Transferring ${JAR_NAME} to remote server..."
                script {
                    sshagent(credentials: [env.SSH_CREDENTIALS_ID]) {
                        sh """
                            scp -o StrictHostKeyChecking=no ${BUILD_PATH}/${JAR_NAME} ${DEPLOY_SERVER}:${DEPLOY_PATH}/
                        """
                    }
                }
            }
        }
        
        stage('Verify Transfer') {
            steps {
                echo 'Verifying file transfer...'
                script {
                    sshagent(credentials: [env.SSH_CREDENTIALS_ID]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_SERVER} "ls -lh ${DEPLOY_PATH}/${JAR_NAME}"
                        """
                    }
                }
            }
        }

        stage('Restart Keycloak') {
            steps {
                echo 'Restarting Keycloak...'
                script {
                    sshagent(credentials: [env.SSH_CREDENTIALS_ID]) {
                        sh """
                            ssh -o StrictHostKeyChecking=no ${DEPLOY_SERVER} "cd ${DEPLOY_PATH} && docker compose restart app"
                        """
                    }
                }
            }
        }
    }
    
    post {
        success {
            echo 'Build and deployment successful!'
            // 성공 시 알림 (선택사항)
            // emailext subject: "Build Success: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //          body: "The build was successful and JAR file has been deployed.",
            //          to: "team@example.com"
        }
        failure {
            echo 'Build or deployment failed!'
            // 실패 시 알림 (선택사항)
            // emailext subject: "Build Failed: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            //          body: "The build failed. Please check the console output.",
            //          to: "team@example.com"
        }
        always {
            echo 'Cleaning up workspace...'
            cleanWs()
        }
    }
}
