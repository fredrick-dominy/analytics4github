package com.rhcloud.analytics4github;

import com.rhcloud.analytics4github.service.GitHubTrendingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class Application {

    private static Logger LOG = LoggerFactory.getLogger(Application.class);
    @Autowired
    GitHubTrendingService gitHubTrendingService;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
