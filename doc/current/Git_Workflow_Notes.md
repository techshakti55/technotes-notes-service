# TechNotes Notes Service — Git Workflow Notes

**Status:** Living reference document  
**Repository:** `techshakti55/technotes-notes-service`  
**Working integration branch:** `develop`  
**Stable/release branch:** `main`

> Keep updating this same file when a new Git scenario is learned. Do not create duplicate Git command notes.

---

## 1. Branch Model

```text
main
  ↑ stable/release integration
  |
develop
  ↑ feature integration
  |
feature/<feature-name>
```

Normal development happens on a feature branch. Commit and push logical pieces of work there. When a checkpoint is ready and tested, open a PR from the feature branch into `develop`. `main` is updated only at an intentional stable/release milestone.

---

## 2. Scenario A — Create a Fresh Feature Branch from `develop`

Use this for normal new work.

```powershell
git checkout develop
git status
git pull --ff-only origin develop
git checkout -b feature/notes-first-live
git branch --show-current
git push -u origin feature/notes-first-live
```

### Why each command is used

- `git checkout develop` — move to the integration branch.
- `git status` — confirm branch and ensure there are no accidental local changes.
- `git pull --ff-only origin develop` — update local `develop` without creating an accidental local merge commit.
- `git checkout -b <branch>` — create a feature branch from the current `develop` commit and switch to it.
- `git branch --show-current` — verify that coding will happen on the intended feature branch.
- `git push -u origin <branch>` — publish the branch and configure its upstream.

After the first upstream push, later pushes can normally be:

```powershell
git push
```

---

## 3. Normal Work on a Feature Branch

Do not switch branches after every Java file. Work on the feature branch and commit at logical checkpoints.

```powershell
git branch --show-current
git status
.\mvnw.cmd test
git add <intended-files>
git status
git commit -m "feat: describe the logical change"
git push
```

Examples of logical checkpoints:

```text
Documents complete
Repositories + DTOs + mappers complete
Security configuration complete
Category API complete
Publication flow complete
```

Prefer explicit `git add <path>` during recovery or when unrelated changes exist. `git add .` is acceptable only after `git status` confirms every current change belongs in the same commit.

---

## 4. Scenario B — New Code Was Accidentally Written on an Old Feature Branch

This occurred when useful Notes Service implementation was created while still on:

```text
feature/TEC-14-git-workflow-guide
```

The goal was to preserve the code, integrate it safely, and then start future work from a fresh branch.

### Inspect and preserve the work

```powershell
git status
```

Stage only intended files, for example:

```powershell
git add pom.xml
git add src/main/resources/application.yaml
git add src/main/java/com/technotes/notes/TechnotesNotesServiceApplication.java
git add src/main/java/com/technotes/notes/enums
git add src/main/java/com/technotes/notes/document
```

Verify:

```powershell
git status
```

Commit and push the useful work:

```powershell
git commit -m "feat: add initial Notes Service domain model"
git push
```

If no upstream exists yet:

```powershell
git push -u origin <branch-name>
```

### Synchronize the baseline when required

In this specific recovery, `main` contained baseline/documentation commits not yet present in `develop`. We inspected history and synchronized deliberately:

```powershell
git log --oneline --decorate -5
git checkout develop
git pull origin develop
git merge main
git push origin develop
```

This was a one-time repository recovery/synchronization step, **not** a command sequence to repeat after every feature.

### Integrate the old feature work into `develop`

```powershell
git checkout develop
git merge feature/TEC-14-git-workflow-guide
```

If Git reports conflicts, resolve them intentionally and stage the resolved files. When `git status` says:

```text
All conflicts fixed but you are still merging.
(use "git commit" to conclude merge)
```

finish the merge:

```powershell
git commit -m "merge: integrate Notes Service initial domain model into develop"
.\mvnw.cmd test
git push origin develop
git log --oneline --decorate -5
```

After recovery, start new work from updated `develop` using Scenario A.

---

## 5. Scenario C — `git push` Rejected with `fetch first`

This occurred on `feature/notes-first-live`. A documentation commit had been added to the remote feature branch while local development also created a new commit. The local branch therefore did not contain the latest remote commit.

Typical error:

```text
! [rejected] feature/notes-first-live -> feature/notes-first-live (fetch first)
Updates were rejected because the remote contains work that you do not have locally.
```

### Important rule

Do **not** solve this with `git push --force` when the remote commit is legitimate work that must be kept.

### Verified recovery used

```powershell
git pull --rebase origin feature/notes-first-live
```

Purpose: fetch the remote branch and replay the local-only commit on top of the remote commit, avoiding an unnecessary merge commit.

Then verify:

```powershell
git status
git log --oneline --decorate -5
```

The verified history became conceptually:

```text
develop baseline
   ↓
remote documentation commit
   ↓
local persistence/DTO/mapper commit
```

When status says the local branch is ahead of its upstream and the working tree is clean:

```powershell
git push
```

Then verify again:

```powershell
git status
```

Desired result:

```text
Your branch is up to date with 'origin/feature/notes-first-live'.
nothing to commit, working tree clean
```

### If rebase reports a conflict

Do not blindly continue. Resolve the conflicting files, stage them, then continue the rebase:

```powershell
git add <resolved-files>
git rebase --continue
```

If the rebase should be abandoned:

```powershell
git rebase --abort
```

---

## 6. Scenario D — Feature Checkpoint → Pull Request → `develop`

This is the preferred integration workflow for normal feature work.

### On the feature branch

First verify and test:

```powershell
git branch --show-current
git status
.\mvnw.cmd test
```

Stage intended work and inspect it:

```powershell
git add <intended-files>
git status
```

Commit and push:

```powershell
git commit -m "feat: describe the checkpoint"
git push
```

### Create the PR

Use:

```text
base:    develop
compare: feature/<feature-name>
```

For the Notes first-live foundation the actual PR was:

```text
feature/notes-first-live → develop
PR #4
```

Before merging, confirm the PR has no unresolved conflicts and that the intended files/commits are present.

For this repository workflow we selected **Create a merge commit**. This preserves the feature commits and adds an explicit PR merge commit on `develop`.

Do not merge the feature directly into `main` at this checkpoint.

---

## 7. Scenario E — PR Merged on GitHub but Local `develop` Still Shows the Old Commit

After PR #4 was merged on GitHub, local `git status` initially said `develop` was up to date, while `git log` still showed the old commit. This happened because the local remote-tracking reference `origin/develop` had not been refreshed yet.

### Diagnose

```powershell
git log --oneline --decorate -7
git status
```

If both local `develop` and the locally cached `origin/develop` still show the old commit, refresh remote references:

```powershell
git fetch origin
```

In the verified Notes Service case this moved the remote-tracking branch:

```text
47b795b..166b26d  develop -> origin/develop
```

Inspect remote history before changing local `develop`:

```powershell
git log --oneline --decorate -7 origin/develop
```

The new remote history showed:

```text
166b26d Merge pull request #4 from techshakti55/feature/notes-first-live
4396439 feat: add category service controller and exception handling
6a26551 feat: add Notes persistence repositories DTOs and mappers
7cdb9fd docs: add Git workflow and recovery notes
47b795b old develop HEAD
```

### Fast-forward local `develop`

Because the local working tree was clean and local `develop` had no independent commits, we used:

```powershell
git pull --ff-only origin develop
```

Why `--ff-only`:

- local `develop` simply moves forward to the remote commit;
- no unnecessary local merge commit is created;
- Git refuses instead of silently creating a merge if fast-forward is impossible.

Final verification:

```powershell
git status
git log --oneline --decorate -7
```

Verified result:

```text
166b26d (HEAD -> develop, origin/develop) Merge pull request #4 from techshakti55/feature/notes-first-live
```

and:

```text
nothing to commit, working tree clean
```

At this point local and remote `develop` are synchronized.

---

## 8. `fetch` vs `pull` — What We Learned Practically

```powershell
git fetch origin
```

Downloads updated remote information and moves remote-tracking references such as `origin/develop`, but does not move the checked-out local `develop` branch.

```powershell
git pull --ff-only origin develop
```

Fetches/integrates the remote `develop` into the current local branch, but only when that integration can be a clean fast-forward.

This is why after PR #4:

```text
Before fetch:
local develop  = 47b795b
origin/develop = 47b795b  (stale local knowledge)

After git fetch origin:
local develop  = 47b795b
origin/develop = 166b26d

After git pull --ff-only origin develop:
local develop  = 166b26d
origin/develop = 166b26d
```

---

## 9. Core Command Reference

| Command | Meaning |
|---|---|
| `git status` | Shows current branch plus working/staging state. |
| `git branch` | Lists local branches; `*` marks current branch. |
| `git branch --show-current` | Prints the current branch name. |
| `git checkout develop` | Switches to local `develop`. |
| `git checkout -b <name>` | Creates a branch from current HEAD and switches to it. |
| `git fetch origin` | Refreshes remote objects/references without moving the current local branch. |
| `git pull origin develop` | Fetches and integrates remote `develop`. |
| `git pull --ff-only origin develop` | Updates from remote `develop` only if a fast-forward is possible. |
| `git pull --rebase origin <branch>` | Integrates remote changes and replays local-only commits on top. |
| `git add <path>` | Stages a file/directory for the next commit. |
| `git commit -m "..."` | Creates a local commit from staged changes. |
| `git push` | Pushes commits to the configured upstream branch. |
| `git push -u origin <branch>` | First push and sets upstream tracking. |
| `git merge <branch>` | Integrates the named branch into the currently checked-out branch. |
| `git rebase --continue` | Continues a paused rebase after conflicts are resolved/staged. |
| `git rebase --abort` | Cancels the current rebase and restores the pre-rebase state. |
| `git log --oneline --decorate -7` | Shows compact recent history with branch/ref labels. |

---

## 10. Safety Checklist

Before starting work:

```powershell
git branch --show-current
git status
```

Before committing:

```powershell
git status
.\mvnw.cmd test
git add <intended-files>
git status
```

Before integrating to `develop`:

```text
1. Feature branch pushed?
2. Tests passed?
3. PR base = develop?
4. PR compare = intended feature branch?
5. Conflicts resolved?
6. Files changed reviewed?
```

After a GitHub PR merge:

```powershell
git checkout develop
git fetch origin
git log --oneline --decorate -7 origin/develop
git pull --ff-only origin develop
git status
git log --oneline --decorate -7
```

---

## 11. Repository Rules We Are Following

1. Do normal implementation on `feature/*`, not directly on `develop` or `main`.
2. Use `develop` as the feature integration branch.
3. Use PRs for normal feature → `develop` integration.
4. Keep `main` for intentional stable/release integration.
5. Do not force-push merely to bypass a `fetch first` rejection when legitimate remote work exists.
6. Keep the working tree clean at branch/integration checkpoints.
7. Run tests before publishing an implementation checkpoint.
8. Prefer `--ff-only` when local `develop` should simply catch up with already-merged remote `develop`.
9. Record only workflows we actually use/verify in this living document.

---

## 12. Document Update Rule

When another real Git situation occurs — stash, cherry-pick, rollback, reset/revert, branch cleanup, release PR, additional rebase/conflict recovery, etc. — add the verified procedure to **this same file**.

Do not create duplicate Git command documents and do not add commands merely because they are possible. Record the workflow actually adopted and verified for this repository.