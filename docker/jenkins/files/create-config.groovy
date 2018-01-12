import groovy.text.SimpleTemplateEngine
@Grab('org.yaml:snakeyaml:1.19')
import org.yaml.snakeyaml.Yaml

import java.nio.file.Path
import java.nio.file.Paths

File configFile = this.args[0] as File
File templateFile = this.args[1] as File
Map config = new Yaml().load(configFile.text)
Map binding = [
        'ISSUE_STATUS_OPEN': config.jira.statuses.open,
        'ISSUE_STATUS_IN_PROGRESS': config.jira.statuses.inProgress,
        'ISSUE_STATUS_CODE_APPROVED': config.jira.statuses.codeApproved,
        'ISSUE_STATUS_CODE_REVIEW': config.jira.statuses.codeReview,
        'ISSUE_STATUS_MANUAL_VERIFICATION': config.jira.statuses.manualVerification,
        'ISSUE_STATUS_MANUAL_VERIFICATION_OK': config.jira.statuses.manualVerificationOk,
        'ISSUE_TRANSITION_START': config.jira.transitions.start,
        'ISSUE_TRANSITION_READY_FOR_CODE_REVIEW': config.jira.transitions.readyForCodeReview,
        'ISSUE_TRANSITION_RESUME_WORK': config.jira.transitions.resumeWork,
        'CRUCIBLE_URL': config.crucible.url,
        'CRUCIBLE_PROJECT_KEY': config.crucible.projectKey,
        'dockerRegistryStagingLocalAddress': config.docker.registries['StagingLocal'].address,
        'dockerRegistryStagingLocalApiUrl': config.docker.registries['StagingLocal'].apiUrl ?: '',
        'dockerRegistryStagingPublicAddress': config.docker.registries['StagingPublic'].address,
        'dockerRegistryStagingPublicApiUrl': config.docker.registries['StagingPublic'].apiUrl ?: '',
        'dockerRegistryProductionLocalAddress': config.docker.registries['ProductionLocal'].address,
        'dockerRegistryProductionLocalApiUrl': config.docker.registries['ProductionLocal'].apiUrl ?: '',
        'dockerRegistryProductionPublicAddress': config.docker.registries['ProductionPublic'].address,
        'dockerRegistryProductionPublicApiUrl': config.docker.registries['ProductionPublic'].apiUrl ?: ''
]
Path homeDir = Paths.get(System.getenv("JENKINS_HOME"))
println "Creating Jenkins configuration..."
File outputFile = new File(homeDir.toString(), 'config.xml')
outputFile.delete()
outputFile << new SimpleTemplateEngine().createTemplate(templateFile).make(binding).toString()
