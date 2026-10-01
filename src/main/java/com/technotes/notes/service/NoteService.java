package com.technotes.notes.service;
import com.technotes.notes.document.NoteRevisionDocument;
import com.technotes.notes.repository.NoteRevisionRepository;
import org.springframework.transaction.annotation.Transactional;
import com.technotes.notes.document.CategoryDocument;
import com.technotes.notes.document.NoteDocument;
import com.technotes.notes.dto.common.PageResponse;
import com.technotes.notes.dto.note.CreateNoteRequest;
import com.technotes.notes.dto.note.NoteListItemResponse;
import com.technotes.notes.dto.note.NoteResponse;
import com.technotes.notes.dto.note.UpdateNoteRequest;
import com.technotes.notes.enums.ContentKind;
import com.technotes.notes.enums.NoteStatus;
import com.technotes.notes.exception.InvalidStateException;
import com.technotes.notes.exception.MissingIfMatchException;
import com.technotes.notes.exception.ResourceNotFoundException;
import com.technotes.notes.exception.StaleVersionException;
import com.technotes.notes.mapper.NoteMapper;
import com.technotes.notes.repository.CategoryRepository;
import com.technotes.notes.repository.NoteRepository;
import com.technotes.notes.security.CurrentUserProvider;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class NoteService {

    private static final Pattern NON_SLUG_CHARACTERS =
            Pattern.compile("[^a-z0-9]+");

    private static final Pattern LEADING_TRAILING_HYPHENS =
            Pattern.compile("(^-+|-+$)");

    private final NoteRepository noteRepository;
    private final CategoryRepository categoryRepository;
    private final CurrentUserProvider currentUserProvider;
    private final NoteRevisionRepository noteRevisionRepository;

    public NoteService(
            NoteRepository noteRepository,
            CategoryRepository categoryRepository,
            NoteRevisionRepository noteRevisionRepository,
            CurrentUserProvider currentUserProvider) {

        this.noteRepository = noteRepository;
        this.categoryRepository = categoryRepository;
        this.noteRevisionRepository = noteRevisionRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public NoteResponse createNote(CreateNoteRequest request) {

        String authorId = currentUserProvider.getCurrentUserId();

        validateActiveCategory(request.getPrimaryCategoryId());

        Instant now = Instant.now();

        NoteDocument document = new NoteDocument();

        document.setTitle(request.getTitle().trim());
        document.setSlug(generateUniqueSlug(request.getTitle()));
        document.setSummary(normalizeNullable(request.getSummary()));
        document.setContentMarkdown(request.getContentMarkdown());
        document.setPrimaryCategoryId(request.getPrimaryCategoryId());
        document.setTags(copyTags(request.getTags()));

        document.setContentKind(ContentKind.NOTE);
        document.setAuthorId(authorId);
        document.setStatus(NoteStatus.DRAFT);
        document.setVisibility(request.getVisibility());

        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        NoteDocument saved = noteRepository.save(document);

        return NoteMapper.toResponse(saved);
    }

    public PageResponse<NoteListItemResponse> getMyNotes(
            String view,
            int page,
            int size,
            String sort) {

        // First-live supports only owner-scoped notes.
        if (!"mine".equalsIgnoreCase(view)) {
            throw new IllegalArgumentException(
                    "Only view=mine is supported in the first live release."
            );
        }

        // Page numbering starts from 0.
        if (page < 0) {
            throw new IllegalArgumentException(
                    "page must be greater than or equal to 0."
            );
        }

        // Protect backend from oversized requests.
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "size must be between 1 and 100."
            );
        }

        // First-live exposes one stable sort contract.
        if (sort == null ||
                !"updatedAt,desc".equalsIgnoreCase(sort.trim())) {

            throw new IllegalArgumentException(
                    "Only sort=updatedAt,desc is supported in the first live release."
            );
        }

        String authorId =
                currentUserProvider.getCurrentUserId();

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "updatedAt"
                        )
                );

        Page<NoteDocument> result =
                noteRepository.findByAuthorId(
                        authorId,
                        pageable
                );

        List<NoteListItemResponse> items =
                result.getContent()
                        .stream()
                        .map(NoteMapper::toListItemResponse)
                        .toList();

        PageResponse<NoteListItemResponse> response =
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

    public NoteResponse getMyNote(String noteId) {

        NoteDocument document = getOwnedNote(noteId);

        return NoteMapper.toResponse(document);
    }

    public NoteResponse updateNote(
            String noteId,
            String ifMatch,
            UpdateNoteRequest request) {

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new MissingIfMatchException(
                    "If-Match header is required."
            );
        }

        NoteDocument document = getOwnedNote(noteId);

        validateIfMatch(document, ifMatch);

        ensureEditable(document);

        if (request.getTitle() != null) {

            String title = request.getTitle().trim();

            if (title.isBlank()) {
                throw new IllegalArgumentException(
                        "Title must not be blank."
                );
            }

            document.setTitle(title);
        }

        if (request.getSummary() != null) {
            document.setSummary(
                    normalizeNullable(request.getSummary())
            );
        }

        if (request.getContentMarkdown() != null) {
            document.setContentMarkdown(
                    request.getContentMarkdown()
            );
        }

        if (request.getPrimaryCategoryId() != null) {

            validateActiveCategory(
                    request.getPrimaryCategoryId()
            );

            document.setPrimaryCategoryId(
                    request.getPrimaryCategoryId()
            );
        }

        if (request.getTags() != null) {
            document.setTags(copyTags(request.getTags()));
        }

        if (request.getVisibility() != null) {
            document.setVisibility(request.getVisibility());
        }

        document.setUpdatedAt(Instant.now());

        try {
            NoteDocument saved =
                    noteRepository.save(document);

            return NoteMapper.toResponse(saved);

        } catch (OptimisticLockingFailureException ex) {

            throw new StaleVersionException(
                    "The note was modified by another request."
            );
        }
    }

    public NoteResponse submitNote(
            String noteId,
            String ifMatch) {

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new MissingIfMatchException(
                    "If-Match header is required."
            );
        }

        NoteDocument document = getOwnedNote(noteId);

        validateIfMatch(document, ifMatch);

        if (document.getStatus() != NoteStatus.DRAFT) {
            throw new InvalidStateException(
                    "Only a DRAFT note can be submitted for review."
            );
        }

        document.setStatus(NoteStatus.IN_REVIEW);
        document.setUpdatedAt(Instant.now());

        try {
            NoteDocument saved =
                    noteRepository.save(document);

            return NoteMapper.toResponse(saved);

        } catch (OptimisticLockingFailureException ex) {

            throw new StaleVersionException(
                    "The note was modified by another request."
            );
        }
    }

    public String buildEtag(NoteResponse response) {

        return buildEtag(
                response.getId(),
                response.getVersion()
        );
    }
    @Transactional
    public NoteResponse publishNote(
            String noteId,
            String ifMatch) {

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new MissingIfMatchException(
                    "If-Match header is required."
            );
        }

        NoteDocument document = getOwnedNote(noteId);

        validateIfMatch(document, ifMatch);

        if (document.getStatus() != NoteStatus.IN_REVIEW) {
            throw new InvalidStateException(
                    "Only an IN_REVIEW note can be published."
            );
        }

        CategoryDocument category =
                categoryRepository
                        .findById(document.getPrimaryCategoryId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found."
                                )
                        );

        if (!category.isActive()) {
            throw new InvalidStateException(
                    "Category is not active."
            );
        }

        String publisherId =
                currentUserProvider.getCurrentUserId();

        long nextRevisionNumber =
                noteRevisionRepository
                        .findTopByNoteIdOrderByRevisionNumberDesc(
                                document.getId()
                        )
                        .map(revision ->
                                revision.getRevisionNumber() + 1
                        )
                        .orElse(1L);

        Instant now = Instant.now();

        NoteRevisionDocument revision =
                new NoteRevisionDocument();

        revision.setNoteId(document.getId());
        revision.setRevisionNumber(nextRevisionNumber);

        revision.setSlug(document.getSlug());
        revision.setTitle(document.getTitle());
        revision.setSummary(document.getSummary());
        revision.setContentMarkdown(
                document.getContentMarkdown()
        );

        revision.setPrimaryCategoryId(
                document.getPrimaryCategoryId()
        );

        revision.setCategoryName(category.getName());

        revision.setTags(
                document.getTags() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(document.getTags())
        );

        revision.setAuthorId(document.getAuthorId());
        revision.setVisibility(document.getVisibility());

        revision.setPublishedAt(now);
        revision.setPublishedBy(publisherId);
        revision.setCreatedAt(now);

        noteRevisionRepository.save(revision);

        document.setStatus(NoteStatus.PUBLISHED);
        document.setPublishedRevisionId(revision.getId());
        document.setPublishedAt(now);
        document.setUpdatedAt(now);

        try {

            NoteDocument saved =
                    noteRepository.save(document);

            return NoteMapper.toResponse(saved);

        } catch (OptimisticLockingFailureException ex) {

            throw new StaleVersionException(
                    "The note was modified by another request."
            );
        }
    }

    private NoteDocument getOwnedNote(String noteId) {

        String authorId =
                currentUserProvider.getCurrentUserId();

        return noteRepository
                .findByIdAndAuthorId(noteId, authorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Note not found."
                        )
                );
    }

    private void validateActiveCategory(String categoryId) {

        CategoryDocument category =
                categoryRepository.findById(categoryId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found."
                                )
                        );

        if (!category.isActive()) {
            throw new InvalidStateException(
                    "Category is not active."
            );
        }
    }

    private void ensureEditable(NoteDocument document) {

        if (document.getStatus() != NoteStatus.DRAFT) {
            throw new InvalidStateException(
                    "Only a DRAFT note can be edited."
            );
        }
    }

    private void validateIfMatch(
            NoteDocument document,
            String ifMatch) {

        String expected =
                buildEtag(
                        document.getId(),
                        document.getVersion()
                );

        if (!expected.equals(ifMatch.trim())) {
            throw new StaleVersionException(
                    "The supplied ETag does not match the current note version."
            );
        }
    }

    private String buildEtag(
            String noteId,
            Long version) {

        return "\"note-"
                + noteId
                + "-v"
                + version
                + "\"";
    }

    private String generateUniqueSlug(String title) {

        String baseSlug = slugify(title);

        if (baseSlug.isBlank()) {
            baseSlug = "note";
        }

        String candidate = baseSlug;
        int suffix = 2;

        while (noteRepository.existsBySlug(candidate)) {
            candidate = baseSlug + "-" + suffix;
            suffix++;
        }

        return candidate;
    }

    private String slugify(String value) {

        String normalized =
                Normalizer.normalize(
                        value,
                        Normalizer.Form.NFD
                );

        normalized =
                normalized.replaceAll("\\p{M}", "");

        normalized =
                normalized.toLowerCase(Locale.ROOT);

        normalized =
                NON_SLUG_CHARACTERS
                        .matcher(normalized)
                        .replaceAll("-");

        return LEADING_TRAILING_HYPHENS
                .matcher(normalized)
                .replaceAll("");
    }

    private String normalizeNullable(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }

    private List<String> copyTags(List<String> tags) {

        if (tags == null) {
            return new ArrayList<>();
        }

        return tags.stream()
                .filter(tag -> tag != null)
                .map(String::trim)
                .filter(tag -> !tag.isBlank())
                .distinct()
                .toList();
    }


}