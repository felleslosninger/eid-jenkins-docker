import groovy.text.SimpleTemplateEngine
import java.nio.file.Path
import java.nio.file.Paths

File templateFile = this.args[0] as File
Map binding = [
        'dependencyTrackApi' : new File('/run/secrets/dependency-track-api').text.trim()
]
Path homeDir = Paths.get(System.getenv("JENKINS_HOME"))
println "Creating Dependency-track configuration..."
File outputFile = new File(homeDir.toString(), 'org.jenkinsci.plugins.DependencyTrack.DependencyTrackPublisher.xml')
outputFile.delete()
outputFile << new SimpleTemplateEngine().createTemplate(templateFile).make(binding).toString()
