package com.technotes.notes.service;

import com.technotes.notes.document.NoteDocument;
import com.technotes.notes.document.NoteRevisionDocument;
import com.technotes.notes.dto.common.PageResponse;
import com.technotes.notes.dto.note.PublicNoteCardResponse;
import com.technotes.notes.dto.note.PublicNoteDetailResponse;
import com.technotes.notes.enums.NoteStatus;
import com.technotes.notes.enums.Visibility;
import com.technotes.notes.exception.ResourceNotFoundException;
import com.technotes.notes.mapper.NoteMapper;
import com.technotes.notes.repository.NoteRepository;
import com.technotes.notes.repository.NoteRevisionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicNoteService {

    private final NoteRepository noteRepository;
    private final NoteRevisionRepository noteRevisionRepository;

    public PublicNoteService(
            NoteRepository noteRepository,
            NoteRevisionRepository noteRevisionRepository) {

        this.noteRepository = noteRepository;
        this.noteRevisionRepository = noteRevisionRepository;
    }

    public PageResponse<PublicNoteCardResponse> getPublishedNotes(
            String primaryCategoryId,
            int page,
            int size,
            String sort) {

        validatePagination(page, size);
        validateSort(sort);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "publishedAt"
                        )
                );

        Page<NoteDocument> result;

        if (primaryCategoryId != null
                && !primaryCategoryId.isBlank()) {

            result =
                    noteRepository
                            .findByStatusAndVisibilityAndPrimaryCategoryId(
                                    NoteStatus.PUBLISHED,
                                    Visibility.PUBLIC,
                                    primaryCategoryId.trim(),
                                    pageable
                            );

        } else {

            result =
                    noteRepository.findByStatusAndVisibility(
                            NoteStatus.PUBLISHED,
                            Visibility.PUBLIC,
                            pageable
                    );
        }

        List<PublicNoteCardResponse> items =
                result.getContent()
                        .stream()
                        .map(this::getPublishedRevision)
                        .map(NoteMapper::toPublicCardResponse)
                        .toList();

        PageResponse<PublicNoteCardResponse> response =
                new PageResponse<>();

        response.setItems(items);
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(
                result.getTotalElements()
        );
        response.setTotalPages(
                result.getTotalPages()
        );

        return response;
    }

    public PublicNoteDetailResponse getPublishedNote(
            String slug) {

        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException(
                    "slug must not be blank."
            );
        }

        NoteDocument note =
                noteRepository
                        .findBySlugAndStatusAndVisibility(
                                slug.trim(),
                                NoteStatus.PUBLISHED,
                                Visibility.PUBLIC
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Published note not found."
                                )
                        );

        NoteRevisionDocument revision =
                getPublishedRevision(note);

        return NoteMapper.toPublicDetailResponse(
                revision
        );
    }

    private NoteRevisionDocument getPublishedRevision(
            NoteDocument note) {

        String revisionId =
                note.getPublishedRevisionId();

        if (revisionId == null
                || revisionId.isBlank()) {

            throw new ResourceNotFoundException(
                    "Published note snapshot not found."
            );
        }

        return noteRevisionRepository
                .findById(revisionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Published note snapshot not found."
                        )
                );
    }

    private void validatePagination(
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be greater than or equal to 0."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100."
            );
        }
    }

    private void validateSort(String sort) {

        if (sort == null ||
                !"publishedAt,desc"
                        .equalsIgnoreCase(sort.trim())) {

            throw new IllegalArgumentException(
                    "Only sort=publishedAt,desc is supported."
            );
        }
    }
}