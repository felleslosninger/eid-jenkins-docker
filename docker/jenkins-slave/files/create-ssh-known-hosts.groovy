@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

File configFile = this.args[0] as File
Map config = new Yaml().load(configFile.text)
List<String> knownHosts = config.ssh.knownHosts
println "Creating /etc/ssh/ssh_known_hosts..."
File outputFile = new File('/etc/ssh/ssh_known_hosts')
outputFile.delete()
outputFile << knownHosts.join('\n')
