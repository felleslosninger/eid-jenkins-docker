package no.difi.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static java.lang.Thread.setDefaultUncaughtExceptionHandler;

@SpringBootApplication
@EnableWebMvc
public class PollingAgentApplication {

    private static final Logger logger = LoggerFactory.getLogger(PollingAgentApplication.class);

    public static void main(String...args) {
        setDefaultUncaughtExceptionHandler((t, e) -> {
                    logger.error("Thread " + t.getName() + " failed - exiting", e);
                    System.exit(1);
                });
        SpringApplication.run(PollingAgentApplication.class, args);
    }


}
