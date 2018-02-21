import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

def slaves = this.args[0]
slaves.tokenize(',').each { String slaveName ->
    Path slaveDir = Paths.get(System.getenv('JENKINS_HOME') + "/nodes/${slaveName}")
    println "Creating slave '${slaveName}'..."
    Files.createDirectories(slaveDir)
    new File(slaveDir.toString(), 'config.xml').withWriter { w ->
        new File('/templates/slave-config.xml').eachLine { line ->
            w << line.replaceAll('SLAVE_NAME', slaveName) + System.getProperty('line.separator')
    }}
}
