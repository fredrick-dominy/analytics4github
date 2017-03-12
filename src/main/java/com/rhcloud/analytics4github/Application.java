package com.rhcloud.analytics4github;

import com.fasterxml.jackson.databind.JsonNode;
import com.rhcloud.analytics4github.exception.TrendingException;
import com.rhcloud.analytics4github.service.GitHubTrendingService;
import com.rhcloud.analytics4github.util.GithubApiIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;


@SpringBootApplication
public class Application {


    private static Logger LOG = LoggerFactory.getLogger(Application.class);

    @Autowired
    private GitHubTrendingService gitHubTrendingService;

    @Autowired
    private RestTemplate restTemplate;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void initIt() {
        try{
            HttpEntity entity = restTemplate.getForEntity("https://api.github.com/users/whatever", JsonNode.class);
            HttpHeaders headers = entity.getHeaders();
            GithubApiIterator.requestsLeft = Integer.parseInt(headers.get("X-RateLimit-Remaining").get(0));
        } catch (Exception e){
            LOG.debug(e.getMessage());
        }
        try {
            gitHubTrendingService.parseTrendingReposWebPage();
        } catch (TrendingException exception) {
            LOG.error(String.valueOf(exception));
        }
    }
}
