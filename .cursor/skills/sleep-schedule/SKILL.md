---
name: sleep-schedule
description: >-
  Audits and corrects README.md, AGENTS.md, and repo-local skills for
  inconsistencies, errors, and undocumented features against the codebase.
  Use after code or doc changes, when the stop hook requests doc sync, or when
  the user asks to sync or verify project documentation.
---

# Sleep Schedule — Doc Sync

Keep `README.md`, `AGENTS.md`, and `.cursor/skills/**/SKILL.md` aligned with the codebase. **Code is the source of truth**; docs describe what exists, not aspirations.

## When to run

Run this skill when:

- The stop hook sends a follow-up to sync docs
- You changed app code, Gradle config, navigation, data model, or UI behavior
- You added or edited repo-local skills or hooks
- The user asks to verify or update project documentation

Skip when the session was read-only (questions, reviews with no edits).

## Workflow

Copy and track:

```
Sleep schedule:
- [ ] 1. Inventory code changes (git diff / recently edited files)
- [ ] 2. Verify README.md
- [ ] 3. Verify AGENTS.md
- [ ] 4. Verify repo-local skills
- [ ] 5. Cross-check consistency across all three
- [ ] 6. Apply fixes (minimal, accurate diffs only)
- [ ] 7. Report outcome
```

### 1. Inventory

Inspect what changed:

```bash
git diff --name-only HEAD
git diff --cached --name-only
git ls-files --others --exclude-standard
```

Read affected source files. For a full audit (hook follow-up with no obvious diff), spot-check the [reference checklist](reference.md).

### 2. Verify README.md

README is the **human-facing overview**. It must be accurate but concise.

| Section | Must reflect |
|---------|----------------|
| Features | Current user-visible behavior only |
| Tech stack | Libraries actually used in app code (not unused Gradle deps) |
| Requirements | `compileSdk`, `minSdk`, JDK from `app/build.gradle.kts` |
| Build & run | Working Gradle commands |
| Project layout | Packages under `app/src/main/java/com/episode6/headachetracker/` |
| Architecture (short) | MVVM + stateless Composables; link to AGENTS.md |

Remove or fix stale claims. Do not document internal conventions here — point to AGENTS.md.

### 3. Verify AGENTS.md

AGENTS is the **contributor / AI assistant guide**. It must match implementation detail.

Confirm these areas against code (see [reference.md](reference.md) for the full inventory):

- Product model (`HeadacheEntry` fields, ranges, date format)
- Package map matches directories
- MVVM, DI (Metro), navigation (hybrid NavHost + adaptive pane), calendar layout modes
- Room version, migrations, backup format
- Build tooling (KSP, Compose BOM, SDK levels)
- Key file paths still exist
- Common pitfalls still apply

Add missing **undocumented features** discovered in code (new screens, routes, migrations, backup version bumps, layout behavior). Remove or update sections that describe removed behavior.

Ensure AGENTS.md mentions doc maintenance:

- A **Documentation** section noting this skill and the stop hook (add if absent; keep to 2–4 lines).

### 4. Verify repo-local skills

Glob `.cursor/skills/**/SKILL.md`. For each skill:

| Check | Action |
|-------|--------|
| Valid frontmatter (`name`, `description`) | Fix if missing or malformed |
| `name` matches directory name | Rename directory or frontmatter to match |
| Description is third-person, specific, includes trigger terms | Rewrite if vague |
| Body references real paths/commands | Update or remove stale references |
| Instructions match current repo behavior | Align with code and AGENTS.md |
| No references to personal-only paths (`~/.cursor/skills-cursor/`) as project skills | Fix |

Do not edit `~/.cursor/skills-cursor/` (Cursor built-ins). Only maintain `.cursor/skills/` in this repo.

### 5. Cross-check consistency

These facts must agree across README, AGENTS, and skills (when mentioned):

- Domain ranges: intensity 0–3, pills 0–2
- SDK: minSdk 26, compileSdk/targetSdk 35
- DB version and entity shape
- DI framework (Metro, not Hilt)
- Navigation model (adaptive list/detail; `Route.EditEntry` unused in NavHost)
- Backup via JSON file picker; no backend
- Calendar layout split (vertical list vs horizontal pager)

**Conflict resolution:** code → AGENTS.md → README.md → skills. README stays short; AGENTS holds depth.

### 6. Apply fixes

- Edit only what is wrong or missing; no drive-by rewrites
- Prefer updating AGENTS.md over duplicating detail in README
- User-facing strings stay in `strings.xml`, not docs (unless documenting i18n policy)
- Do not commit unless the user asks

### 7. Report

If changes were made, summarize:

```
Sleep schedule: updated [files]
- Fixed: [brief list]
- Added docs for: [undocumented features, if any]
```

If everything matched:

```
Sleep schedule: passed — README, AGENTS, and repo skills match the codebase.
```

## Anti-patterns

- Documenting unused Gradle dependencies as if the app uses them
- Duplicating full AGENTS.md content in README
- Adding time-sensitive "as of DATE" notes
- Creating new markdown files the user did not ask for
- Over-documenting obvious code in AGENTS.md

## Additional resources

- Full fact inventory: [reference.md](reference.md)
