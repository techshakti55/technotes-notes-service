package com.technotes.notes.repository;

import com.technotes.notes.document.NoteDocument;
import com.technotes.notes.enums.NoteStatus;
import com.technotes.notes.enums.Visibility;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NoteRepository extends MongoRepository<NoteDocument, String> {

    Optional<NoteDocument> findByIdAndAuthorId(
            String id,
            String authorId
    );

    Optional<NoteDocument> findBySlugAndStatusAndVisibility(
            String slug,
            NoteStatus status,
            Visibility visibility
    );
}