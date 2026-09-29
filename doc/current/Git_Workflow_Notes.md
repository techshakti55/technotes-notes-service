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
  ↑ release/stable integration
  |
develop
  ↑ feature integration
  |
feature/<feature-name>
```

Normal development happens on a feature branch. A logical piece of work is committed and pushed on that feature branch. When the feature is ready and tested, it is integrated into `develop`. `main` is updated at a stable/release milestone rather than after every small code change.

---

# 2. Scenario A — Create a Fresh Feature Branch from `develop`

This is the normal workflow to use when starting new work.

## Step 1 — Switch to `develop`

```powershell
git checkout develop
```

Purpose: move the local working directory to the integration branch from which the new feature should start.

## Step 2 — Check working tree

```powershell
git status
```

Purpose: verify the current branch and ensure there are no accidental uncommitted changes before switching/creating branches.

Desired result:

```text
On branch develop
nothing to commit, working tree clean
```

If the working tree is not clean, do not blindly create/switch branches. First understand whether the changes should be committed, stashed, or discarded.

## Step 3 — Update local `develop`

```powershell
git pull origin develop
```

Purpose: download and integrate the latest remote `develop` changes so the new feature starts from the latest integration baseline.

## Step 4 — Create and switch to feature branch

Example used for Notes first-live work:

```powershell
git checkout -b feature/notes-first-live
```

Meaning:

- `checkout` — switch branch/working tree.
- `-b` — create a new branch first.
- `feature/notes-first-live` — new branch name.
- The new branch starts from the commit currently checked out (`develop` in this workflow).

## Step 5 — Verify current branch

```powershell
git branch --show-current
```

Expected:

```text
feature/notes-first-live
```

Purpose: prevent accidentally writing code on `develop`, `main`, or an old feature branch.

Useful additional command:

```powershell
git branch
```

The branch marked with `*` is the currently checked-out branch.

## Step 6 — Publish branch to GitHub

```powershell
git push -u origin feature/notes-first-live
```

Meaning:

- `push` — send local commits/ref to the remote repository.
- `origin` — the configured GitHub remote.
- `-u` — set the remote branch as this local branch's upstream/tracking branch.

After this first push, normal future pushes from this branch can usually be:

```powershell
git push
```

---

# 3. Normal Work While Staying on the Feature Branch

Do not switch branches after every Java file.

Typical loop:

```powershell
git status
.\mvnw.cmd test
git add <files>
git commit -m "feat: describe the logical change"
git push
```

Commit at logical checkpoints, for example:

```text
Documents complete
Repositories complete
DTO set complete
Security configuration complete
Category API complete
```

Do not create a separate branch for every small file unless the team's ticket/branch policy specifically requires it.

---

# 4. Scenario B — New Code Was Accidentally Written on an Old Feature Branch

This happened during the initial Notes Service work. New Notes implementation was created while the working branch was still:

```text
feature/TEC-14-git-workflow-guide
```

The code itself was useful, but it was on the wrong/old branch.

The recovery objective was:

```text
preserve new code
      ↓
clean/synchronize branch baseline
      ↓
integrate useful implementation into develop
      ↓
start future work from a fresh feature branch
```

## Step 1 — Inspect the situation

```powershell
git status
```

Purpose: identify:

- current branch;
- modified files;
- new/untracked files;
- whether a merge is in progress.

This command revealed that Notes implementation had been made on the old `feature/TEC-14-git-workflow-guide` branch.

## Step 2 — Stage the intended implementation

The relevant files were staged explicitly, for example:

```powershell
git add pom.xml
git add src/main/resources/application.yaml
git add src/main/java/com/technotes/notes/TechnotesNotesServiceApplication.java
git add src/main/java/com/technotes/notes/enums
git add src/main/java/com/technotes/notes/document
```

Purpose: put the intended changes into Git's staging area for the next commit.

Why explicit paths can be useful during recovery: they make it easier to see exactly what is being committed instead of blindly staging unrelated files.

## Step 3 — Verify staged changes

```powershell
git status
```

Purpose: confirm that only the intended files appear under `Changes to be committed`.

## Step 4 — Commit the useful work on the current branch

```powershell
git commit -m "feat: add initial Notes Service domain model"
```

Purpose: create a safe Git snapshot of the work before branch cleanup/integration.

## Step 5 — Push the branch

```powershell
git push
```

If the branch has no upstream yet, Git may require:

```powershell
git push -u origin <branch-name>
```

Purpose: create a remote backup and make the commit available for later integration.

---

# 5. Synchronizing `develop` with the Existing Baseline

During this recovery, `main` contained commits that `develop` did not yet contain. Before integrating the implementation, the baseline was synchronized.

## Inspect recent history

```powershell
git log --oneline -5
```

Purpose: show recent commits in compact form and understand where `main`, `develop`, and feature branches point.

Useful richer form:

```powershell
git log --oneline --decorate -5
```

`--decorate` displays branch/ref names next to commits.

## Switch to `develop`

```powershell
git checkout develop
```

## Update remote `develop`

```powershell
git pull origin develop
```

## Bring the current `main` baseline into `develop`

```powershell
git merge main
```

Purpose in this specific recovery: make sure `develop` includes the baseline/documentation commits that were already on `main` before integrating the newer Notes implementation.

Then publish:

```powershell
git push origin develop
```

Important: this was a repository synchronization/recovery step. It is not something that must be repeated after every small code change.

---

# 6. Integrating the Old Feature Branch into `develop`

With `develop` checked out:

```powershell
git merge feature/TEC-14-git-workflow-guide
```

Purpose: bring the useful Notes implementation commit from the old feature branch into `develop`.

If there are conflicts, Git stops the merge. Resolve each conflict intentionally, then stage the resolved files.

Check state:

```powershell
git status
```

During our recovery Git reported:

```text
All conflicts fixed but you are still merging.
(use "git commit" to conclude merge)
```

This means conflict resolution is done, but the merge commit has not yet been created.

Conclude the merge:

```powershell
git commit -m "merge: integrate Notes Service initial domain model into develop"
```

Then verify:

```powershell
git status
```

Run tests before publishing the integrated branch:

```powershell
.\mvnw.cmd test
```

Expected:

```text
BUILD SUCCESS
```

Then push:

```powershell
git push origin develop
```

Finally inspect history:

```powershell
git log --oneline --decorate -5
```

---

# 7. Starting Clean Work After Recovery

After `develop` contains the intended implementation and tests pass, start future work from `develop` using Scenario A:

```powershell
git checkout develop
git status
git pull origin develop
git checkout -b feature/notes-first-live
git branch --show-current
git push -u origin feature/notes-first-live
```

Result:

```text
develop
   |
   +-- feature/notes-first-live  ← active Notes first-live development
```

---

# 8. Quick Meaning of Core Commands

| Command | Meaning |
|---|---|
| `git status` | Shows current branch and working/staging state. |
| `git branch` | Lists local branches; `*` marks current branch. |
| `git branch --show-current` | Prints only the current branch name. |
| `git checkout develop` | Switches to local `develop`. |
| `git checkout -b <name>` | Creates a branch from the current commit and switches to it. |
| `git pull origin develop` | Fetches and integrates latest remote `develop`. |
| `git add <path>` | Stages a file/directory for the next commit. |
| `git commit -m "..."` | Creates a local commit from staged changes. |
| `git push` | Pushes commits to the configured upstream branch. |
| `git push -u origin <branch>` | First push and sets upstream tracking. |
| `git merge <branch>` | Integrates the named branch into the currently checked-out branch. |
| `git log --oneline -5` | Shows five recent commits compactly. |
| `git log --oneline --decorate -5` | Same plus branch/ref labels. |

---

# 9. Safety Checklist Before Starting New Work

Run:

```powershell
git branch --show-current
git status
```

Ask:

1. Am I on the intended feature branch?
2. Is the working tree in the state I expect?
3. Was this branch created from the correct/latest `develop` baseline?

These two quick checks prevent most of the wrong-branch problem encountered in Scenario B.

---

# 10. Document Update Rule

When another Git situation occurs — PR workflow, merge conflict, stash, rebase, rollback, cherry-pick, branch cleanup, etc. — add the verified procedure to **this same file** after we actually use/confirm it.

Do not add commands merely because they are possible; record the workflow we intentionally adopt for this repository.