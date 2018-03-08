package no.difi.pipeline.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Repository
public class JobRepository {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private File directory;
    private ObjectMapper objectMapper;

    public JobRepository(File repositoryDirectory, ObjectMapper jobMapper) {
        this.directory = repositoryDirectory;
        this.objectMapper = jobMapper;
        if (!repositoryDirectory.exists()) {
            if (!repositoryDirectory.mkdirs())
                throw new RuntimeException("Failed to create repository directory " + repositoryDirectory);
        }
    }

    public void save(Job job) {
        try {
            objectMapper.writer().writeValue(file(job.id()), job);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save job " + job.id() + ": " + e.getMessage());
        }
        logger.info("Saved " + file(job.id()));
    }

    public void delete(String jobId) {
        if (!file(jobId).delete()) {
            logger.warn("Failed to delete " + file(jobId));
        } else {
            logger.info("Deleted " + file(jobId));
        }
    }

    public List<Job> load() {
        return files().stream()
                .map(this::parseFile)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toList());
    }

    private File file(String jobId) {
        return new File(directory, jobId);
    }

    private List<File> files() {
        File[] files = directory.listFiles();
        if (files == null) throw new RuntimeException("Failed to list files (returned null)");
        return Arrays.asList(files);
    }

    private Optional<Job> parseFile(File file) {
        logger.info("Loading file " + file.getName() + "...");
        try {
            if (file.getName().startsWith(JiraStatusJob.class.getSimpleName()))
                return Optional.of(objectMapper.readerFor(JiraStatusJob.class).readValue(file));
            else if (file.getName().startsWith(CallbackJob.class.getSimpleName()))
                return Optional.of(objectMapper.readerFor(CallbackJob.class).readValue(file));
            else
                logger.warn("Unrecognized file name for a job: " + file.getName());
        } catch (IOException e) {
            logger.warn("Failed to parse file " + file.getName() + ": " + e.toString());
        }
        return Optional.empty();
    }

}
