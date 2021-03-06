package com.rhcloud.analytics4github;

import com.rhcloud.analytics4github.exception.TrendingException;
import com.rhcloud.analytics4github.service.GitHubTrendingService;
import com.rhcloud.analytics4github.util.GitHubApiIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;


@SpringBootApplication
@EnableScheduling
public class Application {

    private static Logger LOG = LoggerFactory.getLogger(Application.class);

    @Autowired
    private GitHubTrendingService gitHubTrendingService;

    @Autowired
    private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    /**
     * with start application shows requests number left and parses top trending repositories
     */
    @PostConstruct
    public void initIt() {
        try {
            GitHubApiIterator.initializeRequestsLeft(restTemplate);
            gitHubTrendingService.parseTrendingReposWebPage();
        } catch (TrendingException exception) {
            LOG.error(String.valueOf(exception));
        }
    }
}
