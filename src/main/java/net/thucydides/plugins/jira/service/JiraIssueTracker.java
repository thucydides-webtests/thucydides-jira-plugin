package net.thucydides.plugins.jira.service;


import net.thucydides.plugins.jira.client.SOAPSession;
import thucydides.plugins.jira.soap.RemoteComment;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

/**
 * Update comments in JIRA issues with links to Thucydides reports.
 * This plugin will use the JIRA username and password provided in the <b>jira.username</b>
 * and <b>jira.password</b> system properties. The URL of the JIRA instance should be provided
 * using the <b>jira.url</b> system property.
 */
public class JiraIssueTracker implements IssueTracker {


    private final JIRAConfiguration configuration;
    private SOAPSession soapSession;

    public JiraIssueTracker() {
        configuration = new SystemPropertiesJIRAConfiguration();
    }

    protected JIRAConfiguration getConfiguration() {
        return configuration;
    }

    private SOAPSession getSoapSession() throws MalformedURLException, RemoteException {
        if (soapSession == null) {
            soapSession = SOAPSession.openConnectionTo(new URL(getJiraWebserviceUrl()))
                                     .usingCredentials(getJiraUser(), getJiraPassword());
        }
        return soapSession;
    }

    protected String getJiraUser() {
        return getConfiguration().getJiraUser();
    }

    protected String getJiraPassword() {
        return getConfiguration().getJiraPassword();
    }

    protected String getJiraWebserviceUrl() {
        return getConfiguration().getJiraWebserviceUrl();
    }

    public void addComment(final String issueKey, final String commentText) throws IssueTrackerUpdateException {

        try {
            String token = getSoapSession().getAuthenticationToken();
            RemoteComment comment = newCommentWithText(commentText);
            getSoapSession().getJiraSoapService().addComment(token, issueKey, comment);
        } catch (MalformedURLException e) {
            throw new IssueTrackerUpdateException("Could not update JIRA using URL ("
                                                  + getJiraWebserviceUrl() + ")", e);
        } catch (RemoteException e) {
            throw new IssueTrackerUpdateException("Could not update JIRA at URL ("
                                                  + getJiraWebserviceUrl() + ")", e);
        }

    }

    private RemoteComment newCommentWithText(String commentText) {
        RemoteComment comment = new RemoteComment();
        comment.setAuthor(getJiraUser());
        comment.setBody(commentText);
        return comment;
    }
}
