package net.thucydides.plugins.jira.domain;

import java.net.URI;

public class IssueSummary {

    private final URI self;
    private final String key;
    private final String summary;
    private final String description;
    private final String type;

    public IssueSummary(URI self, String key, String summary, String description, String type) {
        this.self = self;
        this.key = key;
        this.summary = summary;
        this.description = description;
        this.type = type;
    }

    public URI getSelf() {
        return self;
    }

    public String getKey() {
        return key;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return "IssueSummary{" +
                "key='" + key + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }
}
