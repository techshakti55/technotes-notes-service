# Notes Service

> Status: design notes for the first slice. The API, MongoDB shape and workflow below are proposed; this documentation change does not implement them. Confirm them through Jira acceptance criteria before coding.

## Purpose and ownership

Repository: `technotes-notes-service`  
Runtime: Java 21, Spring Boot 3.5.16, Spring Cloud 2025.0.3  
Port: 8081  
Database owner: Notes Service  
Database: MongoDB, `technotes_notes_db`

The planned Notes Service owns categories/topics and learning content: technical notes first, then blogs and interview questions. It stores editable Markdown and note metadata. Images and PDFs belong in object storage through a later Media Service; MongoDB will store attachment IDs/metadata, never Base64 file bytes.

## First release scope

The proposed first end-to-end slice is deliberately small:

1. An author creates a draft note.
2. The author retrieves that draft.
3. A later security slice enforces author ownership and reviewer permissions.
4. A later publish slice makes only reviewed, published content visible to readers.
5. A small React form is added after the API contract is stable.

Do not implement blog, interview-question, rich-editor, search, Kafka or upload workflows in this first slice. Reserve the content model so those can be added through reviewed tickets.

## Proposed first draft contract

### Create draft

`POST /api/v1/notes`

Request fields: `title` (required), `summary` (optional), `contentMarkdown` (required), `categoryId` (optional until taxonomy is finalized), and `tags` (optional).

The server generates `id`, `slug`, `status=DRAFT`, `version=1`, timestamps, and audit fields. Ignore or reject client-supplied server-owned fields. Return `201 Created` with a response DTO and resource location.

### Read draft

`GET /api/v1/notes/{id}`

The required rule is owner, reviewer or admin only for drafts; public readers may fetch a note only after publication and only when visibility permits. Until authentication exists, keep local services bound to loopback and do not deploy or expose draft APIs.

Use a consistent error response with timestamp, status, errorCode, message, path and fieldErrors. Do not return MongoDB document classes directly.

## Domain decisions that need Jira acceptance

The Library holds two different taxonomy proposals: Category → Topic references in the MongoDB design document, and a single recursive category tree in the Notes Service company standard. Resolve this before implementing domain collections. Recommended choice: a recursive `categories` collection with `parentId`, path/level metadata and cycle prevention; keep `notes` separate and reference the selected category node. This supports Java → Core Java → Collections → ArrayList without a collection per level.

Reserve a content kind such as NOTE, BLOG and INTERVIEW_QUESTION, but implement only NOTE in the first slice. Interview Q&A may later need question, answer, difficulty and tags as a defined content type; do not force those fields into the draft-note ticket.

The old documents suggest both `note_versions` and `note_revisions`. Choose one name and write the revision policy in the model ticket before implementing it. Keep MongoDB as source of truth and Elasticsearch, when introduced, as a rebuildable search projection.

## Proposed initial MongoDB shape

`categories`: `_id`, `name`, `slug`, `parentId`, `path`, `level`, `active`, `sortOrder`, audit timestamps.

`notes`: `_id`, `title`, `slug`, `summary`, `contentMarkdown`, `contentKind`, `categoryId`, `tags`, `status`, `visibility`, `authorId`, `attachmentIds`, `version`, `createdAt`, `updatedAt`, `publishedAt`.

Add indexes only for agreed query patterns, initially slug uniqueness and status/category/updatedAt listing. Enforce sibling slug uniqueness if that is the chosen category rule. Review MongoDB index creation behavior before enabling automatic index creation.

## Publishing and attachments

Draft creation never publishes content. Later workflow: DRAFT → IN_REVIEW → PUBLISHED → ARCHIVED, with rejected content returned for revision. Enforce each transition in service logic and authorization checks; never accept arbitrary status changes through a generic update API.

Attachment uploads are not part of the first draft API. A future Media Service will validate PDF/image type and size, store bytes in MinIO locally and S3-compatible storage in production, and return an attachment ID. Notes will associate that ID after ownership checks.

## Integrations and local setup

- Gateway: requests enter through port 8080; service is registered in Eureka on port 8761.
- Config Server: local optional import from port 8888.
- MongoDB Compose: host port 27018, database `technotes_notes_db`; the default URI is `mongodb://localhost:27018/technotes_notes_db`. Set `MONGODB_URI` for another local MongoDB.
- Health: `http://localhost:8081/actuator/health`.

Start Eureka, Config Server, MongoDB, Notes Service, then Gateway. Build with `./mvnw clean verify` (Windows: `mvnw.cmd clean verify`). The current baseline context test excludes MongoDB, so it does not prove persistence connectivity; add a real MongoDB integration test with the first persistence ticket.

## Security and production gates

JWT, role-based access, ownership checks, rich HTML rendering/sanitization, media uploads, revision history, rate limiting and production monitoring are not implemented by this baseline. Never publish draft content or trust UI-only restrictions. Before a public launch, add service-side authorization, safe Markdown rendering, upload validation, secure secrets/configuration, backups and operational checks.

## Library references

This note consolidates `TechNotes_Project_HQ_Master_v1`, `Notes_Service_Technical_Design_Specification_v1.0`, `TECH-12_Notes_Service_MongoDB_Database_Design_v1.0` and `TechNotes_Notes_Service_Company_Standard_v1`. Where those files conflict, the open decisions above must be resolved in Jira before code locks the schema.
