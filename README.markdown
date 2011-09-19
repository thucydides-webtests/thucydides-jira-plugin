# Thucydides plugin for JIRA

This plugin updates JIRA issues referenced in Thucydides stories with links to the corresponding story reports.

To do this, it needs the following parameters:
    - *thucydides.issue.tracker.url*: The URL of the JIRA instance
    - *jira.username* and *jira.password*: A JIRA username and password that can be used to connect to JIRA and to consult and update comments in the
    project issues.
    - *jira.url*: The base URL of the published Thucydides stories reports.

You provide the base URL for the published Thucydides report in the system property *thucydides.reports.url*.

If you do not want Thucydides to update the JIRA issues, you can also set *thucydides.skip.jira.updates* to true, e.g.

    $mvn verify -Dthucydides.skip.jira.updates=true

This will simply write the relevant issue numbers to the log rather than trying to connect to JIRA.