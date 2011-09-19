package net.thucydides.plugins.jira.integration;

import net.thucydides.plugins.jira.model.IssueComment;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.service.JIRAConfiguration;
import net.thucydides.plugins.jira.service.JiraIssueTracker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class WhenAddingACommentUsingTheJiraTracker {

    IssueTracker tracker;

    private static final String JIRA_WEBSERVICE_URL = "http://ec2-122-248-221-171.ap-southeast-1.compute.amazonaws.com:8081/rpc/soap/jirasoapservice-v2";

    private String issueKey;

    private IssueHarness testIssueHarness;

    @Mock
     JIRAConfiguration configuration;

     @Before
     public void prepareIssueTracker() throws Exception {
         MockitoAnnotations.initMocks(this);

         when(configuration.getJiraUser()).thenReturn("bruce");
         when(configuration.getJiraPassword()).thenReturn("batm0bile");
         when(configuration.getJiraWebserviceUrl()).thenReturn(JIRA_WEBSERVICE_URL);

         testIssueHarness = new IssueHarness(JIRA_WEBSERVICE_URL);
         issueKey = testIssueHarness.createTestIssue();

         tracker = new JiraIssueTracker() {
             @Override
             protected JIRAConfiguration getConfiguration() {
                 return configuration;
             }
         };
     }


    @After
    public void  deleteTestIssue() throws Exception {
        testIssueHarness.deleteTestIssues();
    }

    @Test
    public void should_be_able_to_add_a_comment_to_an_issue() throws Exception {
        List<IssueComment> comments = tracker.getCommentsFor(issueKey);
        assertThat(comments.size(), is(0));

        tracker.addComment(issueKey, "Integration test comment");

        comments = tracker.getCommentsFor(issueKey);
        assertThat(comments.size(), is(1));
    }

    @Test
    public void should_be_able_to_update_a_comment_from_an_issue() throws Exception {
        tracker.addComment(issueKey, "Integration test comment 1");
        tracker.addComment(issueKey, "Integration test comment 2");
        tracker.addComment(issueKey, "Integration test comment 3");

        List<IssueComment> comments = tracker.getCommentsFor(issueKey);

        IssueComment oldComment = comments.get(0);
        IssueComment updatedComment = new IssueComment(oldComment.getId(), "Integration test comment 4", oldComment.getAuthor());

        tracker.updateComment(updatedComment);

        comments = tracker.getCommentsFor(issueKey);
        assertThat(comments.get(0).getText(), is("Integration test comment 4"));
    }

}
