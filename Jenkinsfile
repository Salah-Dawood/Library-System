// BookNest CI/CD pipeline. Any failing step stops the pipeline (red build).
pipeline {
    agent any

    options {
        // one build at a time, and keep the last 20
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        // every image the pipeline builds and pushes (each folder has its own Dockerfile)
        SERVICES = 'api-gateway user-service catalog-service lending-service notification-service frontend'
        // the AWS server (EC2 Elastic IP) that main is deployed to
        DEPLOY_HOST = '54.158.44.134'
        DEPLOY_TARGET = "ec2-user@${DEPLOY_HOST}"
        SSH_OPTS = '-o StrictHostKeyChecking=accept-new -o ConnectTimeout=15'
    }

    triggers {
        // Jenkins runs on a laptop that GitHub cannot call, so it checks GitHub for new commits every 2 minutes
        pollSCM('H/2 * * * *')
    }

    stages {
        stage('Build') {
            steps {
                // compiles and packages every service; a compile error stops the pipeline here
                sh 'chmod +x mvnw'
                sh './mvnw -B clean package -DskipTests'
            }
        }

        stage('Test') {
            steps {
                // lending-service tests (concurrency, waitlist, fines) and the user-service Cucumber scenarios
                sh './mvnw -B test'
            }
            post {
                always {
                    // test results appear on the build page; the Cucumber report is kept as a file to download
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                    archiveArtifacts artifacts: 'user-service/target/cucumber-report.html', allowEmptyArchive: true
                }
            }
        }

        stage('Build images') {
            // only main produces images; other branches stop after the tests
            when { branch 'main' }
            steps {
                script {
                    // 1.0.0 from the pom, and 1.0.0-<commit> so every image can be traced back to its commit
                    env.VERSION = sh(script: './mvnw -q -B help:evaluate -Dexpression=project.version -DforceStdout', returnStdout: true).trim()
                    env.IMAGE_TAG = "${env.VERSION}-${env.GIT_COMMIT.take(7)}"
                    echo "Building images ${env.VERSION} and ${env.IMAGE_TAG}"
                }
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    script {
                        for (String service : SERVICES.split(' ')) {
                            sh "docker build -f ${service}/Dockerfile" +
                                    " -t \$DOCKERHUB_USER/booknest-${service}:${env.VERSION}" +
                                    " -t \$DOCKERHUB_USER/booknest-${service}:${env.IMAGE_TAG}" +
                                    " -t \$DOCKERHUB_USER/booknest-${service}:latest ."
                        }
                    }
                }
            }
        }

        stage('Push images') {
            when { branch 'main' }
            steps {
                // the Docker Hub login is a Jenkins credential (id "dockerhub"), never written in the code
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKERHUB_USER', passwordVariable: 'DOCKERHUB_TOKEN')]) {
                    sh 'echo "$DOCKERHUB_TOKEN" | docker login -u "$DOCKERHUB_USER" --password-stdin'
                    script {
                        for (String service : SERVICES.split(' ')) {
                            for (String tag : [env.VERSION, env.IMAGE_TAG, 'latest']) {
                                sh "docker push \$DOCKERHUB_USER/booknest-${service}:${tag}"
                            }
                        }
                    }
                }
            }
            post {
                always {
                    sh 'docker logout || true'
                }
            }
        }

        stage('Deploy') {
            when { branch 'main' }
            steps {
                // the server's SSH key is a Jenkins credential (id "ec2-ssh"), never written in the code
                sshagent(credentials: ['ec2-ssh']) {
                    // the latest compose file goes to the server first
                    sh "scp ${SSH_OPTS} docker-compose.prod.yml ${DEPLOY_TARGET}:~/booknest/docker-compose.prod.yml"
                    // the server runs exactly the images this build pushed; --wait fails the build if a service never becomes healthy
                    sh """
                        ssh ${SSH_OPTS} ${DEPLOY_TARGET} '
                            set -e
                            cd ~/booknest
                            if grep -q "^VERSION=" .env; then sed -i "s/^VERSION=.*/VERSION=${env.IMAGE_TAG}/" .env; else echo "VERSION=${env.IMAGE_TAG}" >> .env; fi
                            docker compose -f docker-compose.prod.yml pull
                            docker compose -f docker-compose.prod.yml up -d --wait --wait-timeout 300
                            docker image prune -f
                        '
                    """
                }
                // the site must answer from outside, or the build is red
                sh "curl -fsS --retry 6 --retry-delay 10 --retry-connrefused -o /dev/null http://${DEPLOY_HOST}/"
                echo "BookNest ${env.IMAGE_TAG} is live at http://${DEPLOY_HOST}"
            }
        }
    }
}
