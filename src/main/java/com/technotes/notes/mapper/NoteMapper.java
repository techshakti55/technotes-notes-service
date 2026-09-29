package com.technotes.notes.mapper;

import com.technotes.notes.document.NoteDocument;
import com.technotes.notes.document.NoteRevisionDocument;
import com.technotes.notes.dto.note.NoteListItemResponse;
import com.technotes.notes.dto.note.NoteResponse;
import com.technotes.notes.dto.note.PublicNoteCardResponse;
import com.technotes.notes.dto.note.PublicNoteDetailResponse;

public final class NoteMapper {

    private NoteMapper() {
    }

    public static NoteResponse toResponse(NoteDocument document) {

        NoteResponse response = new NoteResponse();

        response.setId(document.getId());
        response.setTitle(document.getTitle());
        response.setSlug(document.getSlug());
        response.setSummary(document.getSummary());
        response.setContentMarkdown(document.getContentMarkdown());
        response.setPrimaryCategoryId(document.getPrimaryCategoryId());
        response.setTags(document.getTags());
        response.setContentKind(document.getContentKind());
        response.setAuthorId(document.getAuthorId());
        response.setStatus(document.getStatus());
        response.setVisibility(document.getVisibility());
        response.setVersion(document.getVersion());
        response.setCreatedAt(document.getCreatedAt());
        response.setUpdatedAt(document.getUpdatedAt());

        return response;
    }

    public static NoteListItemResponse toListItemResponse(
            NoteDocument document) {

        NoteListItemResponse response = new NoteListItemResponse();

        response.setId(document.getId());
        response.setTitle(document.getTitle());
        response.setSlug(document.getSlug());
        response.setSummary(document.getSummary());
        response.setPrimaryCategoryId(document.getPrimaryCategoryId());
        response.setTags(document.getTags());
        response.setStatus(document.getStatus());
        response.setVisibility(document.getVisibility());
        response.setVersion(document.getVersion());
        response.setUpdatedAt(document.getUpdatedAt());

        return response;
    }

    public static PublicNoteCardResponse toPublicCardResponse(
            NoteRevisionDocument revision) {

        PublicNoteCardResponse response = new PublicNoteCardResponse();

        response.setId(revision.getNoteId());
        response.setSlug(revision.getSlug());
        response.setTitle(revision.getTitle());
        response.setSummary(revision.getSummary());
        response.setPrimaryCategoryId(revision.getPrimaryCategoryId());
        response.setCategoryName(revision.getCategoryName());
        response.setTags(revision.getTags());
        response.setPublishedAt(revision.getPublishedAt());

        return response;
    }

    public static PublicNoteDetailResponse toPublicDetailResponse(
            NoteRevisionDocument revision) {

        PublicNoteDetailResponse response =
                new PublicNoteDetailResponse();

        response.setId(revision.getNoteId());
        response.setSlug(revision.getSlug());
        response.setTitle(revision.getTitle());
        response.setSummary(revision.getSummary());
        response.setContentMarkdown(revision.getContentMarkdown());
        response.setPrimaryCategoryId(revision.getPrimaryCategoryId());
        response.setCategoryName(revision.getCategoryName());
        response.setTags(revision.getTags());
        response.setRevisionNumber(revision.getRevisionNumber());
        response.setPublishedAt(revision.getPublishedAt());

        return response;
    }
}