pipeline {
    agent any
    tools {
        jdk 'jdk21'
        maven 'mvm3.9.9'
    }
    triggers {
        pollSCM('* * * * *')
    }
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }
        stage('Deploy') {
            steps {
                sh 'pkill -f "java -jar target/*.jar" || true'
                sh 'nohup java -jar target/*.jar &'
            }
        }
    }
}
