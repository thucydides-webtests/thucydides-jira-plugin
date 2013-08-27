package net.thucydides.plugins.jira.client

import net.thucydides.plugins.jira.domain.IssueSummary
import spock.lang.Specification

class WhenLoadingIssueKeysUsingTheJerseyClient extends Specification {

    def "should load issue keys with JQL filters"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net","bruce","batm0bile")
        when:
            List<IssueSummary> issues = jiraClient.findByJQL("project='DEMO'")
        then:
            issues.size() > 10
    }

    def "should load issue summary by key"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net","bruce","batm0bile")
        when:
            IssueSummary issues = jiraClient.findByKey("DEMO-8")
        then:
            issues.key == "DEMO-8"
    }

    InputStream streamed(String source) { new ByteArrayInputStream(source.bytes) }
}
