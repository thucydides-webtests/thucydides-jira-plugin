package net.thucydides.plugins.jira;

import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.model.ReportNamer.ReportType;
import net.thucydides.core.model.Stories;
import net.thucydides.core.model.Story;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.steps.StepListener;
import net.thucydides.core.steps.TestStepResult;
import net.thucydides.plugins.jira.model.IssueComment;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.service.JiraIssueTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static net.thucydides.core.annotations.TestAnnotations.forClass;

/**
 * Updates JIRA issues referenced in a story with a link to the corresponding story report.
 */
public class JiraListener implements StepListener {

    private final IssueTracker issueTracker;

    private Class<?> currentTestCase;
    public Story currentStory;

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraListener.class);

    public JiraListener(IssueTracker issueTracker) {
        this.issueTracker = issueTracker;
    }

    public JiraListener() {
        this(new JiraIssueTracker());
    }

    protected IssueTracker getIssueTracker() {
        return issueTracker;
    }

    public void testSuiteStarted(final Class<?> testCase) {
        this.currentTestCase = testCase;
        this.currentStory = null;
    }

    public void testSuiteStarted(final Story story) {
        this.currentStory = story;
        this.currentTestCase = null;
    }

    public void testStarted(final String testName) {
        List<String> issues = stripInitialHashesFrom(forClass(currentTestCase).getAnnotatedIssuesForMethod(testName));
        for(String issueId : issues) {
            logIssueTracking(issueId);
            if (!dryRun()) {
                addOrUpdateCommentFor(issueId);
            }
        }
    }

    private void addOrUpdateCommentFor(final String issueId) {
        List<IssueComment> comments = issueTracker.getCommentsFor(issueId);
        IssueComment existingComment = findExistingCommentIn(comments);
        if (existingComment != null) {
            IssueComment updatedComment = new IssueComment(existingComment.getId(),
                                                           linkToReport(),
                                                           existingComment.getAuthor());
            issueTracker.updateComment(updatedComment);
        } else {
            issueTracker.addComment(issueId, linkToReport());
        }

    }

    private IssueComment findExistingCommentIn(List<IssueComment> comments) {
        for(IssueComment comment : comments) {
            if (comment.getText().contains("Thucydides Test Results")) {
                return comment;
            }
        }
        return null;
    }

    private void logIssueTracking(final String issueId) {
        if (dryRun()) {
            LOGGER.info("--- DRY RUN ONLY: JIRA WILL NOT BE UPDATED ---");
        }
        LOGGER.info("Updating JIRA issue: " + issueId);
        LOGGER.info("JIRA server: " + issueTracker.toString());
    }

    private boolean dryRun() {
        return Boolean.valueOf(System.getProperty("thucydides.skip.jira.updates"));
    }

    private String linkToReport() {
        String reportUrl = ThucydidesSystemProperty.getValue(ThucydidesSystemProperty.PUBLIC_URL);
        String reportName = Stories.reportFor(storyUnderTest(), ReportType.HTML);
        return "[Thucydides Test Results|" + reportUrl + "/" + reportName + "]";
    }

    private Story storyUnderTest() {
        if (currentTestCase != null) {
            return Stories.findStoryFrom(currentTestCase);
        } else {
            return currentStory;
        }
    }

    private List<String> stripInitialHashesFrom(final List<String> issueNumbers) {
        List<String> issues = new ArrayList<String>();
        for(String issueNumber : issueNumbers) {
            issues.add(issueNumber.substring(1));
        }
        return issues;
    }

    public void testFinished(TestStepResult testStepResult){
        
    }

    public void stepStarted(ExecutedStepDescription executedStepDescription) {
        
    }

    public void stepFailed(StepFailure stepFailure) {
        
    }

    public void stepIgnored() {
        
    }

    public void stepPending() {
        
    }

    public void stepFinished() {
        
    }

    public void testFailed(Throwable throwable) {
        
    }

    public void testIgnored() {
        
    }
}
