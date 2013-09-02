package net.thucydides.plugins.jira.client;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import net.thucydides.plugins.jira.domain.IssueSummary;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * A JIRA client using the new REST interface
 */
public class JerseyJiraClient {

    private static final String REST_SEARCH = "rest/api/2/search";
    private static final int REDIRECT_REQUEST = 302;
    private final String url;
    private final String username;
    private final String password;
    private final int batchSize;

    private final static int DEFAULT_BATCH_SIZE = 100;
    private final static int OK = 200;

    public JerseyJiraClient(String url, String username, String password) {
        this(url, username, password, DEFAULT_BATCH_SIZE);
    }

    public JerseyJiraClient(String url, String username, String password, int batchSize) {
        System.out.println("TARGET JIRA INSTANCE: " + url);
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
    public List<IssueSummary> findByJQL(String query) throws JSONException {

        int total = countByJQL(query);

        List<IssueSummary> issues = Lists.newArrayList();
        int startAt = 0;
        while(issues.size() < total) {

            String jsonResponse = getJSONResponse(query, startAt);

            JSONObject responseObject = new JSONObject(jsonResponse);
            JSONArray issueEntries = (JSONArray) responseObject.get("issues");
            for (int i = 0; i < issueEntries.length(); i++) {
                JSONObject issueObject = issueEntries.getJSONObject(i);
                issues.add(convertToIssueSummary(issueObject));
            }
            startAt = startAt + getBatchSize();
        }
        return issues;
    }

    public WebTarget buildWebTargetFor(String path) {
        return restClient().target(url).path(path);
    }

    private String getJSONResponse(String query, int startAt) throws JSONException{
        WebTarget target = buildWebTargetFor(REST_SEARCH)
                                            .queryParam("jql", query)
                                            .queryParam("startAt", startAt)
                                            .queryParam("maxResults", batchSize)
                                            .queryParam("expand", "renderedFields")
                                            .queryParam("fields", "key,summary,description,issuetype,labels");
        Response response = target.request().get();
        checkValid(response);
        return response.readEntity(String.class);
    }

    public Optional<IssueSummary> findByKey(String key) throws JSONException {

        Optional<String> jsonResponse = getClientResponse(url, "rest/api/2/issue/" + key);

        if (jsonResponse.isPresent()) {
            JSONObject responseObject = new JSONObject(jsonResponse.get());
            return Optional.of(convertToIssueSummary(responseObject));
        }
        return Optional.absent();
    }

    public Map<String, IssueSummary> findRelatedIssues(String key) {
        throw new UnsupportedOperationException();
    }

    private IssueSummary convertToIssueSummary(JSONObject issueObject) throws JSONException {
        JSONObject fields = (JSONObject) issueObject.get("fields");
        JSONObject renderedFields = (JSONObject) issueObject.get("renderedFields");
        JSONObject issueType = (JSONObject) fields.get("issuetype");
        String renderedDescription = renderedFields.getString("description");
        return new IssueSummary(uriFrom(issueObject),
                issueObject.getLong("id"),
                stringValueOf(issueObject.get("key")),
                stringValueOf(fields.get("summary")),
                stringValueOf(fields.get("description")),
                renderedDescription,
                stringValueOf(issueType.get("name")),
                toList((JSONArray)fields.get("labels")));
    }

    private List<String> toList(JSONArray array) throws JSONException {
        List<String> list = Lists.newArrayList();
        for (int i = 0; i < array.length(); i++) {
            list.add(stringValueOf(array.get(i)));
        }
        return list;
    }

    private URI uriFrom(JSONObject issueObject) throws JSONException {
        try {
            return new URI((String) issueObject.get("self"));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Self field not a valid URL");
        }
    }

    public int countByJQL(String query) throws JSONException{
        System.out.println("JIRA COUNT BY JQL: " + query);

        WebTarget target = buildWebTargetFor(REST_SEARCH).queryParam("jql", query);
        Response response = target.request().get();

        if (isEmpty(response)) {
            return 0;
        } else {
            checkValid(response);
        }

        String jsonResponse = response.readEntity(String.class);

        int total = 0;
        try {
            JSONObject responseObject = new JSONObject(jsonResponse);
            total = (Integer) responseObject.get("total");
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JQL query: " + query);
        }
        return total;
    }

    private Optional<String> getClientResponse(String url, String path) throws JSONException {
        System.out.println("CALLING JIRA REST CLIENT: URL=" + url + ", path=" +path);
        WebTarget target = restClient().target(url)
                                       .path(path)
                                       .queryParam("expand", "renderedFields");
        Response response = target.request().get();

        if (response.getStatus() == REDIRECT_REQUEST) {
            response = Redirector.forPath(path).usingClient(restClient()).followRedirectsIn(response);
        }

        if (resourceDoesNotExist(response)) {
            return Optional.absent();
        } else {
            checkValid(response);
            return Optional.of(response.readEntity(String.class));
        }
    }

    public Client restClient() {
        return ClientBuilder.newBuilder().register(new HttpBasicAuthFilter(username, password)).build();
    }

    private String stringValueOf(Object field) {
        if (field != null) {
            return field.toString();
        } else {
            return null;
        }
    }

    public boolean resourceDoesNotExist(Response response) {
        return response.getStatus() == 404;
    }

    public boolean isEmpty(Response response) {
        return response.getStatus() == 400;
    }

    public void checkValid(Response response) throws JSONException {
        int status = response.getStatus();
        if (status != OK) {
            switch(status) {
                case 401 : throw new IllegalArgumentException("Authentication error (401) for user " + this.username);
                case 403 : throw new IllegalArgumentException("Forbidden error (403) for user " + this.username);
                case 404 : throw new IllegalArgumentException("Service not found (404) - try checking the JIRA URL?");
                case 407 : throw new IllegalArgumentException("Proxy authentication required (407)");
                default:
                    throw new JSONException("JIRA query failed: error " + status);
            }
        }
    }

    public int getBatchSize() {
        return batchSize;
    }
}
