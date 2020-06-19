package no.difi.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.File;

@Configuration
public class EnvironmentConfig implements EnvironmentAware {

    private String jiraPassword;
    private String jiraUsername;
    private String jenkinsUsername;
    private String jenkinsPassword;
    private File repositoryDirectory;

    @Override
    public void setEnvironment(Environment environment) {
        // NOT ALLOWED in bash to user enviroment variables with period inside
        this.jenkinsUsername = environment.getRequiredProperty("jenkinsUsername");
        this.jenkinsPassword = environment.getRequiredProperty("jenkinsPassword");
        this.jiraUsername = environment.getRequiredProperty("jiraUsername");
        this.jiraPassword = environment.getRequiredProperty("jiraPassword");
        this.repositoryDirectory = environment.getRequiredProperty("repositoryDirectory", File.class);

        Logger logger = LoggerFactory.getLogger(EnvironmentConfig.class);
        logger.debug("Found environment-variable: [jiraUsername:"+jiraUsername+"]");
        logger.debug("Found environment-variable: [jiraPassword:"+jiraPassword+"]");
        logger.debug("Found environment-variable: [jenkinsUsername:"+jenkinsUsername+"]");
        logger.debug("Found environment-variable: [jenkinsPassword:"+jenkinsPassword+"]");
        logger.debug("Found environment-variable: [repositoryDirectory:"+repositoryDirectory+"]");
    }

    public String getJiraPassword() {
        return jiraPassword;
    }
    public String getJiraUsername() {
        return jiraUsername;
    }
    public String getJenkinsPassword() {
        return jenkinsPassword;
    }
    public String getJenkinsUsername() {
        return jenkinsUsername;
    }
    public File getRepositoryDirectory() {
        return repositoryDirectory;
    }
}
