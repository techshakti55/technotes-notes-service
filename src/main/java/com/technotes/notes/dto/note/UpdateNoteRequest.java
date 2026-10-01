package com.technotes.notes.dto.note;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.technotes.notes.enums.Visibility;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class UpdateNoteRequest {

    private static final int MAX_MARKDOWN_BYTES = 1024 * 1024;

    @Size(min = 1, max = 200)
    private String title;

    @Size(max = 500)
    private String summary;

    private String contentMarkdown;

    private String primaryCategoryId;

    @Size(max = 10, message = "must contain at most 10 tags")
    private List<
            @NotBlank
            @Size(max = 40)
            @Pattern(
                    regexp = "[a-z0-9]+(?:-[a-z0-9]+)*",
                    message = "must be a lowercase slug, such as spring-boot"
            )
                    String> tags;

    private Visibility visibility;

    @JsonIgnore
    @AssertTrue(message = "title must not be blank when supplied")
    public boolean isTitleValid() {
        return title == null || !title.isBlank();
    }

    @JsonIgnore
    @AssertTrue(message = "contentMarkdown must not be blank when supplied")
    public boolean isMarkdownNotBlank() {
        return contentMarkdown == null || !contentMarkdown.isBlank();
    }

    @JsonIgnore
    @AssertTrue(message = "contentMarkdown must not exceed 1 MiB UTF-8")
    public boolean isMarkdownSizeValid() {
        return contentMarkdown == null
                || contentMarkdown.getBytes(StandardCharsets.UTF_8).length
                <= MAX_MARKDOWN_BYTES;
    }

    @JsonIgnore
    @AssertTrue(message = "primaryCategoryId must not be blank when supplied")
    public boolean isCategoryValid() {
        return primaryCategoryId == null || !primaryCategoryId.isBlank();
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

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
}