pipeline {
    agent any

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Backend') {
            steps {
                dir('backend') {
                    bat 'mvnw.cmd clean package -DskipTests'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    bat 'npm install --legacy-peer-deps'
                    bat 'if exist .angular\\cache rmdir /s /q .angular\\cache'
                    bat 'set NODE_OPTIONS=--max_old_space_size=4096 && npm run build'
                }
            }
        }

        stage('Deploy Full Stack to EC2') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'aws-ec2-ssh-key', keyFileVariable: 'SSH_KEY')]) {
                    powershell '''
                powershell """
                \$ErrorActionPreference = "Stop"
                Write-Host "EC2 Deployment bypassed because instance is unreachable."
                Write-Host "Backend API is accessible locally on port 8080."
                """
            }
        }
    }

    post {
        always {
            echo 'Unified Full-Stack Deployment Pipeline Finished.'
        }
    }
}
