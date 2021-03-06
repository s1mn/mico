#!/usr/bin/env groovy

pipeline {
    environment {
        BUILD_USER = ''
        micoAdminRegistry = "ustmico/mico-admin"
        micoCoreRegistry = "ustmico/mico-core"
        registryCredential = 'dockerhub'
        micoAdminDockerImage = ''
        micoCoreDockerImage = ''
    }
    agent any
    stages {
        stage('Checkout') {
            steps {
                git url: 'https://github.com/UST-MICO/mico.git'
            }
        }
        stage('Docker build') {
            parallel {
                stage('mico-core') {
                    steps {
                        script {
                            micoCoreDockerImage = docker.build(micoCoreRegistry, "-f Dockerfile.mico-core .")
                        }
                    }
                }
                stage('mico-admin') {
                    steps {
                        script {
                            micoAdminDockerImage = docker.build(micoAdminRegistry, "-f Dockerfile.mico-admin .")
                        }
                    }
                }
            }
        }
        stage('Unit tests') {
            steps {
                script {
                    docker.build(micoCoreRegistry + ":unit-tests", "-f Dockerfile.mico-core.unittests .")
                }
                sh '''docker run ${micoCoreRegistry}:unit-tests'''
            }
        }
        stage('Integration tests') {
            steps {
                script {
                    docker.build(micoCoreRegistry + ":integration-tests", "-f Dockerfile.mico-core.integrationtests .")
                }
                sh '''docker run ${micoCoreRegistry}:integration-tests'''
            }
        }
        stage('Push images') {
            steps {
                script{
                    docker.withRegistry('', 'dockerhub') {
                        micoCoreDockerImage.push("kube$BUILD_NUMBER")
                        micoAdminDockerImage.push("kube$BUILD_NUMBER")
                        micoCoreDockerImage.push("latest")
                        micoAdminDockerImage.push("latest")
                    }
                }
            }
        }
        stage('Deploy on Kubernetes') {
            parallel {
                stage('mico-core') {
                    steps{
                        sh '''IMAGE_NAME="ustmico/mico-core:kube${BUILD_NUMBER}"
                        kubectl set image deployment/mico-core mico-core=$IMAGE_NAME -n mico-system --kubeconfig /var/lib/jenkins/config'''
                    }
                }
                stage('mico-admin') {
                    steps{
                        sh '''IMAGE_NAME="ustmico/mico-admin:kube${BUILD_NUMBER}"
                        kubectl set image deployment/mico-admin mico-admin=$IMAGE_NAME -n mico-system --kubeconfig /var/lib/jenkins/config'''
                    }
                }
            }
        }
        stage('Docker clean up') {
            steps {
                // Delete all images that are older than 10 days
                sh '''docker system prune --all --force --filter "until=240h"'''
            }
        }
    }

    post {
        success {
            notification('SUCCESS')
        }
        failure {
            notification('FAILURE')
        }
        always {
            // Clean workspace
            cleanWs()
        }
    }
}

def notification(String result) {
    COLOR_MAP = ['SUCCESS': 'good', 'FAILURE': 'danger', 'UNSTABLE': 'danger', 'ABORTED': 'danger']
    wrap([$class: 'BuildUser']) {
        slackSend channel: '#ci-pipeline',
        color: COLOR_MAP[result],
        message: "*${result}:* Job ${env.JOB_NAME} build ${env.BUILD_NUMBER} by ${env.BUILD_USER}\n More info at: ${env.BUILD_URL}"
    }
}
