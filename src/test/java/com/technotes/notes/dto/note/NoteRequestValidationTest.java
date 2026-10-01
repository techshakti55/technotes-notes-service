package com.technotes.notes.dto.note;

import com.technotes.notes.enums.Visibility;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class NoteRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setup() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cleanup() {
        factory.close();
    }

    private CreateNoteRequest validCreate() {
        CreateNoteRequest request = new CreateNoteRequest();
        request.setTitle("ArrayList");
        request.setSummary("Collections example");
        request.setContentMarkdown("# ArrayList\nValid content");
        request.setPrimaryCategoryId("507f1f77bcf86cd799439011");
        request.setTags(List.of("java", "spring-boot"));
        request.setVisibility(Visibility.PUBLIC);
        return request;
    }

    @Test
    void validCreateAndPartialUpdateAreAccepted() {
        assertTrue(validator.validate(validCreate()).isEmpty());

        UpdateNoteRequest update = new UpdateNoteRequest();
        update.setSummary("Updated summary");

        assertTrue(validator.validate(update).isEmpty());
        assertNull(update.getContentMarkdown());
    }

    @Test
    void missingRequiredCreateFieldsAreRejected() {
        var violations = validator.validate(new CreateNoteRequest());

        var fields = violations.stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertTrue(fields.containsAll(List.of(
                "title", "contentMarkdown",
                "primaryCategoryId", "visibility"
        )));
    }

    @Test
    void blankMarkdownIsRejectedForCreateAndUpdate() {
        for (String blank : List.of("", "   ", "\n\t")) {
            CreateNoteRequest create = validCreate();
            create.setContentMarkdown(blank);
            assertFalse(validator.validate(create).isEmpty());

            UpdateNoteRequest update = new UpdateNoteRequest();
            update.setContentMarkdown(blank);
            assertFalse(validator.validate(update).isEmpty());
        }
    }

    @Test
    void utf8ByteLimitIsEnforcedForCreateAndUpdate() {
        // Each é occupies two bytes in UTF-8.
        String atLimit = "é".repeat(524288);
        String overLimit = atLimit + "a";

        CreateNoteRequest create = validCreate();
        create.setContentMarkdown(atLimit);
        assertTrue(validator.validate(create).isEmpty());

        create.setContentMarkdown(overLimit);
        assertFalse(validator.validate(create).isEmpty());

        UpdateNoteRequest update = new UpdateNoteRequest();
        update.setContentMarkdown(atLimit);
        assertTrue(validator.validate(update).isEmpty());

        update.setContentMarkdown(overLimit);
        assertFalse(validator.validate(update).isEmpty());
    }

    @Test
    void invalidTagElementsAreRejectedForBothRequests() {
        for (List<String> tags : List.of(
                List.of("Java"),
                List.of("spring boot"),
                List.of(""),
                List.of("a".repeat(41)),
                Arrays.asList((String) null)
        )) {
            CreateNoteRequest create = validCreate();
            create.setTags(tags);
            assertFalse(validator.validate(create).isEmpty());

            UpdateNoteRequest update = new UpdateNoteRequest();
            update.setTags(tags);
            assertFalse(validator.validate(update).isEmpty());
        }
    }

    @Test
    void tagCountLimitAndEmptyTagListWorkForBothRequests() {
        List<String> tenTags = IntStream.range(0, 10)
                .mapToObj(i -> "tag-" + i)
                .toList();

        List<String> elevenTags = IntStream.range(0, 11)
                .mapToObj(i -> "tag-" + i)
                .toList();

        CreateNoteRequest create = validCreate();
        UpdateNoteRequest update = new UpdateNoteRequest();

        for (List<String> allowed : List.of(tenTags, List.<String>of())) {
            create.setTags(allowed);
            update.setTags(allowed);
            assertTrue(validator.validate(create).isEmpty());
            assertTrue(validator.validate(update).isEmpty());
        }

        create.setTags(elevenTags);
        update.setTags(elevenTags);
        assertFalse(validator.validate(create).isEmpty());
        assertFalse(validator.validate(update).isEmpty());
    }

    @Test
    void suppliedBlankTitleAndCategoryAreRejectedInUpdate() {
        UpdateNoteRequest update = new UpdateNoteRequest();
        update.setTitle("   ");
        update.setPrimaryCategoryId("\t");

        var fields = validator.validate(update).stream()
                .map(v -> v.getPropertyPath().toString())
                .toList();

        assertTrue(fields.contains("titleValid"));
        assertTrue(fields.contains("categoryValid"));
    }
}