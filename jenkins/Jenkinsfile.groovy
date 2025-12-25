node {
  def img = 'radiofer/php-login'
  try {
    docker.withServer('172.27.11.100:2375') {
      stage('Build') {
        git branch: 'dev', credentialsId: 'radiofer', url:'git@github.com:radiofer/php-login.git'
        sh "rm -rf .git*"
        docker.build(img, '--no-cache -f docker/Dockerfile .')
      }
      stage('Test') {
        sh "docker compose -p ${JOB_BASE_NAME} -f docker/docker-compose.yml up -d"
        sleep 20
        sh "docker compose -p ${JOB_BASE_NAME} -f docker/docker-compose.yml exec -T mysql mariadb -u root -p'Abc123!' php < db/dump.sql"
        containerID = sh(returnStdout: true, script: "docker compose -p ${JOB_BASE_NAME} -f docker/docker-compose.yml ps -q app").trim()
        ip = sh(returnStdout: true, script: "docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' ${containerID}").trim()
        docker.image('alpine').withRun("-it --network ${JOB_BASE_NAME}_default") { alpine ->
          sh "docker exec ${alpine.id} apk add --no-cache curl"
          sh "docker exec ${alpine.id} curl -sL ${ip} > /dev/null"
          def output = sh(returnStdout: true, script: """
            docker exec ${alpine.id} curl -sL --cookie-jar cookie \
            -d 'username=victor@frankenstein.co.uk&pass=123' \
            http://${ip}/login.php
          """).trim()
          if(!output.contains('Bem Vindo!'))
            error('Login falhou!')
        }     

      }
      stage('Save') {
      }
    }
    stage('Deploy') {
    
    
    } 
  } catch (ex) {
    throw ex
  } finally {
    docker.withServer('172.27.11.100:2375') {
      sh "docker compose -p ${JOB_BASE_NAME} -f docker/docker-compose.yml down -v"
    }
  }
  
}
