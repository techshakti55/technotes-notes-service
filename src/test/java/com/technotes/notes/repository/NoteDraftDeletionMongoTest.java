package com.technotes.notes.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.technotes.notes.document.NoteDocument;
import com.technotes.notes.enums.NoteStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// Dedicated disposable MongoDB; never connects to MONGODB_URI/production.
@Testcontainers(disabledWithoutDocker = true)
class NoteDraftDeletionMongoTest {

    @Container
    static final MongoDBContainer MONGODB =
            new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    private MongoClient client;
    private MongoTemplate mongo;
    private NoteDraftDeletion deletion;
    private String id;

    @BeforeEach
    void setup() {
        client = MongoClients.create(MONGODB.getReplicaSetUrl());
        mongo = new MongoTemplate(client, "draft_delete_test_" + UUID.randomUUID().toString().replace("-", ""));
        deletion = new NoteDraftDeletionImpl(mongo);
        NoteDocument note = new NoteDocument();
        note.setAuthorId("owner");
        note.setStatus(NoteStatus.DRAFT);
        note.setVersion(3L);
        id = mongo.insert(note).getId();
        mongo.updateFirst(Query.query(Criteria.where("_id").is(id)),
                new Update().set("version", 3L), NoteDocument.class);
    }

    @AfterEach
    void cleanup() {
        if (mongo != null) mongo.getDb().drop();
        if (client != null) client.close();
    }

    @Test
    void onlyExactOwnerDraftAndVersionCanBeRemoved() {
        assertFalse(deletion.deleteOwnedDraftAtVersion(id, "other", 3L));
        assertFalse(deletion.deleteOwnedDraftAtVersion(id, "owner", 2L));
        assertNotNull(mongo.findById(id, NoteDocument.class));
        assertTrue(deletion.deleteOwnedDraftAtVersion(id, "owner", 3L));
        assertNull(mongo.findById(id, NoteDocument.class));
        assertFalse(deletion.deleteOwnedDraftAtVersion(id, "owner", 3L));
    }

    @Test
    void deletionDoesNotCascadeToCategoriesOrRevisionSnapshots() {
        mongo.getCollection("categories").insertOne(new org.bson.Document("_id", "category"));
        mongo.getCollection("note_revisions").insertOne(
                new org.bson.Document("noteId", id).append("revisionNumber", 1L));
        assertTrue(deletion.deleteOwnedDraftAtVersion(id, "owner", 3L));
        assertEquals(1, mongo.getCollection("categories").countDocuments());
        assertEquals(1, mongo.getCollection("note_revisions").countDocuments());
    }

    @Test
    void interveningEditOrSubmissionIsNeverDeleted() {
        Query query = Query.query(Criteria.where("_id").is(id));
        mongo.updateFirst(query, new Update().set("version", 4L), NoteDocument.class);
        assertFalse(deletion.deleteOwnedDraftAtVersion(id, "owner", 3L));
        mongo.updateFirst(query, new Update().set("status", NoteStatus.IN_REVIEW), NoteDocument.class);
        assertFalse(deletion.deleteOwnedDraftAtVersion(id, "owner", 4L));
        assertNotNull(mongo.findById(id, NoteDocument.class));
    }

    @Test
    void racingSubmitAndDeleteCannotBothWin() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch start = new CountDownLatch(1);
            var deleted = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                return deletion.deleteOwnedDraftAtVersion(id, "owner", 3L);
            });
            var submitted = executor.submit(() -> {
                assertTrue(start.await(5, TimeUnit.SECONDS));
                Query query = Query.query(Criteria.where("_id").is(id)
                        .and("version").is(3L).and("status").is(NoteStatus.DRAFT));
                return mongo.updateFirst(query, new Update().set("status", NoteStatus.IN_REVIEW)
                        .set("version", 4L), NoteDocument.class).getModifiedCount() == 1;
            });
            start.countDown();
            assertNotEquals(deleted.get(10, TimeUnit.SECONDS), submitted.get(10, TimeUnit.SECONDS));
            NoteDocument remaining = mongo.findById(id, NoteDocument.class);
            if (remaining != null) assertEquals(NoteStatus.IN_REVIEW, remaining.getStatus());
        }
    }
}
