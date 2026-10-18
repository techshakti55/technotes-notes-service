# Delete a saved draft

`DELETE /api/v1/notes/{id}` has no request body.

Headers:

- `Authorization: Bearer <access_token>`: validated by the existing JWT resource server (issuer, audience `technotes-api`, signature and timestamps).
- `If-Match: "note-<id>-v<version>"`: use the exact ETag from the latest authenticated detail/create/update response.

The caller must have AUTHOR or ADMIN role **and** `notes.write` scope. Only the caller's own DRAFT can be deleted. ADMIN has no cross-owner exception.

| Response | Meaning |
| --- | --- |
| 204, empty body | Draft removed |
| 401 | Missing/invalid access token |
| 403 | Required role or scope absent |
| 428 `MISSING_IF_MATCH` | Missing/blank If-Match |
| 404 `RESOURCE_NOT_FOUND` | Unknown ID or another user's note |
| 412 `STALE_VERSION` | ETag mismatch, or conditional deletion lost a race |
| 409 `INVALID_STATE` | Current ETag matches, but note is not DRAFT |

Business errors use the existing ApiError shape: timestamp, status, code, message, path. Security responses retain existing Spring Security behavior; clients must not assume they use ApiError.

Order after authentication/authorization: require If-Match, look up owner-scoped note, compare ETag, check DRAFT, execute conditional deletion. Thus missing If-Match takes precedence over not-found; a stale ETag takes precedence over a non-draft state. A repeat DELETE returns 404 after a completed deletion; a concurrent losing request may return 412.

## Atomicity and related data

MongoDB removes with a single query matching `_id`, `authorId`, `status=DRAFT`, and `version`. A concurrent edit/submission changing version or state prevents removal. The preliminary read only selects conventional error responses; it does not authorize an unconditional delete. No multi-document transaction or replica set is required for this operation.

Only the `notes` document is hard-deleted. Categories are shared and remain. Revisions are created only during publish, and the current workflow never returns published notes to DRAFT, so a normal saved DRAFT has no revisions. There is no cascade into `note_revisions`; manually inconsistent/legacy revisions, if any, are retained. Deleting a draft frees its slug for future reuse.

## Gateway and UI integration

Gateway must route DELETE `/api/v1/notes/{id}` to Notes, forward Authorization and If-Match, and allow DELETE plus these headers in browser CORS/preflight. Existing ETag exposure must remain available. This change does not alter Gateway or UI.

UI should offer delete only for the current user's saved DRAFT. Send the most recent ETag, accept an empty 204 without JSON parsing, then remove the item/navigate away. On 412 reload the note and ask the user to retry with current state; do not silently retry deletion with a fresh ETag. On 409 refresh state and remove the delete action. On 404 refresh the list. Keep existing 401/403 handling.

## Tests

`NoteDraftDeletionTest` runs through the real MVC controller, security filter/converter, method authorization, owner provider, service, and exception advice; repositories and JWT decoding are mocked. It covers missing/invalid bearer, roles/scopes, both owner roles, cross-owner denial including ADMIN, unknown ID, missing/stale ETag, all non-draft states, race failure, and empty 204.

`NoteDraftDeletionMongoTest` exercises the real conditional MongoDB removal, state/version changes, and racing submission versus deletion. It runs only with the existing isolated CI URI `mongodb://localhost:25417/technotes_notes_test...`, creates a unique disposable database, and drops only that generated database after each test. Without that URI it is explicitly skipped. Existing CI's Java gate already supplies this URI.
