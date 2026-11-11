// BookNest CI/CD pipeline. Any failing step stops the pipeline (red build).
pipeline {
    agent any

    options {
        // one build at a time, and keep the last 20
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
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
    }
}
