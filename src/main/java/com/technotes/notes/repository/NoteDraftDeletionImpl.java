package com.technotes.notes.repository;

import com.technotes.notes.document.NoteDocument;
import com.technotes.notes.enums.NoteStatus;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

public class NoteDraftDeletionImpl implements NoteDraftDeletion {

    private final MongoTemplate mongoTemplate;

    public NoteDraftDeletionImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean deleteOwnedDraftAtVersion(
            String noteId, String authorId, Long version) {

        Query query = Query.query(Criteria.where("_id").is(noteId)
                .and("authorId").is(authorId)
                .and("status").is(NoteStatus.DRAFT)
                .and("version").is(version));

        // MongoDB evaluates the predicates and removal as one atomic write.
        // No cascade: categories and published revision snapshots are retained.
        return mongoTemplate.remove(query, NoteDocument.class)
                .getDeletedCount() == 1;
    }
}
