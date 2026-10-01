package com.technotes.notes.repository;

import com.technotes.notes.document.NoteDocument;
import com.technotes.notes.enums.NoteStatus;
import com.technotes.notes.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NoteRepository
        extends MongoRepository<NoteDocument, String> {

    Optional<NoteDocument> findByIdAndAuthorId(
            String id,
            String authorId
    );

    Page<NoteDocument> findByAuthorId(
            String authorId,
            Pageable pageable
    );

    Optional<NoteDocument> findBySlugAndStatusAndVisibility(
            String slug,
            NoteStatus status,
            Visibility visibility
    );

    boolean existsBySlug(String slug);

    Page<NoteDocument> findByStatusAndVisibility(
            NoteStatus status,
            Visibility visibility,
            Pageable pageable
    );

    Page<NoteDocument> findByStatusAndVisibilityAndPrimaryCategoryId(
            NoteStatus status,
            Visibility visibility,
            String primaryCategoryId,
            Pageable pageable
    );
}