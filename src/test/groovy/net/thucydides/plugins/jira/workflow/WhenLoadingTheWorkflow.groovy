package net.thucydides.plugins.jira.workflow

import net.thucydides.plugins.jira.guice.Injectors
import spock.lang.Specification
import static net.thucydides.core.model.TestResult.FAILURE
import static net.thucydides.core.model.TestResult.IGNORED
import static net.thucydides.core.model.TestResult.PENDING
import static net.thucydides.core.model.TestResult.SKIPPED
import static net.thucydides.core.model.TestResult.SUCCESS

class WhenLoadingTheWorkflow extends Specification {

    def cleanup() {
        System.properties.remove('thucydides.jira.workflow')
        System.properties.remove('thucydides.jira.workflow.active')
    }

    def "should look for the jira-workflow.groovy configuration file by default"() {
        when:
            def workflowLoader = Injectors.getInjector().getInstance(WorkflowLoader)

        then:
            workflowLoader.defaultWorkflow == 'jira-workflow.groovy'
    }

    def "should try to load the system-defined workflow configuration if provided"() {
        given:
            System.properties['thucydides.jira.workflow'] = 'custom-workflow.groovy'
        when:
            def workflowLoader = Injectors.getInjector().getInstance(WorkflowLoader)
            Workflow workflow = workflowLoader.load()
        then:
            workflow.name == 'custom-workflow.groovy'
    }

    def "should load the default workflow if available and no system property is set"() {
        given:
            ClasspathWorkflowLoader workflowLoader = new ClasspathWorkflowLoader('workflow-by-default.groovy')
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.name == 'workflow-by-default.groovy'
    }

    def "the workflow specified by the system property should override the default workflow"() {
        given:
            ClasspathWorkflowLoader workflowLoader = new ClasspathWorkflowLoader('workflow-by-default.groovy')
        and:
            System.properties['thucydides.jira.workflow'] = 'custom-workflow.groovy'
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.name == 'custom-workflow.groovy'
    }

    def "when the system-specified workflow does not exist default to the convention-based workflow"() {
        given:
            ClasspathWorkflowLoader workflowLoader = new ClasspathWorkflowLoader('workflow-by-default.groovy')
        and:
            System.properties['thucydides.jira.workflow'] = 'does-not-exist-workflow.groovy'
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.name == 'workflow-by-default.groovy'
    }

    def "when the system-specified and the convention-based workflows do not exist default to the bundledworkflow"() {
        given:
            ClasspathWorkflowLoader workflowLoader = new ClasspathWorkflowLoader('default-workflow-does-not-exist.groovy')
        and:
            System.properties['thucydides.jira.workflow'] = 'does-not-exist-workflow.groovy'
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.name == 'default-workflow.groovy'
    }


    def "should use the default workflow if no jira-workflow.groovy file is found"() {
        given:
            ClasspathWorkflowLoader workflowLoader = new ClasspathWorkflowLoader('jira-workflow.groovy')
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.name == 'default-workflow.groovy'
    }

    def "should activate workflow updates if the workflow is defined by the system property and exists"() {
        given:
            ClasspathWorkflowLoader workflowLoader = new ClasspathWorkflowLoader('workflow-by-default.groovy')
        and:
            System.properties['thucydides.jira.workflow'] = 'custom-workflow.groovy'
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.active == true
    }

    def "should not activate workflow updates if the workflow is defined by the system property but does not exist"() {
        given:
            ClasspathWorkflowLoader workflowLoader = new ClasspathWorkflowLoader('workflow-by-default.groovy')
        and:
            System.properties['thucydides.jira.workflow'] = 'workflow-that-does-not-exist.groovy'
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.active == false
    }


    def "should activate workflow updates if the convention-based workflow file exists"() {
        given:
            ClasspathWorkflowLoader workflowLoader = new ClasspathWorkflowLoader('workflow-by-default.groovy')
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.active == true
    }

    def "should not activate workflow updates if the convention-based workflow file does not exist"() {
        given:
            ClasspathWorkflowLoader workflowLoader = new ClasspathWorkflowLoader('does-not-exist.groovy')
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.active == false
    }

    def "should activate default workflow if the corresponding system property is set"() {
        given:
            System.properties['thucydides.jira.workflow.active'] = 'true'
            def workflowLoader = Injectors.getInjector().getInstance(WorkflowLoader)
        when:
            Workflow workflow = workflowLoader.load()
        then:
            workflow.name == 'default-workflow.groovy'
        and:
            workflow.active == true
    }

}