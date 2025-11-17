/* def call(Map configMap){
    pipeline {
        agent {
                node {
                    label 'AGENT-1'
                }
            }
        environment {
            appVersion = ''
            REGION = "us-east-1"
            ACC_ID = "784585544641"
            PROJECT = configMap.get('project')
            COMPONENT = configMap.get('component')

        }
        options {
            timeout(time: 30, unit: 'MINUTES') 
            disableConcurrentBuilds()
        }
        parameters {
            booleanParam(name: 'deploy', defaultValue: true, description: 'Toggle this value')
        }
    // build
        stages {
            stage('Read package.json') {
                steps {
                    script {
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "Project Version: ${appVersion}"
                    }
                }
            }
            stage('Install Dependencies') {
                steps {
                    script {
                        sh """
                            npm install
                        """
                    }
                }
            }
            stage('Unit Testing') {
                steps {
                    script {
                        sh """
                            echo "unit testing"
                        """
                    }
                }
            }
           /*  stage('sonar scan') {
                environment {
                    scannerHome = tool 'sonar-7.2'
                }
                steps {
                    script {
                        withSonarQubeEnv('sonar-7.2') {
                        sh "${scannerHome}/bin/sonar-scanner"
                        }
                    }
                }
            } */
            // enable webhook in sonarqube server
          /*   stage("Quality Gate") {
                steps {
                    timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true }
                }
            } */
           /*  stage('Check Dependabot Alerts') {
                steps {
                    script {
                        // Call the GitHub API
                        def response = sh(
                            script: """
                                curl -s -H "Accept: application/vnd.github+json" \
                                    -H "Authorization: token ${GITHUB_TOKEN}" \
                                    https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/dependabot/alerts
                            """,
                            returnStdout: true
                        ).trim() */

                        // Parse JSON using Groovy's JsonSlurper
                       /*  def json = new groovy.json.JsonSlurper().parseText(response)

                        // Filter for high or critical severity
                        def criticalOrHigh = json.findAll { it.security_advisory.severity in ['high', 'critical'] }

                        if (criticalOrHigh.size() > 0) {
                            echo "Found ${criticalOrHigh.size()} critical/high Dependabot alerts:"
                            criticalOrHigh.each { alert ->
                                echo "- ${alert.security_advisory.summary} (Severity: ${alert.security_advisory.severity})"
                            }
                            error("Pipeline failed due to critical/high Dependabot alerts!")
                        } else {
                            echo "No critical or high Dependabot alerts found."
                        }
                    }
                }
            } */
         /*    stage('Docker Build') {
                steps {
                    script { 
                        withAWS(credentials: 'aws-creds', region: 'us-east-1') {
                            sh """  
                                aws ecr get-login-password --region ${REGION} | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com
                                docker build -t ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                                docker push ${ACC_ID}.dkr.ecr.us-east-1.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} 
                            """
                        }    
                    }
                }
            }
            stage('trigger deploy') {
                when {
                    expression { params.deploy }
                }
                steps {
                    script { 
                        build job: "../${COMPONENT}-deploy",
                        parameters: [
                            string(name: 'appVersion', value: "${appVersion}"),
                            string(name: 'deploy_to', value: 'dev')
                        ],
                        wait: false,// vpc will not wait for sg pipeline completion
                        propagate: false // even sg fails vpc will not be affected
                            
                    }    
                }
            }
        }
    }        
    post { 
        always { 
            echo 'I will always say Hello again!'
        }
        success {
            echo 'hi this is success'
            deleteDir()
        }
        failure {
            echo 'hi, this is failure'
        }
    }
} */
                 
   def call(Map configMap) {

    node('AGENT-1') {

        // Define variables
        env.REGION  = "us-east-1"
        env.ACC_ID  = "784585544641"
        env.PROJECT = configMap.get('project')
        env.COMPONENT = configMap.get('component')

        try {

            stage('Read package.json') {
                def pkg = readJSON file: 'package.json'
                env.appVersion = pkg.version
                echo "Project Version: ${env.appVersion}"
            }

            stage('Install Dependencies') {
                sh 'npm install'
            }

            stage('Unit Testing') {
                sh 'echo "unit testing"'
            }

            stage('Docker Build') {
                withAWS(credentials: 'aws-creds', region: env.REGION) {
                    sh """
                        aws ecr get-login-password --region ${REGION} \
                        | docker login --username AWS --password-stdin ${ACC_ID}.dkr.ecr.${REGION}.amazonaws.com

                        docker build -t ${ACC_ID}.dkr.ecr.${REGION}.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion} .
                        docker push ${ACC_ID}.dkr.ecr.${REGION}.amazonaws.com/${PROJECT}/${COMPONENT}:${appVersion}
                    """
                }
            }

            stage('Trigger Deploy') {
                if (params.deploy == true) {
                    build job: "../${env.COMPONENT}-deploy",
                        parameters: [
                            string(name: 'appVersion', value: "${env.appVersion}"),
                            string(name: 'deploy_to', value: 'dev')
                        ],
                        wait: false,
                        propagate: false
                }
            }

        } catch (err) {
            echo "Pipeline Failed: ${err}"
            throw err

        } finally {
            echo "Cleaning workspace..."
            deleteDir()
        }
    }
}
         
