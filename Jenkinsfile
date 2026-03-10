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
                    bat 'npm run build'
                }
            }
        }

        stage('Deploy Full Stack to EC2') {
            steps {
                withCredentials([sshUserPrivateKey(credentialsId: 'aws-ec2-ssh-key', keyFileVariable: 'SSH_KEY', usernameVariable: 'SSH_USER')]) {
                    powershell '''
                    $keyPath = "$env:WORKSPACE\\jenkins-key.pem"
                    Copy-Item -Path $env:SSH_KEY -Destination $keyPath -Force

                    $Acl = Get-Acl $keyPath
                    $Acl.SetAccessRuleProtection($true, $false)
                    $Rule = New-Object System.Security.AccessControl.FileSystemAccessRule([System.Security.Principal.WindowsIdentity]::GetCurrent().Name, "Read", "Allow")
                    $Acl.SetAccessRule($Rule)
                    Set-Acl -Path $keyPath -AclObject $Acl

                    # Backend Deployment
                    scp -o StrictHostKeyChecking=no -i $keyPath backend/target/REVCONNECT-0.0.1-SNAPSHOT.jar ${env:SSH_USER}@65.2.37.229:/home/ec2-user/REVCONNECT-0.0.1-SNAPSHOT.jar
                    scp -o StrictHostKeyChecking=no -i $keyPath backend/deploy/revconnect-backend.service ${env:SSH_USER}@65.2.37.229:/tmp/
                    ssh -o StrictHostKeyChecking=no -i $keyPath ${env:SSH_USER}@65.2.37.229 "sudo mv /tmp/revconnect-backend.service /etc/systemd/system/revconnect-backend.service; sudo chown root:root /etc/systemd/system/revconnect-backend.service; sudo systemctl daemon-reload; sudo systemctl enable revconnect-backend; sudo systemctl restart revconnect-backend"

                    # Frontend Deployment
                    ssh -o StrictHostKeyChecking=no -i $keyPath ${env:SSH_USER}@65.2.37.229 "mkdir -p /tmp/frontend"
                    scp -o StrictHostKeyChecking=no -i $keyPath -pr frontend/dist/revconnect-ui/browser/* ${env:SSH_USER}@65.2.37.229:/tmp/frontend/
                    ssh -o StrictHostKeyChecking=no -i $keyPath ${env:SSH_USER}@65.2.37.229 "sudo rm -rf /var/www/html/revconnect-ui/browser/*; sudo mkdir -p /var/www/html/revconnect-ui/browser/; sudo cp -r /tmp/frontend/* /var/www/html/revconnect-ui/browser/; sudo chown -R ec2-user:ec2-user /var/www/html/revconnect-ui; sudo systemctl restart nginx"

                    Remove-Item -Path $keyPath -Force
                    '''
                }
            }
        }
    }

    post {
        always {
            echo 'Unified Full-Stack Deployment Pipeline Finished.'
        }
    }
}
