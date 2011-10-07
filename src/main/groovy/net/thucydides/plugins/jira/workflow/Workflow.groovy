package net.thucydides.plugins.jira.workflow

import org.codehaus.groovy.control.CompilerConfiguration
import net.thucydides.core.model.TestResult
import net.thucydides.plugins.jira.workflow.TransitionBuilder.TransitionSetMap

/**
 * Manage JIRA workflow integration.
 * JIRA workflow integration is configured using a simple Groovy DSL to define the transitionSetMap to be performed
 * for each test result.
 */
class Workflow {

    def builder = new TransitionBuilder()

    static Workflow loadFrom(final InputStream configFile) {
        return new Workflow(configFile.text)
    }

    protected Workflow(String configuration) {

        Script s = new GroovyClassLoader().parseClass(configuration).newInstance()
        s.binding = new BuilderBinding(builder:builder)
        s.run()
    }

    public TransitionSetMap getTransactions() {
        builder.getTransitionSetMap()
    }
}

class BuilderBinding extends Binding {
    def builder
    Object getVariable(String name) {
        return { Object... args ->  builder.invokeMethod(name,args) }
    }
}