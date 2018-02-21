@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

File jobsFile = this.args[0] as File
File templateFile = this.args[1] as File
Map jobs = new Yaml().load((jobsFile).text).jobs
jobs.each { job ->
    def jobName = job.key
    def repository = job.value.repository
    def sshKey = job.value.sshKey
    Path jobDir = Paths.get(System.getenv("JENKINS_HOME") + "/jobs/" + jobName)
    println "Creating job '${jobName}' from repository '${repository}' with SSH credential '${sshKey}'..."
    Files.createDirectories(jobDir)
    new File(jobDir.toString(), 'config.xml').withWriter { w ->
        templateFile.eachLine { line ->
            w << line
                    .replaceAll('REPO', repository)
                    .replaceAll('CREDENTIAL_ID', sshKey) + System.getProperty("line.separator")
        }
    }
}
