import groovy.text.SimpleTemplateEngine
@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

import java.nio.file.Path
import java.nio.file.Paths

File configFile = '/config.yaml' as File
File templateFile = '/templates/hudson.plugins.git.GitSCM.xml' as File
Map config = new Yaml().load(configFile.text)
Map binding = [
        'userName': config.git.userName,
        'userEmail': config.git.userEmail,
]
Path homeDir = Paths.get(System.getenv("JENKINS_HOME"))
File outputFile = new File(homeDir.toString(), templateFile.getName())
println "Creating ${outputFile}..."
outputFile.delete() // Delete old file if exists
outputFile << new SimpleTemplateEngine().createTemplate(templateFile).make(binding).toString()
