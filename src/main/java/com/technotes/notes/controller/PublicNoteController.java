package com.technotes.notes.controller;

import com.technotes.notes.dto.common.PageResponse;
import com.technotes.notes.dto.note.PublicNoteCardResponse;
import com.technotes.notes.dto.note.PublicNoteDetailResponse;
import com.technotes.notes.service.PublicNoteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/notes")
public class PublicNoteController {

    private final PublicNoteService publicNoteService;

    public PublicNoteController(
            PublicNoteService publicNoteService) {

        this.publicNoteService = publicNoteService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<PublicNoteCardResponse>>
    getPublishedNotes(
            @RequestParam(required = false)
            String primaryCategoryId,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "20")
            int size,

            @RequestParam(defaultValue = "publishedAt,desc")
            String sort) {

        return ResponseEntity.ok(
                publicNoteService.getPublishedNotes(
                        primaryCategoryId,
                        page,
                        size,
                        sort
                )
        );
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PublicNoteDetailResponse>
    getPublishedNote(
            @PathVariable String slug) {

        return ResponseEntity.ok(
                publicNoteService.getPublishedNote(slug)
        );
    }
}