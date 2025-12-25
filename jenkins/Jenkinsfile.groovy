node {
  def img = 'radiofer/php-login'
  docker.withServer('172.27.11.100:2375') {
    stage('Build') {
      git branch: 'dev', credentialsId: 'radiofer', url:'git@github.com:radiofer/php-login.git'
      sh "rm -rf .git*"
      docker.build(img, '--no-cache -f docker/Dockerfile .')
    }
    stage('Save') {
    }
    stage('Deploy') {
    }
  }
  stage('Deploy') {
  
  
 }
}
