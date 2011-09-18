package net.thucydides.plugins.jira.integration;

import net.thucydides.plugins.jira.client.SOAPSession;
import net.thucydides.plugins.jira.service.IssueTracker;
import net.thucydides.plugins.jira.service.JIRAConfiguration;
import net.thucydides.plugins.jira.service.JiraIssueTracker;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import thucydides.plugins.jira.soap.RemoteComment;
import thucydides.plugins.jira.soap.RemoteIssue;
import thucydides.plugins.jira.soap.RemoteProject;

import java.net.URL;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

public class WhenAddingACommentUsingTheJiraTracker {

    IssueTracker tracker;

    @Mock
    JIRAConfiguration configuration;

    @Before
    public void prepareIssueTracker() {
        MockitoAnnotations.initMocks(this);

        when(configuration.getJiraUser()).thenReturn("bruce");
        when(configuration.getJiraPassword()).thenReturn("batm0bile");
        when(configuration.getJiraWebserviceUrl()).thenReturn("http://ec2-122-248-221-171.ap-southeast-1.compute.amazonaws.com:8081/rpc/soap/jirasoapservice-v2");

        tracker = new JiraIssueTracker() {
            @Override
            protected JIRAConfiguration getConfiguration() {
                return configuration;
            }
        };
    }

    @Test
    public void should_be_able_to_update_a_comment() throws Exception {
        tracker.addComment("THUCINT-1", "Integration test comment");
    }
}
