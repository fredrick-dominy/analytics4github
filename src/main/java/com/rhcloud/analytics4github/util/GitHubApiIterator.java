package com.rhcloud.analytics4github.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rhcloud.analytics4github.config.GitHubApiEndpoints;
import com.rhcloud.analytics4github.exception.GitHubRESTApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URISyntaxException;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Iterate over commits array of specified project on Github using RestTemplate
 *
 * @author lyashenkogs.
 */
public class GitHubApiIterator implements Iterator<JsonNode> {
    private static Logger LOG = LoggerFactory.getLogger(GitHubApiIterator.class);

    private final int numberOfPages;
    private final String projectName;
    private final GitHubApiEndpoints githubEndpoint;
    private final RestTemplate restTemplate;
    private final ExecutorService executor = Executors.newFixedThreadPool(5);
    private Instant since = null;
    private Instant until = null;
    private String author;
    private volatile AtomicInteger counter = new AtomicInteger();
    public static int requestsLeft;

    public GitHubApiIterator(String projectName, RestTemplate restTemplate, GitHubApiEndpoints endpoint
    ) throws URISyntaxException, GitHubRESTApiException {
        this.restTemplate = restTemplate;
        this.projectName = projectName;
        this.githubEndpoint = endpoint;
        this.numberOfPages = getLastPageNumber(projectName);
        this.counter.set(numberOfPages);
    }

    public GitHubApiIterator(String projectName, String author, RestTemplate restTemplate, GitHubApiEndpoints endpoint
    ) throws URISyntaxException, GitHubRESTApiException {
        this.author = author;
        this.restTemplate = restTemplate;
        this.projectName = projectName;
        this.githubEndpoint = endpoint;
        this.numberOfPages = getLastPageNumber(projectName);
        this.counter.set(numberOfPages);
    }

    public GitHubApiIterator(String projectName, RestTemplate restTemplate, GitHubApiEndpoints endpoint,
                             Instant since, Instant until) throws URISyntaxException, GitHubRESTApiException {
        this.since = since;
        this.until = until;
        this.restTemplate = restTemplate;
        this.projectName = projectName;
        this.githubEndpoint = endpoint;
        this.numberOfPages = getLastPageNumber(projectName);
        this.counter.set(numberOfPages);
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public String getProjectName() {
        return projectName;
    }

    public int getLastPageNumber(String projectName) throws URISyntaxException, GitHubRESTApiException {
        return Utils.getLastPageNumber(projectName, restTemplate, githubEndpoint, author, since, until);
    }

    public synchronized boolean hasNext() {
        return counter.get() > 0;
    }

    /**
     * Performs side efects (parsing a header and updating the static field
     * @return JsonNode that represents a stargazers list
     *
     */
    public JsonNode next() {
        if (counter.get() > 0) {
            String basicURL = "https://api.github.com/repos/" + projectName + "/" + githubEndpoint.toString().toLowerCase();
            UriComponents page;

            if (since != null && until!=null) {
                page = UriComponentsBuilder.fromHttpUrl(basicURL)
                        .queryParam("page", counter.getAndDecrement())
                        .queryParam("since", since)
                        .queryParam("until",until)
                        .build();
            } else if (since != null){
                page = UriComponentsBuilder.fromHttpUrl(basicURL)
                        .queryParam("page", counter.getAndDecrement())
                        .queryParam("since", since)
                        .build();
            } else if (author != null) {
                page = UriComponentsBuilder.fromHttpUrl(basicURL)
                        .queryParam("page", counter.getAndDecrement())
                        .queryParam("author", author)
                        .build();
            } else {
                page = UriComponentsBuilder.fromHttpUrl(basicURL)
                        .queryParam("page", counter.getAndDecrement())
                        .build();
            }
            String URL = page.encode().toUriString();
            LOG.debug(URL);
            //sent request
            HttpEntity entity = restTemplate.getForEntity(URL, JsonNode.class);
            initializeRequestsLeft(restTemplate);
            JsonNode node = new ObjectMapper().convertValue(entity.getBody(), JsonNode.class);
            LOG.debug(node.toString());
            return node;
        } else throw new IndexOutOfBoundsException("there is no next element");
    }

    public static void initializeRequestsLeft(RestTemplate restTemplate){
        try{
            HttpEntity entity = restTemplate.getForEntity("https://api.github.com/users/whatever", JsonNode.class);
            HttpHeaders headers = entity.getHeaders();
            GitHubApiIterator.requestsLeft = Integer.parseInt(headers.get("X-RateLimit-Remaining").get(0));
        } catch (Exception e){
            LOG.debug(e.getMessage());
        }
    }

    /**
     * @param batchSize number of numberOfPages to return
     * @return list of stargazer numberOfPages according to batchSize
     * @throws IndexOutOfBoundsException if there is no elements left to return
     */
    public List<JsonNode> next(int batchSize) throws ExecutionException, InterruptedException {
        int reliableBatchSize = batchSize;
        if (batchSize > counter.get()) {
            LOG.warn("batch size is bigger then number of elements left, decrease batch size to " + counter);
            reliableBatchSize = counter.get();
        }

        List<CompletableFuture<JsonNode>> completableFutures = IntStream.range(0, reliableBatchSize)
                .mapToObj(
                        e -> CompletableFuture
                                .supplyAsync(this::next, executor)
                ).collect(Collectors.toList());

        CompletableFuture<List<JsonNode>> result = CompletableFuture
                .allOf(completableFutures
                        .toArray(new CompletableFuture[completableFutures.size()]))
                .thenApply(v -> completableFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));//compose all in one task
        List<JsonNode> jsonNodes = result.get();//
        LOG.debug("batch completed, counter:" + counter.get());
        return jsonNodes;
    }

    /**
     * invoke explicitly after every class usage to close ThreadPool correctly
     */
    public void close() {
        this.executor.shutdown();
    }
}
