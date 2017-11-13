@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

String jobsFile = this.args[0]
Map jobs = new Yaml().load((jobsFile as File).text).jobs
jobs.each {
    def jobName = it.key
    def repository = it.value.repository
    def sshKey = it.value.sshKey
    Path jobDir = Paths.get(System.getenv().get("JENKINS_HOME") + "/jobs/" + jobName)
    println "Creating job '${jobName}' from repository '${repository}' with SSH credential '${sshKey}'..."
    Files.createDirectories(jobDir)
    new File(jobDir.toString(), 'config.xml').withWriter { w ->
        new File('/files/template-job-config.xml').eachLine { line ->
            w << line
                    .replaceAll('REPO', repository)
                    .replaceAll('CREDENTIAL_ID', sshKey) + System.getProperty("line.separator")
        }
    }
}
