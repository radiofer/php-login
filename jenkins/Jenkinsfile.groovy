node {
  def servers = [100, 200]
  def server = servers[new Random().nextInt(servers.size())]
  def img = 'radiofer/php-login'
  def parameters = [
    '--restart always',
    '-p 8081:80',
    "--name ${JOB_BASE_NAME}",
    "--network ${JOB_BASE_NAME}",
    '--ip 192.168.10.10',
    '--add-host=memcached:172.27.11.30',
    '-e DB_HOST=172.27.11.30',
    '-e DB_PORT=3306',
    '-e DB_NAME=infraagil',
    '-e DB_USER=devops',
    '-e DB_PASS=4linux'
  ]
  try {
    println "Utilizando servidor 172.27.11.${server}"
    docker.withServer("172.27.11.${server}:2375") {
      stage('Build') {
        sh "rm -rf .git*"
        docker.build(img, '--no-cache -f docker/Dockerfile .')
      }
      stage('Test') {
        sh "docker compose -p ${JOB_BASE_NAME} -f docker/docker-compose.yml up -d"
        sleep 30
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
          if(!output.contains('Bem Vindo!')) {
            error('Login falhou!')
          }
        }     
      }
      stage('Save') {
       withDockerRegistry(credentialsId: 'docker-registry', url: 'https://index.docker.io/v1/') {
        docker.image(img).push()
       }
      }
      stage('Deploy') { 
        [100, 200].each {
          docker.withServer("172.27.11.${it}:2375") {
            withDockerRegistry(credentialsId: 'docker-registry', url: 'https://index.docker.io/v1/') {
              docker.image(img).pull()
          }
          sh "docker network create --subnet 192.168.10.0/24 ${JOB_BASE_NAME} || /bin/true"
          sh "docker rm -f ${JOB_BASE_NAME} || /bin/true"
          docker.image(img).run(parameters.join(' '))
        }
      } 
    }
  }
  } catch (ex) {
      throw ex
    } finally {
      docker.withServer("172.27.11.${server}:2375") {
        sh "docker compose -p ${JOB_BASE_NAME} -f docker/docker-compose.yml down -v"
      }
  }
  
}
