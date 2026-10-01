package com.technotes.notes.controller;

import com.technotes.notes.document.CategoryDocument;
import com.technotes.notes.dto.category.CategoryResponse;
import com.technotes.notes.dto.common.PageResponse;
import com.technotes.notes.mapper.CategoryMapper;
import com.technotes.notes.repository.CategoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/categories")
public class PublicCategoryController {

    private final CategoryRepository categoryRepository;

    public PublicCategoryController(
            CategoryRepository categoryRepository) {

        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public ResponseEntity<PageResponse<CategoryResponse>> getCategories(
            @RequestParam(defaultValue = "false") boolean rootOnly,
            @RequestParam(required = false) String parentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        validatePagination(page, size);

        if (rootOnly && parentId != null && !parentId.isBlank()) {
            throw new IllegalArgumentException(
                    "rootOnly=true and parentId cannot be used together."
            );
        }

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("sortOrder"),
                        Sort.Order.asc("name")
                )
        );

        Page<CategoryDocument> result;

        if (rootOnly) {

            result =
                    categoryRepository
                            .findByActiveTrueAndParentIdIsNull(
                                    pageable
                            );

        } else if (parentId != null && !parentId.isBlank()) {

            result =
                    categoryRepository
                            .findByActiveTrueAndParentId(
                                    parentId.trim(),
                                    pageable
                            );

        } else {

            result =
                    categoryRepository.findByActiveTrue(
                            pageable
                    );
        }

        List<CategoryResponse> items =
                result.getContent()
                        .stream()
                        .map(CategoryMapper::toResponse)
                        .toList();

        PageResponse<CategoryResponse> response =
                new PageResponse<>();

        response.setItems(items);
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(
                result.getTotalElements()
        );
        response.setTotalPages(
                result.getTotalPages()
        );

        return ResponseEntity.ok(response);
    }

    private void validatePagination(
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be greater than or equal to 0."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100."
            );
        }
    }
}