# Thucydides plugin for JIRA

This plugin updates JIRA issues referenced in Thucydides stories with links to the corresponding story reports.

To do this, it needs the following parameters:
    - The URL of the JIRA instance
    - A JIRA username and password that can be used to connect to JIRA and to consult and update comments in the
    project issues.
    - The base URL of the published Thucydides stories reports.

You provide the base URL for the published Thucydides report in the system property *thucydides.reports.url*.