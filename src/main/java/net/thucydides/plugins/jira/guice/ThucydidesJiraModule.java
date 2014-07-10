package net.thucydides.plugins.jira.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.SystemEnvironmentVariables;
import net.thucydides.core.webdriver.Configuration;
import net.thucydides.core.webdriver.SystemPropertiesConfiguration;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.service.JIRAConfiguration;
import net.thucydides.plugins.jira.service.JiraIssueTracker;
import net.thucydides.plugins.jira.service.SystemPropertiesJIRAConfiguration;
import net.thucydides.plugins.jira.workflow.ClasspathWorkflowLoader;
import net.thucydides.plugins.jira.workflow.WorkflowLoader;

public class ThucydidesJiraModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Configuration.class).to(SystemPropertiesConfiguration.class);
        bind(IssueTracker.class).to(JiraIssueTracker.class);
        bind(JIRAConfiguration.class).to(SystemPropertiesJIRAConfiguration.class);
        bind(WorkflowLoader.class).to(ClasspathWorkflowLoader.class);

        bindConstant().annotatedWith(Names.named("defaultWorkflow")).to("jira-workflow.groovy");
    }

    @Provides
    public EnvironmentVariables provideEnvironmentVariables() {
        return SystemEnvironmentVariables.createEnvironmentVariables();
    }
}
