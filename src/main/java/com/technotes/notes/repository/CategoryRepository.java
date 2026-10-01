package com.technotes.notes.repository;

import com.technotes.notes.document.CategoryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CategoryRepository
        extends MongoRepository<CategoryDocument, String> {

    Optional<CategoryDocument> findByParentIdAndSlug(
            String parentId,
            String slug
    );

    Page<CategoryDocument> findByActiveTrue(
            Pageable pageable
    );

    Page<CategoryDocument> findByActiveTrueAndParentIdIsNull(
            Pageable pageable
    );

    Page<CategoryDocument> findByActiveTrueAndParentId(
            String parentId,
            Pageable pageable
    );
}