package net.thucydides.plugins.jira.service;


import ch.lambdaj.function.convert.Converter;
import ch.lambdaj.function.convert.PropertyExtractor;
import net.thucydides.plugins.jira.client.SOAPSession;
import net.thucydides.plugins.jira.guice.Injectors;
import net.thucydides.plugins.jira.model.IssueComment;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.model.IssueTrackerUpdateException;
import sun.misc.JavaSecurityProtectionDomainAccess;
import thucydides.plugins.jira.soap.RemoteComment;
import thucydides.plugins.jira.soap.RemoteFieldValue;
import thucydides.plugins.jira.soap.RemoteIssue;
import thucydides.plugins.jira.soap.RemoteNamedObject;
import thucydides.plugins.jira.soap.RemoteStatus;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ch.lambdaj.Lambda.convert;
import static ch.lambdaj.Lambda.extract;
import static ch.lambdaj.Lambda.on;

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
        configuration = Injectors.getInjector().getInstance(JIRAConfiguration.class);
    }

    protected JIRAConfiguration getConfiguration() {
        return configuration;
    }

    public boolean isWikiRenderedActive() {
        return getConfiguration().isWikiRenderedActive();
    }

    private SOAPSession getSoapSession() throws MalformedURLException, RemoteException {
        if (soapSession == null) {
            soapSession = SOAPSession.openConnectionTo(new URL(getJiraWebserviceUrl()))
                                     .usingCredentials(getJiraUser(), getJiraPassword());
        }
        return soapSession;
    }

    public String getJiraUser() {
        return getConfiguration().getJiraUser();
    }

    public String getJiraPassword() {
        return getConfiguration().getJiraPassword();
    }

    public String getJiraWebserviceUrl() {
        return getConfiguration().getJiraWebserviceUrl();
    }

    @Override
    public String toString() {
        return "Connection to JIRA instance at " + configuration.getJiraWebserviceUrl()
                + " with user " + configuration.getJiraUser();
    }

    /**
     * Add a comment to the specified issue.
     * The author is the JIRA user specified in the *jira.user* system property.
     *
     * @param issueKey the unique key identifying the issue to be commented.
     * @param commentText  text of the comment.
     * @throws IssueTrackerUpdateException
     */
    public void addComment(final String issueKey, final String commentText) throws IssueTrackerUpdateException {

        try {
            String token = getSoapSession().getAuthenticationToken();
            RemoteComment comment = newCommentWithText(commentText);
            getSoapSession().getJiraSoapService().addComment(token, issueKey, comment);


        } catch (IOException e) {
            throw new IssueTrackerUpdateException("Could not update JIRA using URL ("
                                                  + getJiraWebserviceUrl() + ")", e);
        }

    }

    /**
     * Return the comments associated with the specified issue.
     *
     * @param issueKey Identifies the specified issue.
     * @return the list of comments.
     * @throws IssueTrackerUpdateException
     */
    public List<IssueComment> getCommentsFor(String issueKey) throws IssueTrackerUpdateException {
        try {
            String token = getSoapSession().getAuthenticationToken();
            RemoteComment[] comments = getSoapSession().getJiraSoapService().getComments(token, issueKey);
            return convert(comments, new CommentConverter());

        } catch (IOException e) {
            throw new IssueTrackerUpdateException("Could not update JIRA using URL ("
                                                  + getJiraWebserviceUrl() + ")", e);
        }
    }

    public void updateComment(IssueComment issueComment) {
        try {
            String token = getSoapSession().getAuthenticationToken();

            RemoteComment updatedComment = getSoapSession().getJiraSoapService().getComment(token, issueComment.getId());
            updatedComment.setBody(issueComment.getText());

            getSoapSession().getJiraSoapService().editComment(token, updatedComment);
        } catch (IOException e) {
            throw new IssueTrackerUpdateException("Could not update JIRA using URL ("
                                                  + getJiraWebserviceUrl() + ")", e);
        }
    }

    /**
     * Return the current status for a given JIRA issue.
     * Note that the status value depends on the issue workflow, so can be very variable.
     * @param issueKey
     * @return
     * @throws IssueTrackerUpdateException
     */
    public String getStatusFor(final String issueKey) throws IssueTrackerUpdateException {
        try {
            String token = getSoapSession().getAuthenticationToken();

            RemoteIssue issue = getSoapSession().getJiraSoapService().getIssue(token, issueKey);
            checkThatIssueExists(issue, issueKey);
            return getStatusLabel(issue);

        } catch (IOException e) {
            throw new IssueTrackerUpdateException("Could not update JIRA using URL ("
                                                  + getJiraWebserviceUrl() + ")", e);
        }
    }

    public void doTransition(final String issueKey, final String workflowAction) throws IssueTrackerUpdateException {
        try {
            String token = getSoapSession().getAuthenticationToken();
            RemoteIssue issue = getSoapSession().getJiraSoapService().getIssue(token, issueKey);
            checkThatIssueExists(issue, issueKey);

            String actionId = getAvailableActions(issueKey).get(workflowAction);
            if (actionId != null) {
                getSoapSession().getJiraSoapService().progressWorkflowAction(token, issueKey, actionId, null);
            }

        } catch (IOException e) {
            throw new IssueTrackerUpdateException("Could not update JIRA using URL ("
                                                  + getJiraWebserviceUrl() + ")", e);
        }
    }

    private void checkThatActionExists(String actionId, String workflowAction) {
    }

    private String getStatusLabel(final RemoteIssue issue) {
        return getStatusCodeMap().get(issue.getStatus());
    }

    private Map<String, String> getAvailableActions(final String issueKey) {
        Map<String, String> availableActionMap = null;
        if (availableActionMap == null) {
            availableActionMap = new HashMap<String, String>();
            try {
                String token = getSoapSession().getAuthenticationToken();
                RemoteNamedObject[] actions = getSoapSession().getJiraSoapService().getAvailableActions(token, issueKey);
                for(RemoteNamedObject action : actions) {
                    availableActionMap.put(action.getName(), action.getId());
                }
            } catch (IOException e) {
                throw new IssueTrackerUpdateException("Could not read JIRA using URL ("
                                                      + getJiraWebserviceUrl() + ")", e);
            }
        }
        return availableActionMap;
    }

    private Map<String, String> statusCodeMap = null;
    private Map<String, String> getStatusCodeMap() {
        if (statusCodeMap == null) {
            statusCodeMap = new HashMap<String, String>();
            try {
                String token = getSoapSession().getAuthenticationToken();
                RemoteStatus[] statuses = getSoapSession().getJiraSoapService().getStatuses(token);
                for(RemoteStatus status : statuses) {
                    statusCodeMap.put(status.getId(), status.getName());
                }
            } catch (IOException e) {
                throw new IssueTrackerUpdateException("Could not read JIRA using URL ("
                                                      + getJiraWebserviceUrl() + ")", e);
            }
        }
        return statusCodeMap;
    }


    private Map<String, String> statusLabelMap = null;
    private String getStatusId(final String statusLabel) {
        return getStatusLabelMap().get(statusLabel);
    }

    private Map<String, String> getStatusLabelMap() {
        if (statusLabelMap == null) {
            statusLabelMap = new HashMap<String, String>();
            try {
                String token = getSoapSession().getAuthenticationToken();
                RemoteStatus[] statuses = getSoapSession().getJiraSoapService().getStatuses(token);
                for(RemoteStatus status : statuses) {
                    statusLabelMap.put(status.getName(), status.getId());
                }
            } catch (IOException e) {
                throw new IssueTrackerUpdateException("Could not read JIRA using URL ("
                                                      + getJiraWebserviceUrl() + ")", e);
            }
        }
        return statusLabelMap;
    }

    private void checkThatIssueExists(final RemoteIssue issue, final String issueKey) {
        if (issue == null) {
            throw new NoSuchIssueException("No issue found for " + issueKey);
        }
    }

    private RemoteComment newCommentWithText(final String commentText) {
        RemoteComment comment = new RemoteComment();
        comment.setAuthor(getJiraUser());
        comment.setBody(commentText);
        return comment;
    }

    private class CommentConverter implements Converter<RemoteComment, IssueComment> {

        public IssueComment convert(RemoteComment from) {
            return new IssueComment(Long.valueOf(from.getId()), from.getBody(), from.getAuthor());
        }
    }
}
