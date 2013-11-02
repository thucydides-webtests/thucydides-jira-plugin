package net.thucydides.plugins.jira.domain;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueSummary {

    private final URI self;
    private final Long id;
    private final String key;
    private final String summary;
    private final String description;
    private final String type;
    private final List<String> labels;
    private final String renderedDescription;
    private final List<String> fixVersions;
    private final Map<String, Object> customFieldValues;

    public IssueSummary(URI self, Long id, String key, String summary, String description, String renderedDescription, String type) {
        this(self, id, key, summary, description, renderedDescription, type,
                new ArrayList<String>(), new ArrayList<String>(), new HashMap<String, Object>());
    }

    public IssueSummary(URI self, Long id, String key, String summary, String description, String renderedDescription,
                        String type, List<String> labels, List<String> fixVersions, Map<String, Object> customFields) {
        this.self = self;
        this.id = id;
        this.key = key;
        this.summary = summary;
        this.description = description;
        this.renderedDescription = renderedDescription;
        this.type = type;
        this.labels = ImmutableList.copyOf(labels);
        this.fixVersions = ImmutableList.copyOf(fixVersions);
        this.customFieldValues = ImmutableMap.copyOf(customFields);
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

    public String getRenderedDescription() {
        return renderedDescription;
    }

    public String getType() {
        return type;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<String> getFixVersions() {
        return fixVersions;
    }

    @Override
    public String toString() {
        return "IssueSummary{" +
                "key='" + key + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }

    public Optional<CustomFieldCast> customField(String fieldName) {
        if (customFieldValues.get(fieldName) == null) {
            return Optional.absent();
        } else {
            return Optional.of(new CustomFieldCast(customFieldValues.get(fieldName)));
        }
    }

}
