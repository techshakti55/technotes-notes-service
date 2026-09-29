package com.technotes.notes.service;

import com.technotes.notes.document.CategoryDocument;
import com.technotes.notes.dto.category.CategoryResponse;
import com.technotes.notes.dto.category.CreateCategoryRequest;
import com.technotes.notes.exception.InvalidStateException;
import com.technotes.notes.mapper.CategoryMapper;
import com.technotes.notes.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import com.technotes.notes.exception.DuplicateResourceException;
import com.technotes.notes.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponse createCategory(CreateCategoryRequest request) {

        CategoryDocument parent = resolveParent(request.getParentId());

        validateDuplicateSlug(
                request.getParentId(),
                request.getSlug()
        );

        CategoryDocument category = new CategoryDocument();

        category.setId(UUID.randomUUID().toString());
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setParentId(
                parent == null ? null : parent.getId()
        );

        category.setAncestorIds(
                buildAncestorIds(parent)
        );

        category.setLevel(
                calculateLevel(parent)
        );

        category.setActive(true);
        category.setSortOrder(request.getSortOrder());

        Instant now = Instant.now();

        category.setCreatedAt(now);
        category.setUpdatedAt(now);

        CategoryDocument savedCategory =
                categoryRepository.save(category);

        return CategoryMapper.toResponse(savedCategory);
    }

    private CategoryDocument resolveParent(String parentId) {

        if (parentId == null || parentId.isBlank()) {
            return null;
        }

        CategoryDocument parent = categoryRepository
                .findById(parentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Parent category not found: " + parentId
                        )
                );

        if (!parent.isActive()) {
            throw new InvalidStateException(
                    "Parent category is not active: " + parentId
            );
        }

        return parent;
    }

    private void validateDuplicateSlug(
            String parentId,
            String slug) {

        categoryRepository
                .findByParentIdAndSlug(parentId, slug)
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Category slug already exists under this parent: "
                                    + slug
                    );
                });
    }

    private List<String> buildAncestorIds(
            CategoryDocument parent) {

        List<String> ancestorIds = new ArrayList<>();

        if (parent == null) {
            return ancestorIds;
        }

        if (parent.getAncestorIds() != null) {
            ancestorIds.addAll(parent.getAncestorIds());
        }

        ancestorIds.add(parent.getId());

        return ancestorIds;
    }

    private int calculateLevel(CategoryDocument parent) {

        if (parent == null) {
            return 0;
        }

        return parent.getLevel() + 1;
    }
}