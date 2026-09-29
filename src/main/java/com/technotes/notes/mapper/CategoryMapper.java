package com.technotes.notes.mapper;

import com.technotes.notes.document.CategoryDocument;
import com.technotes.notes.dto.category.CategoryResponse;

public final class CategoryMapper {

    private CategoryMapper() {
    }

    public static CategoryResponse toResponse(CategoryDocument document) {

        CategoryResponse response = new CategoryResponse();

        response.setId(document.getId());
        response.setName(document.getName());
        response.setSlug(document.getSlug());
        response.setParentId(document.getParentId());
        response.setAncestorIds(document.getAncestorIds());
        response.setLevel(document.getLevel());
        response.setActive(document.isActive());
        response.setSortOrder(document.getSortOrder());
        response.setVersion(document.getVersion());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());

        return response;
    }
}