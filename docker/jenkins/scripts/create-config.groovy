import groovy.text.SimpleTemplateEngine
@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

import java.nio.file.Path
import java.nio.file.Paths

File configFile = this.args[0] as File
File templateFile = this.args[1] as File
Map config = new Yaml().load(configFile.text)
Map binding = [
        'CRUCIBLE_URL': config.crucible.url,
        'CRUCIBLE_PROJECT_KEY': config.crucible.projectKey,
]
Path homeDir = Paths.get(System.getenv("JENKINS_HOME"))
println "Creating Jenkins configuration..."
File outputFile = new File(homeDir.toString(), 'config.xml')
outputFile.delete()
outputFile << new SimpleTemplateEngine().createTemplate(templateFile).make(binding).toString()
