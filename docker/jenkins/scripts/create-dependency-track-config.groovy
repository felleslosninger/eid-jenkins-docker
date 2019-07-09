import groovy.text.SimpleTemplateEngine
@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

import java.nio.file.Path
import java.nio.file.Paths

File templateFile = this.args[1] as File
Map binding = [
        'dependency-track-api': new File('/run/secrets/dependency-track-api').text.trim(),
]
Path homeDir = Paths.get(System.getenv("JENKINS_HOME"))
println "Creating Jira configuration..."
File outputFile = new File(homeDir.toString(), 'org.jenkinsci.plugins.DependencyTrack.DependencyTrackPublisher.xml')
outputFile.delete()
outputFile << new SimpleTemplateEngine().createTemplate(templateFile).make(binding).toString()
