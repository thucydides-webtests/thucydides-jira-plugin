package net.thucydides.plugins.jira.client;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import net.thucydides.plugins.jira.domain.IssueSummary;
import net.thucydides.plugins.jira.domain.Version;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

/**
 * A JIRA client using the new REST interface
 */
public class JerseyJiraClient {

    private static final String REST_SEARCH = "rest/api/latest/search";
    private static final String VERSIONS_SEARCH = "rest/api/latest/project/%s/versions";
    private static final int REDIRECT_REQUEST = 302;
    private final String url;
    private final String username;
    private final String password;
    private final int batchSize;

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(JerseyJiraClient.class);

    private final static int DEFAULT_BATCH_SIZE = 100;
    private final static int OK = 200;

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
     * @param query A valid JQL query
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


    public List<Version> findVersionsForProject(String projectName) throws JSONException {
        List<Version> versions = Lists.newArrayList();

        String versionData = getJSONProjectVersions(projectName);
        JSONArray versionEntries = new JSONArray(versionData);
        for (int i = 0; i < versionEntries.length(); i++) {
            JSONObject issueObject = versionEntries.getJSONObject(i);
            versions.add(convertToVersion(issueObject));
        }
        return versions;
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

    private String getJSONProjectVersions(String projectName) throws JSONException{
        String url = String.format(VERSIONS_SEARCH,projectName);
        WebTarget target = buildWebTargetFor(url);
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

    private Version convertToVersion(JSONObject issueObject) throws JSONException {
        try {
            return new Version(uriFrom(issueObject),
                    issueObject.getLong("id"),
                    stringValueOf(issueObject.get("name")),
                    booleanValueOf(issueObject.get("archived")),
                    booleanValueOf(issueObject.get("released")));
        } catch (JSONException e) {
            logger.error("Could not load issue from JSON",e);
            logger.error("JSON:" + issueObject.toString(4));
            throw e;
        }
    }

    private IssueSummary convertToIssueSummary(JSONObject issueObject) throws JSONException {

        JSONObject fields = (JSONObject) issueObject.get("fields");
        JSONObject renderedFields = (JSONObject) issueObject.get("renderedFields");
        JSONObject issueType = (JSONObject) fields.get("issuetype");
        String renderedDescription = stringValueOf(optional(renderedFields,"description"));
        try {
            return new IssueSummary(uriFrom(issueObject),
                    issueObject.getLong("id"),
                    stringValueOf(issueObject.get("key")),
                    stringValueOf(fields.get("summary")),
                    stringValueOf(optional(fields,"description")),
                    renderedDescription,
                    stringValueOf(issueType.get("name")),
                    toList((JSONArray) fields.get("labels")));
        } catch (JSONException e) {
            logger.error("Could not load issue from JSON",e);
            logger.error("JSON:" + issueObject.toString(4));
            throw e;
        }
    }

    private Object optional(JSONObject fields, String fieldName) throws JSONException {
        return (fields.has(fieldName) ? fields.get(fieldName) : null);
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
        WebTarget target = buildWebTargetFor(REST_SEARCH).queryParam("jql", query);
        Response response = target.request().get();

        if (isEmpty(response)) {
            return 0;
        } else {
            checkValid(response);
        }

        String jsonResponse = response.readEntity(String.class);

        int total;
        try {
            JSONObject responseObject = new JSONObject(jsonResponse);
            total = (Integer) responseObject.get("total");
        } catch (JSONException e) {
            throw new IllegalArgumentException("Invalid JQL query: " + query);
        }
        return total;
    }

    private Optional<String> getClientResponse(String url, String path) throws JSONException {
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

    private boolean booleanValueOf(Object field) {
        if (field != null) {
            return Boolean.valueOf(field.toString());
        } else {
            return false;
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
