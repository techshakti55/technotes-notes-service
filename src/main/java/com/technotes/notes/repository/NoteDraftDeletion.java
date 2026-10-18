package com.technotes.notes.repository;

public interface NoteDraftDeletion {

    boolean deleteOwnedDraftAtVersion(String noteId, String authorId, Long version);
}
