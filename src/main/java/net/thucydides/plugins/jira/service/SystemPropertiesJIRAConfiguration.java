package net.thucydides.plugins.jira.service;

import com.google.inject.Inject;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.util.EnvironmentVariables;

/**
 * Obtain the JIRA configuration details from system properties.
 */
public class SystemPropertiesJIRAConfiguration implements JIRAConfiguration {

    private final EnvironmentVariables environmentVariables;

    public static final String JIRA_URL = "jira.url";
    public static final String JIRA_PROJECT = "jira.project";
    public static final String JIRA_USERNAME = "jira.username";
    public static final String JIRA_PASSWORD = "jira.password";
    public static final String JIRA_WIKI_RENDERER = "jira.wiki.renderer";

    @Inject
    public SystemPropertiesJIRAConfiguration(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
    }

    public String getJiraUser() {
        return environmentVariables.getProperty(JIRA_USERNAME);
    }

    public String getJiraPassword() {
        return environmentVariables.getProperty(JIRA_PASSWORD);
    }

    public boolean isWikiRenderedActive() {
        return Boolean.valueOf(environmentVariables.getProperty(JIRA_WIKI_RENDERER, "true"));
    }

    public String getProject() {
        return environmentVariables.getProperty(JIRA_PROJECT);
    }

    public String getJiraWebserviceUrl() {
        String baseUrl = environmentVariables.getProperty(JIRA_URL);
        return (baseUrl != null) ? baseUrl + "/rpc/soap/jirasoapservice-v2" : null;
    }


}
