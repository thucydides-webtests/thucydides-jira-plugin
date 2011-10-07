package net.thucydides.plugins.jira.workflow;

import net.thucydides.core.model.TestResult;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class WhenConfiguringWorkflow {

    @Test
    public void should_be_able_to_load_a_workflow_configuration() {

        InputStream inputStream = Thread.currentThread().getContextClassLoader()
                                                         .getResourceAsStream("default-workflow.groovy");
        Workflow workflow = Workflow.loadFrom(inputStream);
        assertThat(workflow, is(notNullValue()));

        List<String> transitionSetMap = workflow.getTransactions()
                                                .forTestResult(TestResult.SUCCESS)
                                                .whenIssueIs("In Progress");

        assertThat(transitionSetMap.size(), is(2));
        assertThat(transitionSetMap, Matchers.hasItems("Stop Progress","Resolve issue"));

    }


    @Test
    public void should_be_able_to_configure_a_simple_transition() {

        Workflow workflow = new Workflow(
                " when 'Open', {\n" +
                "    'success' should: 'Resolve issue'\n" +
                " }");

        assertThat(workflow, is(notNullValue()));

        List<String> transitionSetMap = workflow.getTransactions()
                                                .forTestResult(TestResult.SUCCESS)
                                                .whenIssueIs("Open");

        assertThat(transitionSetMap.size(), is(1));
        assertThat(transitionSetMap, hasItem("Resolve issue"));

    }

    @Test
    public void should_be_able_to_configure_multiple_transitions() {

        Workflow workflow = new Workflow(
                "             when 'Open', {\n" +
                "                'success' should: 'Resolve issue'\n" +
                "            }\n" +
                "\n" +
                "            when 'Resolved', {\n" +
                "                'failure' should: 'Reopen issue'\n" +
                "            }\n" +
                "\n" +
                "            when 'In Progress', {\n" +
                "                'success' should: ['Stop Progress','Resolve issue']\n" +
                "            }");

        assertThat(workflow, is(notNullValue()));

        List<String> transitionSetMap = workflow.getTransactions()
                                                .forTestResult(TestResult.SUCCESS)
                                                .whenIssueIs("In Progress");

        assertThat(transitionSetMap.size(), is(2));
        assertThat(transitionSetMap, Matchers.hasItems("Stop Progress","Resolve issue"));

    }

}
