node {
  docker.withServer('172.27.11.100:2375') {
    stage('Build') {
      git branch: 'dev', credentialsId: 'radiofer', url:'git@github.com:radiofer/php-login.git'
    }
    stage('Save') {
    }
    stage('Deploy') {
    }
  }
  stage('Deploy') {
  
  
  }
}
