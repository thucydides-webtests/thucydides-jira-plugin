package net.thucydides.plugins.jira.domain;

import com.google.common.collect.ImmutableList;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class IssueSummary {

    private final URI self;
    private final Long id;
    private final String key;
    private final String summary;
    private final String description;
    private final String type;
    private final List<String> labels;

    public IssueSummary(URI self, Long id, String key, String summary, String description, String type) {
        this(self, id, key, summary, description, type, new ArrayList<String>());
    }

    public IssueSummary(URI self, Long id, String key, String summary, String description, String type, List<String> labels) {
        this.self = self;
        this.id = id;
        this.key = key;
        this.summary = summary;
        this.description = description;
        this.type = type;
        this.labels = ImmutableList.copyOf(labels);
    }

    public URI getSelf() {
        return self;
    }

    public Long getId() {
        return id;
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
