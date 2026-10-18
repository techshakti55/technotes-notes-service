package com.technotes.notes.controller;

import com.technotes.notes.document.NoteDocument;
import com.technotes.notes.enums.NoteStatus;
import com.technotes.notes.repository.CategoryRepository;
import com.technotes.notes.repository.NoteRepository;
import com.technotes.notes.repository.NoteRevisionRepository;
import com.technotes.notes.security.CurrentUserProvider;
import com.technotes.notes.security.SecurityConfig;
import com.technotes.notes.service.NoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NoteController.class)
@Import({SecurityConfig.class, CurrentUserProvider.class, NoteService.class})
class NoteDraftDeletionTest {

    private static final String ID = "507f1f77bcf86cd799439011";
    private static final String ETAG = "\"note-" + ID + "-v3\"";

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean NoteRepository notes;
    @MockitoBean CategoryRepository categories;
    @MockitoBean NoteRevisionRepository revisions;

    private NoteDocument draft;

    @BeforeEach
    void setup() {
        draft = new NoteDocument();
        draft.setId(ID);
        draft.setAuthorId("owner");
        draft.setStatus(NoteStatus.DRAFT);
        draft.setVersion(3L);
        when(notes.findByIdAndAuthorId(ID, "owner")).thenReturn(Optional.of(draft));
        when(notes.deleteOwnedDraftAtVersion(ID, "owner", 3L)).thenReturn(true);
        token("owner", "AUTHOR", "notes.write");
    }

    private void token(String subject, String role, String scope) {
        Instant now = Instant.now();
        when(jwtDecoder.decode("valid")).thenReturn(Jwt.withTokenValue("valid")
                .header("alg", "RS256").subject(subject)
                .issuer("http://localhost:9000").audience(List.of("technotes-api"))
                .issuedAt(now).expiresAt(now.plusSeconds(300))
                .claim("roles", List.of(role)).claim("scope", scope).build());
    }

    private MockHttpServletRequestBuilder request() {
        return delete("/api/v1/notes/{id}", ID)
                .header("Authorization", "Bearer valid");
    }

    @ParameterizedTest
    @CsvSource({"AUTHOR", "ADMIN"})
    void ownerWithWriteScopeDeletesDraftWithEmpty204(String role) throws Exception {
        token("owner", role, "notes.write");
        mvc.perform(request().header("If-Match", ETAG))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(notes).deleteOwnedDraftAtVersion(ID, "owner", 3L);
        verifyNoInteractions(categories, revisions);
    }

    @Test
    void missingAndInvalidBearerAre401() throws Exception {
        mvc.perform(delete("/api/v1/notes/{id}", ID).header("If-Match", ETAG))
                .andExpect(status().isUnauthorized());
        when(jwtDecoder.decode("invalid")).thenThrow(new BadJwtException("Invalid token"));
        mvc.perform(delete("/api/v1/notes/{id}", ID)
                .header("Authorization", "Bearer invalid").header("If-Match", ETAG))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(notes);
    }

    @ParameterizedTest
    @CsvSource({"AUTHOR, notes.read", "ADMIN, notes.read", "READER, notes.write"})
    void roleAndWriteScopeAreBothRequired(String role, String scope) throws Exception {
        token("owner", role, scope);
        mvc.perform(request().header("If-Match", ETAG)).andExpect(status().isForbidden());
        verifyNoInteractions(notes);
    }

    @ParameterizedTest
    @CsvSource({"AUTHOR", "ADMIN"})
    void anotherUsersDraftIsHiddenEvenFromAdmin(String role) throws Exception {
        token("other", role, "notes.write");
        when(notes.findByIdAndAuthorId(ID, "other")).thenReturn(Optional.empty());
        mvc.perform(request().header("If-Match", ETAG))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        verify(notes, never()).deleteOwnedDraftAtVersion(any(), any(), any());
    }

    @Test
    void unknownNoteIs404() throws Exception {
        when(notes.findByIdAndAuthorId(ID, "owner")).thenReturn(Optional.empty());
        mvc.perform(request().header("If-Match", ETAG)).andExpect(status().isNotFound());
        verify(notes, never()).deleteOwnedDraftAtVersion(any(), any(), any());
    }

    @Test
    void missingAndBlankIfMatchAre428BeforeLookup() throws Exception {
        mvc.perform(request()).andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("MISSING_IF_MATCH"));
        mvc.perform(request().header("If-Match", " "))
                .andExpect(status().isPreconditionRequired());
        verifyNoInteractions(notes);
    }

    @ParameterizedTest
    @CsvSource({"old", "*", "W/etag"})
    void staleOrUnsupportedEtagIs412(String etag) throws Exception {
        mvc.perform(request().header("If-Match", etag))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));
        verify(notes, never()).deleteOwnedDraftAtVersion(any(), any(), any());
    }

    @ParameterizedTest
    @EnumSource(value = NoteStatus.class, names = {"IN_REVIEW", "PUBLISHED", "ARCHIVED"})
    void nonDraftIs409(NoteStatus status) throws Exception {
        draft.setStatus(status);
        mvc.perform(request().header("If-Match", ETAG))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_STATE"));
        verify(notes, never()).deleteOwnedDraftAtVersion(any(), any(), any());
        verifyNoInteractions(revisions);
    }

    @Test
    void concurrentChangeOrDeletionReturns412() throws Exception {
        when(notes.deleteOwnedDraftAtVersion(ID, "owner", 3L)).thenReturn(false);
        mvc.perform(request().header("If-Match", ETAG))
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath("$.code").value("STALE_VERSION"));
    }
}
