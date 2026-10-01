package com.technotes.notes.controller;

import com.technotes.notes.dto.category.CategoryResponse;
import com.technotes.notes.dto.category.CreateCategoryRequest;
import com.technotes.notes.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    @PreAuthorize(
            "hasRole('ADMIN') and hasAuthority('SCOPE_taxonomy.write')"
    )
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {

        CategoryResponse response =
                categoryService.createCategory(request);

        URI location = URI.create(
                "/api/v1/categories/" + response.getId()
        );

        String etag = buildEtag(response);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(location)
                .header(HttpHeaders.ETAG, etag)
                .body(response);
    }

    private String buildEtag(CategoryResponse response) {

        return "\"category-"
                + response.getId()
                + "-v"
                + response.getVersion()
                + "\"";
    }
}