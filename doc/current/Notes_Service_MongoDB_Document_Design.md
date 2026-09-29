# TechNotes Notes Service — MongoDB Document Design

**Status:** Living design document  
**Service:** Notes Service  
**Database:** MongoDB  
**Source of truth:** `docs/first-live/notes.md` in `techshakti55/technotes-documentation`  
**Scope:** First live release only

> This file captures the current agreed design for the Notes Service MongoDB documents. When the design changes, update this same document rather than creating duplicate design files.

---

## 1. Design Rules

- The Notes Service first-live contract is authoritative.
- Use a single hierarchical `Category` model with `parentId`.
- Do not create a separate `SubCategory` or `Topic` collection for first live.
- `NoteDocument` stores the current editable note state.
- `NoteRevisionDocument` stores immutable published snapshots.
- `authorId` is derived from JWT `sub`; it is never accepted from the client request.
- Public content is visible only when both conditions are true:
  - `status = PUBLISHED`
  - `visibility = PUBLIC`
- Draft/private/archived/deleted content must never leak through public APIs.
- PATCH/submit/publish use optimistic concurrency with ETag / If-Match.
- Publication must be atomic and create an immutable revision/snapshot.

---

# 2. CategoryDocument

## 2.1 Purpose

Represents the TechNotes category hierarchy using a single collection.

Example hierarchy:

```text
Programming
   └── Java
        └── Core Java
```

Example persisted relationship:

```text
Programming
parentId = null
ancestorIds = []
level = 0

Java
parentId = Programming-ID
ancestorIds = [Programming-ID]
level = 1

Core Java
parentId = Java-ID
ancestorIds = [Programming-ID, Java-ID]
level = 2
```

## 2.2 Agreed fields

```text
id
name
slug
parentId
ancestorIds
level
active
sortOrder
version
createdAt
updatedAt
```

### Field notes

- `id` — business UUID string.
- `name` — display name.
- `slug` — unique category slug.
- `parentId` — direct parent category business ID; `null` for root categories.
- `ancestorIds` — ordered list of all ancestor category IDs.
- `level` — root is `0`, child is `1`, grandchild is `2`, etc.
- `active` — direct category active flag.
- `sortOrder` — display ordering value.
- `version` — optimistic concurrency/version field.
- `createdAt`, `updatedAt` — UTC timestamps.

## 2.3 Relationship rules

- Category hierarchy is self-referential through `parentId`.
- No separate `SubCategory` collection.
- No separate `Topic` collection for first live.
- `ancestorIds` is calculated by the service; it is not client-controlled.
- `level` is calculated by the service; it is not client-controlled.

## 2.4 Effective-active behavior

A category can be individually marked `active=true`, but public APIs should return only effectively active nodes.

Example:

```text
Parent.active = false
Child.active = true
```

The child is still not effectively public while its ancestor is inactive.

The exact validation/query implementation will be finalized in the service/repository step.

## 2.5 Initial index direction

```text
id             UNIQUE
slug           UNIQUE
parentId
active
sortOrder
```

Exact compound indexes can be refined when repository/query patterns are implemented.

---

# 3. NoteDocument

## 3.1 Purpose

Stores the current editable/working state of a note.

Published immutable content is stored separately in `NoteRevisionDocument`.

## 3.2 Agreed fields

```text
id
title
slug
summary
contentMarkdown
primaryCategoryId
tags
contentKind
authorId
status
visibility
version
createdAt
updatedAt
```

### Field notes

- `id` — business UUID string.
- `title` — editable note title.
- `slug` — server-generated note slug.
- `summary` — editable summary.
- `contentMarkdown` — editable Markdown source.
- `primaryCategoryId` — logical reference to `CategoryDocument.id`.
- `tags` — note tags.
- `contentKind` — `NOTE` for the first-live scope.
- `authorId` — immutable owner identifier from JWT `sub`.
- `status` — workflow state.
- `visibility` — public/private visibility flag.
- `version` — optimistic concurrency version.
- `createdAt`, `updatedAt` — UTC timestamps.

## 3.3 Category relationship

```text
NoteDocument.primaryCategoryId
        ↓
CategoryDocument.id
```

Use the business ID as a logical reference rather than persisting a nested category document or ORM-style object relationship.

## 3.4 Author relationship

The client never sends `authorId`.

```text
JWT sub → NoteDocument.authorId
```

The OAuth first-live contract defines `sub` as the immutable user UUID.

Protected owner-scoped reads and owner checks use this identity.

## 3.5 Status workflow

Current first-live workflow:

```text
DRAFT
   ↓ submit
IN_REVIEW
   ↓ publish
PUBLISHED
```

Any archived/deleted behavior must remain compatible with the first-live contract and must not make such content publicly accessible.

## 3.6 Visibility rule

Supported first-live visibility values:

```text
PUBLIC
PRIVATE
```

`visibility = PUBLIC` alone does not expose a draft.

Anonymous public visibility requires:

```text
status = PUBLISHED
AND
visibility = PUBLIC
```

## 3.7 Optimistic concurrency

ETag shape:

```text
"note-<uuid>-v<N>"
```

Example:

```text
version = 3
ETag = "note-<uuid>-v3"
```

Mutation request:

```http
If-Match: "note-<uuid>-v3"
```

Expected behavior:

```text
Missing If-Match  -> 428 Precondition Required
Stale If-Match    -> 412 Precondition Failed
Successful update -> version increment + new ETag
```

This applies to PATCH and the state-changing submit/publish operations according to the first-live contract.

---

# 4. NoteRevisionDocument

## 4.1 Purpose

Stores an immutable snapshot created when a note is published.

Conceptual flow:

```text
NoteDocument (current/editable)
       │
       │ publish
       ▼
NoteRevisionDocument (immutable published snapshot)
```

The snapshot must not be modified after creation.

## 4.2 Current proposed fields

```text
id
noteId
revisionNumber
slug
title
summary
contentMarkdown
primaryCategoryId
categoryName
tags
authorId
visibility
publishedAt
publishedBy
createdAt
```

### Field notes

- `id` — snapshot business UUID string.
- `noteId` — original `NoteDocument.id`.
- `revisionNumber` — publication revision number for the note.
- `slug` — published slug snapshot.
- `title` — published title snapshot.
- `summary` — published summary snapshot.
- `contentMarkdown` — immutable published Markdown.
- `primaryCategoryId` — category business ID captured at publication.
- `categoryName` — category name captured in the snapshot for stable public representation.
- `tags` — tags captured at publication.
- `authorId` — original note author UUID.
- `visibility` — visibility captured at publication.
- `publishedAt` — publication time.
- `publishedBy` — publishing ADMIN's JWT `sub`.
- `createdAt` — snapshot creation time.

## 4.3 Publication transaction

For a valid publish request, the service should validate authorization, state and concurrency first, then perform publication atomically in MongoDB.

Logical transaction:

```text
1. Create immutable NoteRevisionDocument
2. Assign revisionNumber
3. Update NoteDocument status to PUBLISHED
4. Update the current published pointer/state
5. Increment NoteDocument version
6. Commit transaction
```

If any operation fails, the publication transaction must roll back.

MongoDB transaction support therefore requires replica-set support for the publication path.

## 4.4 Published pointer/state

The first-live contract requires publication to update the published pointer/state.

The exact persisted field(s) on `NoteDocument` — for example a published revision identifier and/or revision number — are intentionally not finalized in this document yet. They will be added here when that implementation step is reached and agreed.

---

# 5. Public Read Model Rules

## Public list

```text
GET /api/v1/public/notes
```

Returns only notes that satisfy:

```text
status = PUBLISHED
AND
visibility = PUBLIC
```

The card response must not expose `contentMarkdown`.

## Public detail

```text
GET /api/v1/public/notes/{slug}
```

The public detail should use the published snapshot and expose the contract fields:

```text
id
slug
title
summary
contentMarkdown
primaryCategoryId
categoryName
tags
revisionNumber
publishedAt
```

Absent, draft, private, archived, deleted or otherwise inaccessible notes return `404` according to the first-live contract.

---

# 6. Relationship Summary

```text
CategoryDocument
       ↑
       │ primaryCategoryId
       │
NoteDocument  ← current/editable state
       │
       │ publish
       ▼
NoteRevisionDocument ← immutable published snapshot
```

Identity/authorization relationship:

```text
OAuth JWT sub
     ↓
NoteDocument.authorId

Publishing ADMIN JWT sub
     ↓
NoteRevisionDocument.publishedBy
```

---

# 7. Open Design Items

These are not implementation gaps to guess around; they should be finalized when their implementation step is reached and then updated in this same document.

1. Exact MongoDB collection names.
2. Exact compound/index strategy after query design is implemented.
3. Exact `NoteDocument` published-pointer field(s).
4. Final representation of archive/delete state if required by the fixed first-live contract.
5. Final Java annotations and MongoDB versioning approach.
6. Exact revision-number generation strategy under transaction/concurrency.

---

# 8. Change Policy

Whenever one of the agreed document models changes:

- update this file;
- do not create a duplicate replacement design document;
- keep the first-live endpoint contract unchanged unless the source-of-truth contract itself is explicitly revised;
- do not introduce legacy Topic/SubCategory/Blog/Subscription designs into this first-live model.
