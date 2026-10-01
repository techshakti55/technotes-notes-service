package com.technotes.notes.controller;

import com.technotes.notes.dto.common.PageResponse;
import com.technotes.notes.dto.note.CreateNoteRequest;
import com.technotes.notes.dto.note.NoteListItemResponse;
import com.technotes.notes.dto.note.NoteResponse;
import com.technotes.notes.dto.note.UpdateNoteRequest;
import com.technotes.notes.service.NoteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/notes")
public class NoteController {

    private final NoteService noteService;

    public NoteController(NoteService noteService) {
        this.noteService = noteService;
    }

    // =========================================================
    // GET /api/v1/notes
    // Owner-scoped note list
    // First-live supports view=mine
    // =========================================================
    @GetMapping
    @PreAuthorize(
            "(hasRole('AUTHOR') or hasRole('ADMIN')) " +
                    "and hasAuthority('SCOPE_notes.read')"
    )
    public ResponseEntity<PageResponse<NoteListItemResponse>> getMyNotes(
            @RequestParam(defaultValue = "mine") String view,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {

        PageResponse<NoteListItemResponse> response =
                noteService.getMyNotes(
                        view,
                        page,
                        size,
                        sort
                );

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // GET /api/v1/notes/{id}
    // Owner-scoped editable detail
    // =========================================================
    @GetMapping("/{id}")
    @PreAuthorize(
            "(hasRole('AUTHOR') or hasRole('ADMIN')) " +
                    "and hasAuthority('SCOPE_notes.read')"
    )
    public ResponseEntity<NoteResponse> getMyNote(
            @PathVariable String id) {

        NoteResponse response =
                noteService.getMyNote(id);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.ETAG,
                        noteService.buildEtag(response)
                )
                .body(response);
    }

    // =========================================================
    // POST /api/v1/notes
    // Creates DRAFT note
    // authorId comes from JWT sub, never from request
    // =========================================================
    @PostMapping
    @PreAuthorize(
            "(hasRole('AUTHOR') or hasRole('ADMIN')) " +
                    "and hasAuthority('SCOPE_notes.write')"
    )
    public ResponseEntity<NoteResponse> createNote(
            @Valid @RequestBody CreateNoteRequest request) {

        NoteResponse response =
                noteService.createNote(request);

        URI location =
                URI.create(
                        "/api/v1/notes/" + response.getId()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(location)
                .header(
                        HttpHeaders.ETAG,
                        noteService.buildEtag(response)
                )
                .body(response);
    }

    // =========================================================
    // PATCH /api/v1/notes/{id}
    // Requires If-Match
    // =========================================================
    @PatchMapping("/{id}")
    @PreAuthorize(
            "(hasRole('AUTHOR') or hasRole('ADMIN')) " +
                    "and hasAuthority('SCOPE_notes.write')"
    )
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable String id,
            @RequestHeader(
                    value = HttpHeaders.IF_MATCH,
                    required = false
            ) String ifMatch,
            @Valid @RequestBody UpdateNoteRequest request) {

        NoteResponse response =
                noteService.updateNote(
                        id,
                        ifMatch,
                        request
                );

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.ETAG,
                        noteService.buildEtag(response)
                )
                .body(response);
    }

    // =========================================================
    // POST /api/v1/notes/{id}/submit
    // DRAFT -> IN_REVIEW
    // Requires If-Match
    // =========================================================
    @PostMapping("/{id}/submit")
    @PreAuthorize(
            "(hasRole('AUTHOR') or hasRole('ADMIN')) " +
                    "and hasAuthority('SCOPE_notes.write')"
    )
    public ResponseEntity<NoteResponse> submitNote(
            @PathVariable String id,
            @RequestHeader(
                    value = HttpHeaders.IF_MATCH,
                    required = false
            ) String ifMatch) {

        NoteResponse response =
                noteService.submitNote(
                        id,
                        ifMatch
                );

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.ETAG,
                        noteService.buildEtag(response)
                )
                .body(response);
    }

    // =========================================================
    // POST /api/v1/notes/{id}/publish
    // IN_REVIEW -> PUBLISHED
    // ADMIN + notes.review
    // Requires If-Match
    // =========================================================
    @PostMapping("/{id}/publish")
    @PreAuthorize(
            "hasRole('ADMIN') " +
                    "and hasAuthority('SCOPE_notes.review')"
    )
    public ResponseEntity<NoteResponse> publishNote(
            @PathVariable String id,
            @RequestHeader(
                    value = HttpHeaders.IF_MATCH,
                    required = false
            ) String ifMatch) {

        NoteResponse response =
                noteService.publishNote(
                        id,
                        ifMatch
                );

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.ETAG,
                        noteService.buildEtag(response)
                )
                .body(response);
    }
}