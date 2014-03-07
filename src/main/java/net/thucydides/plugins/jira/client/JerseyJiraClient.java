package net.thucydides.plugins.jira.client;

import com.beust.jcommander.internal.Maps;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.thucydides.plugins.jira.domain.IssueSummary;
import net.thucydides.plugins.jira.domain.Version;
import net.thucydides.plugins.jira.model.CascadingSelectOption;
import net.thucydides.plugins.jira.model.CustomField;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.Collections.EMPTY_LIST;

/**
 * A JIRA client using the new REST interface
 */
@SuppressWarnings("unchecked")
public class JerseyJiraClient {

    private static final String REST_SEARCH = "rest/api/latest/search";
    private static final String VERSIONS_SEARCH = "rest/api/latest/project/%s/versions";
    private static final int REDIRECT_REQUEST = 302;
    private static final String DEFAULT_ISSUE_TYPE = "Bug";
    private final String url;
    private final String username;
    private final String password;
    private final int batchSize;
    private final String project;
    private final List<String> customFields;
    private Map<String, CustomField> customFieldsIndex;
    private String metadataIssueType;
    private LoadingCache<String , Optional<IssueSummary>> issueSummaryCache;
    private LoadingCache<String , List<IssueSummary>> issueQueryCache;

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(JerseyJiraClient.class);

    private final static int DEFAULT_BATCH_SIZE = 100;
    private final static int OK = 200;

    public JerseyJiraClient(String url, String username, String password, String project) {
        this(url, username, password, DEFAULT_BATCH_SIZE, project);
    }


    public JerseyJiraClient(String url, String username, String password, int batchSize,
                            String project,
                            String metadataIssueType,
                            List<String> customFields) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.batchSize = batchSize;
        this.project = project;
        this.metadataIssueType = metadataIssueType;
        this.customFields = ImmutableList.copyOf(customFields);
        this.issueSummaryCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(new FindByKeyLoader(this));
        this.issueQueryCache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .build(new FindByJQLLoader(this));
    }

    public JerseyJiraClient(String url, String username, String password, int batchSize, String project) {
        this(url,username,password,batchSize,project, DEFAULT_ISSUE_TYPE, EMPTY_LIST);
    }

    public JerseyJiraClient usingCustomFields(List<String> customFields) {
        return new JerseyJiraClient(url, username, password, batchSize, project, metadataIssueType, customFields);
    }

    public JerseyJiraClient usingMetadataIssueType(String metadataIssueType) {
        return new JerseyJiraClient(url, username, password, batchSize, project, metadataIssueType, customFields);
    }

    /**
     * Load the issue keys for all of the issues matching the specified JQL query
     *
     * @param query A valid JQL query
     * @return a list of JIRA issue keys
     */
    public List<IssueSummary> findByJQL(String query) throws JSONException {
        try {
            return issueQueryCache.get(query);
        } catch (ExecutionException e) {
            throw new JSONException(e.getCause());
        }
    }

    protected List<IssueSummary> loadByJQL(String query) throws JSONException {

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
        String versionData = getJSONProjectVersions(projectName);
        return convertJSONVersions(versionData);
    }

    private List<Version> convertJSONVersions(String versionData) throws JSONException {
        List<Version> versions = Lists.newArrayList();

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
                                            .queryParam("fields", "key,summary,description,issuetype,labels,fixVersions");
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
        try {
            return issueSummaryCache.get(key);
        } catch (ExecutionException e) {
            throw new JSONException(e.getCause());
        }
    }

    public Optional<IssueSummary> loadByKey(String key) throws JSONException {

        Optional<String> jsonResponse = readFieldValues(url, "rest/api/2/issue/" + key);

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
                    toList((JSONArray) fields.get("labels")),
                    toListOfVersions((JSONArray) fields.get("fixVersions")),
                    customFieldValuesIn(fields));
        } catch (JSONException e) {
            logger.error("Could not load issue from JSON",e);
            logger.error("JSON:" + issueObject.toString(4));
            throw e;
        }
    }

    private Map<String, Object> customFieldValuesIn(JSONObject fields) throws JSONException {
        Map<String, Object> customFieldValues = Maps.newHashMap();
        for (String customFieldName : customFields) {
            CustomField customField = getCustomFieldsIndex().get(customFieldName);
            if (customFieldDefined(fields, customField)) {
                Object customFieldValue = readFieldValue(fields, customField);
                customFieldValues.put(customFieldName, customFieldValue);
            }
        }
        return customFieldValues;
    }

    private boolean customFieldDefined(JSONObject fields, CustomField customField) throws JSONException {
        return (customField != null) && (fields.has(customField.getId())) && (!fields.get(customField.getId()).equals(null));
    }

    private Object readFieldValue(JSONObject fields, CustomField customField) throws JSONException {
        JSONObject field = new JSONObject(fields.getString(customField.getId()));

        if (customField.getType().equals("string")) {
            return field.getString("value");
        } else if (customField.getType().equals("array")) {
            return readListFrom(field);
        }
        return field.get("value");
    }

    private List<String> readListFrom(JSONObject jsonField) throws JSONException {
        List<String> values = Lists.newArrayList();
        values.add(jsonField.getString("value"));
        if (jsonField.has("child")) {
            values.addAll(readListFrom(jsonField.getJSONObject("child")));
        }
        return values;
    }

    private List<CustomField> convertToCustomFields(JSONArray customFieldsList) throws JSONException {

        List<CustomField> customFields = Lists.newArrayList();

        for (int i = 0; i < customFieldsList.length(); i++) {
            JSONObject fieldObject = customFieldsList.getJSONObject(i);
            customFields.add(convertToCustomField(fieldObject));
        }
        return customFields;

    }

    private CustomField convertToCustomField(JSONObject fieldObject) throws JSONException {
        return new CustomField(fieldObject.getString("id"),
                fieldObject.getString("name"),
                fieldTypeOf(fieldObject));
    }

    private String fieldTypeOf(JSONObject fieldObject) throws JSONException {
        if (fieldObject.has("schema")) {
            return fieldObject.getJSONObject("schema").getString("type");
        } else {
            return "string";
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

    private List<String> toListOfVersions(JSONArray array) throws JSONException {
        List<String> list = Lists.newArrayList();
        for (int i = 0; i < array.length(); i++) {
            JSONObject versionObject = (JSONObject) array.get(i);
            list.add(versionObject.getString("name"));
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

    public Integer countByJQL(String query) throws JSONException{
        return loadCountByJQL(query);
//        try {
//            return issueCountCache.get(query);
//        } catch (ExecutionException e) {
//            throw new JSONException(e.getCause());
//        }
    }

    protected Integer loadCountByJQL(String query) throws JSONException{
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

    private Optional<String> readFieldValues(String url, String path) throws JSONException {
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

    private Optional<String> readFieldMetadata(String url, String path) throws JSONException {
        WebTarget target = restClient().target(url)
                .path(path)
                .queryParam("expand", "renderedFields")
                .queryParam("project", project)
                .queryParam("issuetypeName",metadataIssueType)
                .queryParam("expand","projects.issuetypes.fields");

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
                case 401 : handleAuthenticationError("Authentication error (401) for user " + this.username);
                case 403 : handleAuthenticationError("Forbidden error (403) for user " + this.username);
                case 404 : handleConfigurationError("Service not found (404) - try checking the JIRA URL?");
                case 407 : handleConfigurationError("Proxy authentication required (407)");
                default:
                    throw new JSONException("JIRA query failed: error " + status);
            }
        }
    }

    private void handleAuthenticationError(String message) {
        throw new JIRAAuthenticationError(message);
    }

    private void handleConfigurationError(String message) {
        throw new JIRAConfigurationError(message);
    }


    public int getBatchSize() {
        return batchSize;
    }

    private Map<String, CustomField> getCustomFieldsIndex() throws JSONException {
        if (customFieldsIndex == null) {
             customFieldsIndex = indexCustomFields();
        }
        return customFieldsIndex;
    }

    private Map<String, CustomField> indexCustomFields() throws JSONException {
        Map<String, CustomField> index = Maps.newHashMap();
        for(CustomField field : getExistingCustomFields()) {
            index.put(field.getName(), field);
        }
        return index;
    }

    private List<CustomField> getExistingCustomFields() throws JSONException {

        Optional<String> jsonResponse = readFieldValues(url, "rest/api/2/field");

        if (jsonResponse.isPresent()) {
            JSONArray responseObject = new JSONArray(jsonResponse.get());
            return convertToCustomFields(responseObject);
        }
        return EMPTY_LIST;
    }

    List<CustomField> getCustomFields() throws JSONException {
        List<CustomField> registeredCustomFields = Lists.newArrayList();
        for(String fieldName : customFields) {
            registeredCustomFields.add(getCustomFieldsIndex().get(fieldName));
        }
        return registeredCustomFields;
    }

    public List<CascadingSelectOption> findOptionsForCascadingSelect(String fieldName) {
        JSONObject responseObject = null;
        try {
            Optional<String> jsonResponse = readFieldMetadata(url, "rest/api/2/issue/createmeta");
            if (jsonResponse.isPresent()) {
                responseObject = new JSONObject(jsonResponse.get());

                JSONObject fields = responseObject.getJSONArray("projects")
                        .getJSONObject(0)
                        .getJSONArray("issuetypes")
                        .getJSONObject(0).getJSONObject("fields");

                Iterator fieldKeys = fields.keys();

                while(fieldKeys.hasNext()) {
                    String entryFieldName = (String) fieldKeys.next();
                    JSONObject entry = fields.getJSONObject(entryFieldName);
                    if (entry.getString("name").equalsIgnoreCase(fieldName)) {
                        return convertToCascadingSelectOptions(entry.getJSONArray("allowedValues"));
                    }
                }
            }
        } catch (JSONException e) {
            logger.error("Could not read cascading select options", e);
            logger.info("responseObject = " + responseObject);

        }
        return EMPTY_LIST;
    }

    private List<CascadingSelectOption> convertToCascadingSelectOptions(JSONArray allowedValues) throws JSONException {
        return convertToCascadingSelectOptions(allowedValues, null);
    }

    private List<CascadingSelectOption> convertToCascadingSelectOptions(JSONArray allowedValues,
                CascadingSelectOption parentOption) throws JSONException {
        List<CascadingSelectOption> options = Lists.newArrayList();
        for(int i = 0; i < allowedValues.length(); i++) {
            JSONObject entry = (JSONObject) allowedValues.get(i);
            String value = entry.getString("value");

            CascadingSelectOption option = new CascadingSelectOption(value, parentOption);
            List<CascadingSelectOption> children = Lists.newArrayList();
            if (entry.has("children")) {
                children = convertToCascadingSelectOptions(entry.getJSONArray("children"), option);
            }
            option.addChildren(children);
            options.add(option);
        }
        return options;
    }

}
