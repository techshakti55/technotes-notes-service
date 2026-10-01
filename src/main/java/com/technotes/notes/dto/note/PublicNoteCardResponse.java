package com.technotes.notes.dto.note;

import java.time.Instant;
import java.util.List;

public class PublicNoteCardResponse {

    private String id;
    private String slug;
    private String title;
    private String summary;
    private String primaryCategoryId;
    private String categoryName;
    private List<String> tags;
    private Instant publishedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getPrimaryCategoryId() {
        return primaryCategoryId;
    }

    public void setPrimaryCategoryId(String primaryCategoryId) {
        this.primaryCategoryId = primaryCategoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}