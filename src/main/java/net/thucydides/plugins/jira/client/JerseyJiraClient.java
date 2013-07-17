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
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;
import net.thucydides.plugins.jira.domain.IssueSummary;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.List;

import static ch.lambdaj.Lambda.convert;
import static ch.lambdaj.Lambda.map;

/**
 * A JIRA client using the new REST interface
 */
public class JerseyJiraClient {

    private final String url;
    private final String username;
    private final String password;
    private final int batchSize;

    private final static int DEFAULT_BATCH_SIZE = 100;

    public JerseyJiraClient(String url, String username, String password) {
        this(url, username, password, DEFAULT_BATCH_SIZE);
    }

    public JerseyJiraClient(String url, String username, String password, int batchSize) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.batchSize = batchSize;
    }

    /**
     * Load the issue keys for all of the issues matching the specified JQL query
     *
     * @param query
     * @return a list of JIRA issue keys
     */
    public List<IssueSummary> findByJQL(String query) {

        int total = countByJQL(query);

        List<IssueSummary> issues = Lists.newArrayList();
        int startAt = 0;
        while(issues.size() < total) {
            ClientResponse response = getJQLClientResponse(query, startAt, getBatchSize());
            String jsonResponse = response.getEntity(String.class);
            try {
                JSONObject responseObject = new JSONObject(jsonResponse);
                JSONArray issueEntries = (JSONArray) responseObject.get("issues");
                for (int i = 0; i < issueEntries.length(); i++) {
                    JSONObject issueObject = issueEntries.getJSONObject(i);
                    issues.add(convertToIssueSummary(issueObject));
                }
            } catch (JSONException e) {
                throw new IllegalArgumentException("JSON error", e);
            }
            startAt = startAt + getBatchSize();
        }
        return issues;
    }

    public IssueSummary findByKey(String key) {
        ClientResponse response = getClientResponse(url + "/rest/api/2/issue/" + key);
        String jsonResponse = response.getEntity(String.class);
        try {
            JSONObject responseObject = new JSONObject(jsonResponse);
            return convertToIssueSummary(responseObject);
        } catch (JSONException e) {
            throw new IllegalArgumentException("JSON error", e);
        }
    }

    private IssueSummary convertToIssueSummary(JSONObject issueObject) throws JSONException {
        JSONObject fields = (JSONObject) issueObject.get("fields");
        JSONObject issueType = (JSONObject) fields.get("issuetype");
        return new IssueSummary(uriFrom(issueObject),
                stringValueOf(issueObject.get("key")),
                stringValueOf(fields.get("summary")),
                stringValueOf(fields.get("description")),
                stringValueOf(issueType.get("name")));
    }

    private URI uriFrom(JSONObject issueObject) throws JSONException {
        try {
            return new URI((String) issueObject.get("self"));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Self field not a valid URL");
        }
    }

    public int countByJQL(String query) {
        ClientResponse response = getJQLClientResponse(query, 0, 0);
        String jsonResponse = response.getEntity(String.class);

        int total = 0;
        try {
            JSONObject responseObject = new JSONObject(jsonResponse);
            total = (Integer) responseObject.get("total");
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JQL query: " + query);
        }
        return total;
    }

    private ClientResponse getJQLClientResponse(String query, int startAt, int maxResults) {
        String resourceUrl = null;
        try {
            resourceUrl = url + restJqlQuery(query, startAt, maxResults);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Invalid REST resource: " + query);
        }
        return getClientResponse(resourceUrl);
    }

    private ClientResponse getClientResponse(String resource) {
        String auth = new String(Base64.encode(username + ":" + password));
        Client client = Client.create();
        WebResource webResource = client.resource(resource);
        ClientResponse response = webResource.header("Authorization", "Basic " + auth)
                .type("application/json")
                .accept("application/json")
                .get(ClientResponse.class);

        checkAuthentication(response);
        return response;
    }


    private String stringValueOf(Object summary) {
        if (summary != null) {
            return summary.toString();
        } else {
            return null;
        }
    }

    private void checkAuthentication(ClientResponse response) {
        int statusCode = response.getStatus();
        if (statusCode == 401) {
            throw new IllegalArgumentException("Invalid Username or Password");
        }
    }

    private String restJqlQuery(String jql, int startAt, int batchSize) throws UnsupportedEncodingException {
        String escapedJql = URLEncoder.encode(jql, "UTF-8");
        return "/rest/api/2/search?jql=" + escapedJql
                + "&startAt=" + startAt
                + "&maxResults=" + batchSize
                + "&fields=key,summary,description,issuetype";
    }

    private String restJqlQueryWithAllFields(String jql, int startAt, int batchSize) throws UnsupportedEncodingException {
        String escapedJql = URLEncoder.encode(jql, "UTF-8");
        return "/rest/api/2/search?jql=" + escapedJql
                + "&startAt=" + startAt
                + "&maxResults=" + batchSize
                + "&fields=key,summary,description,issuetype";
    }

    public int getBatchSize() {
        return batchSize;
    }
}
