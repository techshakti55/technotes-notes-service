package com.technotes.notes.repository;

import com.technotes.notes.document.NoteRevisionDocument;
import com.technotes.notes.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NoteRevisionRepository
        extends MongoRepository<NoteRevisionDocument, String> {

    Optional<NoteRevisionDocument>
    findTopByNoteIdOrderByRevisionNumberDesc(String noteId);

    Optional<NoteRevisionDocument>
    findTopBySlugAndVisibilityOrderByRevisionNumberDesc(
            String slug,
            Visibility visibility
    );

    Page<NoteRevisionDocument>
    findByVisibility(
            Visibility visibility,
            Pageable pageable
    );
}