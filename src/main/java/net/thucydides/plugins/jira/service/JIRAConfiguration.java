package net.thucydides.plugins.jira.service;

/**
 * JIRA configuration details for the target JIRA instance.
 */
public interface JIRAConfiguration {

    public String getJiraUser();

    public String getJiraPassword();

    public String getJiraWebserviceUrl();

}
