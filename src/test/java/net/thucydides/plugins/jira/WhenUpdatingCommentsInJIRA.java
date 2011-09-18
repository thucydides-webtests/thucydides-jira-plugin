package net.thucydides.plugins.jira;

import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.annotations.Feature;
import net.thucydides.core.annotations.Story;
import net.thucydides.core.annotations.Title;
import net.thucydides.core.junit.rules.SaveWebdriverSystemPropertiesRule;
import net.thucydides.core.steps.TestStepResult;
import net.thucydides.plugins.jira.service.IssueTracker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.contains;
import static org.mockito.Mockito.verify;

public class WhenUpdatingCommentsInJIRA {

    @Feature
    public static final class SampleFeature {
        public class SampleStory {}
        public class SampleStory2 {}
    }

    @Story(SampleFeature.SampleStory.class)
    private static final class SampleTestSuite {

        @Title("Test for issue #MYPROJECT-123")
        public void issue_123_should_be_fixed_now() {}

        @Title("Fixes issues #MYPROJECT-123,#MYPROJECT-456")
        public void issue_123_and_456_should_be_fixed_now() {}

        public void anotherTest() {}
    }

    @Rule
    public SaveWebdriverSystemPropertiesRule saveWebdriverSystemPropertiesRule = new SaveWebdriverSystemPropertiesRule();

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Mock IssueTracker issueTracker;
    @Mock TestStepResult result;

    @Test
    public void when_a_test_with_a_referenced_issue_finishes_the_plugin_should_add_a_new_comment_for_this_issue() {
        JiraListener listener = new JiraListener(issueTracker);

        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);

        verify(issueTracker).addComment(eq("MYPROJECT-123"), anyString());
    }

    @Test
    public void when_a_test_with_several_referenced_issues_finishes_the_plugin_should_add_a_new_comment_for_each_issue() {
        JiraListener listener = new JiraListener(issueTracker);

        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_and_456_should_be_fixed_now");
        listener.testFinished(result);

        verify(issueTracker).addComment(eq("MYPROJECT-123"), anyString());
        verify(issueTracker).addComment(eq("MYPROJECT-456"), anyString());
    }

    @Test
    public void the_comment_should_contain_a_link_to_the_corresponding_story_report() {
        JiraListener listener = new JiraListener(issueTracker);
        System.setProperty("thucydides.public.url", "http://my.server/myproject/thucydides");
        listener.testSuiteStarted(SampleTestSuite.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);

        verify(issueTracker).addComment(eq("MYPROJECT-123"),
                                        contains("<a href=\"http://my.server/myproject/thucydides/sample_story.html\">Thucyides Test Results</a>"));
    }

}
