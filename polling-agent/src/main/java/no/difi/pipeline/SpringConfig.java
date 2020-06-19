package no.difi.pipeline;

import no.difi.pipeline.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.web.client.RestTemplate;

import java.io.File;

// TODO: skriv om SpringConfig
// Det er noko feil med application-context her slik at ein må import alle klassar (alle med annotasjoner) via Contextconfiguration og Import i SpringBootTests.
// Trur problemet er miksen av manuelt oppretting av bønner i SpringConfig og spring-konfig elles. Trur all beans i SpringConfig burde vore skriv om til "rein" spring konfig med annotasjoner.
@Configuration
public class SpringConfig {

    private EnvironmentConfig environmentConfig;

    @Autowired
    public SpringConfig(EnvironmentConfig environmentConfig){
        this.environmentConfig = environmentConfig;
    }


    @Bean
    public JiraClient jiraClient() {
        String username = environmentConfig.getJiraUsername();
        String password = environmentConfig.getJiraPassword();
        return new JiraClient(
                restTemplate(),
                username,
                password
        );
    }

    @Bean
    public CallbackClient callbackClient() {
        String username = environmentConfig.getJenkinsUsername();
        String password = environmentConfig.getJenkinsPassword();
        return new CallbackClient(restTemplate(),username,password);
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
        return environmentConfig.getRepositoryDirectory();
    }

}
