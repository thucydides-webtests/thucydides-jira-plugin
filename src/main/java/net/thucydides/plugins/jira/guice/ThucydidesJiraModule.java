package net.thucydides.plugins.jira.guice;

import com.google.inject.AbstractModule;
import net.thucydides.core.pages.InternalSystemClock;
import net.thucydides.core.pages.SystemClock;
import net.thucydides.core.reports.json.ColorScheme;
import net.thucydides.core.reports.json.RelativeSizeColorScheme;
import net.thucydides.core.reports.templates.FreeMarkerTemplateManager;
import net.thucydides.core.reports.templates.TemplateManager;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.SystemEnvironmentVariables;
import net.thucydides.plugins.jira.model.IssueTracker;
import net.thucydides.plugins.jira.service.JIRAConfiguration;
import net.thucydides.plugins.jira.service.JiraIssueTracker;
import net.thucydides.plugins.jira.service.SystemPropertiesJIRAConfiguration;

public class ThucydidesJiraModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(JIRAConfiguration.class).to(SystemPropertiesJIRAConfiguration.class);
        bind(IssueTracker.class).to(JiraIssueTracker.class);
    }
}
