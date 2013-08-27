package net.thucydides.plugins.jira.domain;

import com.google.common.collect.ImmutableList;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class IssueSummary {

    private final URI self;
    private final String key;
    private final String summary;
    private final String description;
    private final String type;
    private final List<String> labels;

    public IssueSummary(URI self, String key, String summary, String description, String type) {
        this(self, key, summary, description, type, new ArrayList<String>());
    }

    public IssueSummary(URI self, String key, String summary, String description, String type, List<String> labels) {
        this.self = self;
        this.key = key;
        this.summary = summary;
        this.description = description;
        this.type = type;
        this.labels = ImmutableList.copyOf(labels);
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

    public List<String> getLabels() {
        return labels;
    }

    @Override
    public String toString() {
        return "IssueSummary{" +
                "key='" + key + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }
}
