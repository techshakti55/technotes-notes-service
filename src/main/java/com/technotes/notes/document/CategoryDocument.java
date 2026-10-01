package com.technotes.notes.document;


import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "categories")
@CompoundIndex(
        name = "uk_category_parent_slug",
        def = "{'parentId': 1, 'slug': 1}",
        unique = true
)
public class CategoryDocument {

   @Id
    private String id;

    private String name;


    private String slug;

    @Indexed
    private String parentId;

    private List<String> ancestorIds = new ArrayList<>();

    private int level;

    private boolean active;

    private int sortOrder;

    @Version
    private Long version;

    private Instant createdAt;

    private Instant updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public List<String> getAncestorIds() {
        return ancestorIds;
    }

    public void setAncestorIds(List<String> ancestorIds) {
        this.ancestorIds = ancestorIds;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
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