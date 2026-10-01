package com.technotes.notes.document;

import com.technotes.notes.enums.ContentKind;
import com.technotes.notes.enums.NoteStatus;
import com.technotes.notes.enums.Visibility;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "notes")
public class NoteDocument {

    @Id
    private String id;

    private String title;

    @Indexed(unique = true)
    private String slug;

    private String summary;

    private String contentMarkdown;

    @Indexed
    private String primaryCategoryId;

    private List<String> tags = new ArrayList<>();

    private ContentKind contentKind;

    @Indexed
    private String authorId;

    @Indexed
    private NoteStatus status;
    private Instant publishedAt;

    @Indexed
    private Visibility visibility;

    @Version
    private Long version;

    private String publishedRevisionId;

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
    public String getPublishedRevisionId() {
        return publishedRevisionId;
    }

    public void setPublishedRevisionId(String publishedRevisionId) {
        this.publishedRevisionId = publishedRevisionId;
    }
    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}