node {
    checkout scm
    sh "docker build -t docker-registry.dmz.local/eid-jenkins ."
    if (env.BRANCH_NAME == 'master') {
        sh "docker push docker-registry.dmz.local/eid-jenkins"
    }
}
