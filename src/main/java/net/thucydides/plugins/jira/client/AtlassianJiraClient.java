package net.thucydides.plugins.jira.client;

import ch.lambdaj.function.convert.Converter;
import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.SearchRestClient;
import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.IssueLink;
import com.atlassian.jira.rest.client.domain.IssueLinkType;
import com.atlassian.jira.rest.client.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.common.collect.Lists;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import static ch.lambdaj.Lambda.convert;

/**
 * A JIRA client using the new REST interface
 */
public class AtlassianJiraClient {

    private final URI url;
    private final String username;
    private final String password;
    private final int batchSize;

    private final static int DEFAULT_BATCH_SIZE = 100;

    public AtlassianJiraClient(String url, String username, String password) {
        this(url, username, password, DEFAULT_BATCH_SIZE);
    }

    public AtlassianJiraClient(String url, String username, String password, int batchSize) {
        try {
            this.url = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid JIRA URL: " + url, e);
        }
        this.username = username;
        this.password = password;
        this.batchSize = batchSize;
    }

    /**
     * Load the issue keys for all of the issues matching the specified JQL query
     * @param query
     * @return a list of JIRA issue keys
     */
    public List<String> findByJQL(String query) {
        return convert(findBasicIssuesByJQL(query), toIssueKeys());
    }

    public List<Issue> findIssuesByJQL(String query) {
        return findIssuesByKey(findByJQL(query));
    }

    public Issue findIssueByKey(String issueKey) {
        return getJiraRestClient().getIssueClient().getIssue(issueKey).claim();
    }

    public List<Issue> findIssuesByKey(List<String> issueKeys) {
        return convert(issueKeys, toIssues());
    }

    public List<Issue> findInboundIssuesWithLink(Issue issue, String linkName) {
        List<String> matchingIssues = findLinkedIssueKeys(issue, linkName, IssueLinkType.Direction.INBOUND);
        return findIssuesByKey(matchingIssues);
    }

    public List<Issue> findOutboundIssuesWithLink(Issue issue, String linkName) {
        List<String> matchingIssues = findLinkedIssueKeys(issue, linkName, IssueLinkType.Direction.OUTBOUND);
        return findIssuesByKey(matchingIssues);
    }

    public Object getField(Issue issue, String fieldName) {
        return issue.getFieldByName(fieldName).getValue();
    }

    private Converter<String,Issue> toIssues() {
        return new Converter<String,Issue>() {

            public Issue convert(String key) {
                return findIssueByKey(key);
            }
        };
    }

    private List<String> findLinkedIssueKeys(Issue issue, String linkName, IssueLinkType.Direction direction) {
        List<String> matchingIssues = Lists.newArrayList();

        for(IssueLink link : issue.getIssueLinks()) {
            if ((linkDirection(link) == direction) && linkIsDescribedBy(link,linkName)) {
                matchingIssues.add(link.getTargetIssueKey());
            }
        }
        return matchingIssues;
    }

    private IssueLinkType.Direction linkDirection(IssueLink link) {
        return link.getIssueLinkType().getDirection();
    }

    private boolean linkIsDescribedBy(IssueLink link, String nameOrDescription) {
        return (link.getIssueLinkType().getName().equalsIgnoreCase(nameOrDescription)
                || link.getIssueLinkType().getDescription().equalsIgnoreCase(nameOrDescription));
    }

    private Converter<BasicIssue, String> toIssueKeys() {
        return new Converter<BasicIssue, String>() {

            public String convert(BasicIssue from) {
                return from.getKey();
            }
        };
    }

    private Iterable<BasicIssue> findBasicIssuesByJQL(String query) {
        List< BasicIssue > issues = fetchAllIssues(getJiraRestClient().getSearchClient(), query);
        for(BasicIssue issue : issues) { System.out.print(issue.getKey() + " ");}
        return issues;
    }

    private JiraRestClient getJiraRestClient() {
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        return factory.createWithBasicHttpAuthentication(url, username, password);
    }


    private List<BasicIssue> fetchAllIssues(SearchRestClient searchClient, String query) {

        List<BasicIssue> matchingIssues = Lists.newArrayList();

        int loadedIssues = 0;
        boolean allLoaded = false;
        int startAt = 0;

        while(!allLoaded) {
            SearchResult batchResults = searchClient.searchJql(query, getBatchSize(), startAt).claim();
            List<BasicIssue> issuesInThisBatch = Lists.newArrayList(batchResults.getIssues());
            matchingIssues.addAll(issuesInThisBatch);

            startAt += getBatchSize();
            loadedIssues += issuesInThisBatch.size();
            allLoaded = batchResults.getTotal() <= loadedIssues;
        }
        return matchingIssues;
    }

    public int getBatchSize() {
        return batchSize;
    }
}
