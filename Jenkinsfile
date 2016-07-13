node {
    checkout scm
    sh 'docker build -t docker-registry.dmz.local/eid-jenkins .'
    sh 'docker push docker-registry.dmz.local/eid-jenkins'
}
