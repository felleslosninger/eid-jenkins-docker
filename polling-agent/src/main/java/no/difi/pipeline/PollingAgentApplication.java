package no.difi.pipeline;

import no.difi.pipeline.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.web.client.RestTemplate;

import java.io.File;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;

@SpringBootApplication
public class PollingAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(PollingAgentApplication.class);

    public static void main(String...args) {
        setDefaultUncaughtExceptionHandler((t, e) -> {
                    logger.error("Thread " + t.getName() + " failed - exiting", e);
                    System.exit(1);
                });
        SpringApplication.run(PollingAgentApplication.class, args);
    }

    private final Environment environment;

    @Autowired
    public PollingAgentApplication(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public JiraClient jiraClient() {
        String username = environment.getRequiredProperty("jira.username");
        String password = environment.getRequiredProperty("jira.password");
        return new JiraClient(
                restTemplate(),
                username,
                password
        );
    }

    @Bean
    public CallbackClient callbackClient() {
        return new CallbackClient(restTemplate());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @Scope("prototype")
    public JiraStatusJob.Builder jiraStatusJobBuilder(PollQueue pollQueue, JobRepository jobRepository, JobFactory jobFactory) {
        return new JiraStatusJob.Builder(
                jiraClient(),
                pollQueue,
                jobFactory,
                jobRepository
        );
    }

    @Bean
    @Scope("prototype")
    public CallbackJob.Builder callbackJobBuilder(PollQueue pollQueue, JobRepository jobRepository) {
        return new CallbackJob.Builder(callbackClient(), pollQueue, jobRepository);
    }

    @Bean
    public File repositoryDirectory() {
        return environment.getRequiredProperty("repositoryDirectory", File.class);
    }

}
