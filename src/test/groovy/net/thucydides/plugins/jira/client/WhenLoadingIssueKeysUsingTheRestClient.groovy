package net.thucydides.plugins.jira.client

import com.atlassian.jira.rest.client.domain.Issue
import spock.lang.Specification

class WhenLoadingIssueKeysUsingTheRestClient extends Specification {

    def "should load issues with JQL filters"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net","bruce","batm0bile")
        when:
            List<String> issueKeys = jiraClient.findByJQL("project = 'DEMO' || project != 'DEMO'")
        then:
            issueKeys.size() > 10
    }

    def "should load issue keys with JQL filters in batches"() {
        given:
        def jiraClient = new AtlassianJiraClient("https://wakaleo.atlassian.net","bruce","batm0bile", 3)
        when:
        List<String> issueKeys = jiraClient.findByJQL("project = DEMO")
        then:
        issueKeys.size() > 3
    }

    def "should load issues with JQL filters 2"() {
        given:
        def jiraClient = new AtlassianJiraClient("https://wakaleo.atlassian.net","bruce","batm0bile")
        when:
        List<Issue> issues = jiraClient.findIssuesByJQL("project = 'DEMO'")
        then:
        !issues.isEmpty()
    }

    def "should load outbound linked issues"() {
        given:
        def jiraClient = new AtlassianJiraClient("https://wakaleo.atlassian.net","bruce","batm0bile")
        Issue issue = jiraClient.findIssueByKey("DEMO-8")
        when:
        def linkedIssues = jiraClient.findOutboundIssuesWithLink(issue,"Relates");
        then:
        linkedIssues[0].getKey() == "DEMO-7"
    }


    def "should load inbound linked issues"() {
        given:
        def jiraClient = new AtlassianJiraClient("https://wakaleo.atlassian.net","bruce","batm0bile")
        Issue issue = jiraClient.findIssueByKey("DEMO-7")
        when:
        def linkedIssues = jiraClient.findInboundIssuesWithLink(issue,"Relates");
        then:
        linkedIssues[0].getKey() == "DEMO-8"
    }

    InputStream streamed(String source) { new ByteArrayInputStream(source.bytes) }
}
