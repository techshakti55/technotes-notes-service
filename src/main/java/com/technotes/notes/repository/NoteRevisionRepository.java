package com.technotes.notes.repository;

import com.technotes.notes.document.NoteRevisionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NoteRevisionRepository
        extends MongoRepository<NoteRevisionDocument, String> {

    Optional<NoteRevisionDocument>
    findTopByNoteIdOrderByRevisionNumberDesc(String noteId);
}