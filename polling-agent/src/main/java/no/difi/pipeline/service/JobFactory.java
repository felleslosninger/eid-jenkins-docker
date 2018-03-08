package no.difi.pipeline.service;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class JobFactory implements ApplicationContextAware{

    private ApplicationContext applicationContext;

    public JiraStatusJob.JiraAddress jiraRequest() {
        return applicationContext.getBean(JiraStatusJob.Builder.class);
    }

    public CallbackJob.OnBehalfOf callbackRequest() {
        return applicationContext.getBean(CallbackJob.Builder.class);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
