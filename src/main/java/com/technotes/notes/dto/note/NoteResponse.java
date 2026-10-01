package com.technotes.notes.dto.note;

import com.technotes.notes.enums.ContentKind;
import com.technotes.notes.enums.NoteStatus;
import com.technotes.notes.enums.Visibility;

import java.time.Instant;
import java.util.List;

public class NoteResponse {

    private String id;
    private String title;
    private String slug;
    private String summary;
    private String contentMarkdown;
    private String primaryCategoryId;
    private List<String> tags;
    private ContentKind contentKind;
    private String authorId;
    private NoteStatus status;
    private Visibility visibility;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }

    public String getPrimaryCategoryId() {
        return primaryCategoryId;
    }

    public void setPrimaryCategoryId(String primaryCategoryId) {
        this.primaryCategoryId = primaryCategoryId;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public ContentKind getContentKind() {
        return contentKind;
    }

    public void setContentKind(ContentKind contentKind) {
        this.contentKind = contentKind;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public NoteStatus getStatus() {
        return status;
    }

    public void setStatus(NoteStatus status) {
        this.status = status;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
