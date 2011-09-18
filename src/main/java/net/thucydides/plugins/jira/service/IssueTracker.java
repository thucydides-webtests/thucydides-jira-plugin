package net.thucydides.plugins.jira.service;

/**
 * An interface to an issue tracking system.
 * Should allow a client to connect to an issue tracking system, retrieve comments for an existing issue, and
 * add new comments.
 */
public interface IssueTracker {
    /**
     * Add a new comment to the specified issue in the remote issue tracking system.
     * @param issueKey the unique key identifying the issue to be commented.
     * @param the text of the comment.
     */
    void addComment(final String issueKey, final String commentText)  throws IssueTrackerUpdateException;
}
