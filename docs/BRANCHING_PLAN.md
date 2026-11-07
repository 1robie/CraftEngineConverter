# Branching plan: `develop` for work, `master` for releases

Status: plan only, nothing done yet (2026-11-07). Same model as MachineEngine and Mekanized
(their `docs/BRANCHING_PLAN.md`, D48), adapted to this repository.

## Goal

- **`develop`**: where the work happens. Every push makes a **dev build**, posted on GitHub as the
  rolling pre-release `dev` (jar replaced on every push).
- **`master`**: protected. It only moves by a pull request from `develop` (or `hotfix/*`), opened by
  the owner, merged with a merge commit. Every merge into it is a release (tagged by hand).

## Where things stand

- Repository `1robie/CraftEngineConverter`, public, default branch `master`. No rulesets, no branch
  protection.
- **No workflows at all** in `.github/workflows` (MachineEngine/Mekanized have `build.yml`,
  `dev-build.yml`, `release-source.yml`, `release.yml`, `hangar-page.yml`).
- The build moved from Maven to **Gradle** (multi-module, `:Plugin:shadowJar` makes
  `Plugin/build/libs/CraftEngineConverter-<version>.jar`), but `.github/dependabot.yml` still says
  `maven`, and 10 stale Dependabot PRs from `dependabot/maven/*` branches are open.
- `gradle.properties` has `version = 0.0.2` (the last release, tag `0.0.2`), not a `-SNAPSHOT`.
- **Local `master` is 114 commits ahead and 6 behind `origin/master`.** The 6 are merged Dependabot
  Maven bumps (#65, #78, #79), obsolete now the build is Gradle.
- `build-logic/.../java-conventions.gradle.kts` declares `mavenLocal()`: a dependency that only
  resolves from `~/.m2` on this machine will fail on CI. Stage 3 finds that out.
- Other branches: `bedrock-converter` (local + remote), `backup/bedrock-converter-before-strip`
  (local).

## The model

```
feature/* ──PR or merge──▶ develop ──PR (merge commit)──▶ master ──tag 0.x.y──▶ GitHub release
                              ▲                              │
                              └───────── back-merge ─────────┘   (only after a hotfix)
hotfix/* (from master) ──PR──▶ master, then master ─▶ develop
```

Releases stay merge-commit only, so `master` is always an ancestor of `develop`.

## Decisions (recommended answer first)

1. **Branch name**: keep `master` (protect it as MachineEngine protects `main`) / rename to `main`
   for consistency with the other two (GitHub redirects the old name; open PRs retarget).
2. **The 114 unpushed commits**: they go to `develop` only (as MachineEngine did with its 16), and
   `origin/master` stays at the last released state until the next release PR / push them to
   `master` first, then cut `develop`.
3. **The 6 remote-only Dependabot Maven commits**: merge `origin/master` into local before cutting
   `develop` (keeps history linear with the remote, conflicts in the deleted `pom.xml`s resolve as
   "deleted") / drop them by force-pushing (not possible once the ruleset is on; not recommended).
4. **Hangar Snapshot channel**: not now, GitHub `dev` pre-release only (this plugin has no Hangar
   publishing yet) / add it like Mekanized.
5. **Default branch**: `develop` (contributors' PRs and clones get current code) / keep `master`.

## Stages

### Stage 1: reconcile and create `develop`

1. `git fetch origin` then `git merge origin/master` on local `master` (decision 3); resolve the
   `pom.xml` conflicts as deletions; `./gradlew build` still passes.
2. `git switch -c develop` and `git push -u origin develop`. Local `master` is then reset to
   `origin/master` (decision 2), so the 114 commits live on `develop` only.
3. Settings → General → default branch: `develop`.
4. Close the 10 stale `dependabot/maven/*` PRs and delete their branches.
5. Optional cleanup: delete `backup/bedrock-converter-before-strip` once `bedrock-converter` is
   confirmed to hold the same work.

### Stage 2: CI workflows (on `develop`, one commit)

- **`.github/workflows/build.yml`**: on every push and pull request, plus `workflow_dispatch`.
  JDK 21 + 25 (temurin), `gradle/actions/setup-gradle@v4`, `./gradlew build --console=plain`,
  upload `Plugin/build/libs/CraftEngineConverter-*.jar` as an artifact. Job name **`Build`**
  (stable, it becomes a required check). No CraftEngine-snapshot matrix for now: the version comes
  from `gradle/libs.versions.toml` (`craftengine = "26.10"`), there is no `-Pcraftengine_version`
  override yet.
- **`.github/workflows/release-source.yml`**: copy of MachineEngine's, with `branches: [master]`:
  a PR into `master` must be opened by the owner, from this repository, from `develop` or
  `hotfix/*`.
- **`.github/workflows/dev-build.yml`**: copy of Mekanized's, without the MachineEngine checkout
  and the Hangar step:
  - on push to `develop` (ignoring `docs/**` and `**.md`) and `workflow_dispatch`;
    `if: github.repository == '1robie/CraftEngineConverter'` and no `[skip dev]` in the message;
  - version `<next>-dev.<run_number>` from `version=0.0.3-SNAPSHOT` in `gradle.properties`;
  - job `build` (`concurrency: dev-build-build`, cancel in progress): `./gradlew build -Pversion=…`;
  - job `publish` (`concurrency: dev-build-publish`, never cancelled, `contents: write`):
    `./gradlew :Plugin:shadowJar -Pversion=…`, then
    `gh release delete dev --cleanup-tag --yes || true` and
    `gh release create dev Plugin/build/libs/CraftEngineConverter-$VERSION.jar --target $GITHUB_SHA --prerelease --latest=false`
    with title "CraftEngineConverter dev build (<version>)".
- **`gradle.properties`**: `version = 0.0.3-SNAPSHOT`.
- **`.github/dependabot.yml`**: ecosystem `gradle`, `target-branch: develop`, plus a
  `github-actions` entry.
- Check: `-Pversion` reaches the jar name (`archiveVersion = pluginVersion` = `project.version`)
  and `plugin.yml`/`paper-plugin.yml` (`finalVersion`).

### Stage 3: first runs

1. Push stage 2 to `develop`: `Build` and `Dev build` run. Fix CI-only failures (`mavenLocal`
   dependencies, missing toolchains, tests that need local files under `concepts/`).
2. The release `dev` appears under Releases as a pre-release with
   `CraftEngineConverter-0.0.3-dev.<n>.jar`, not marked latest (`0.0.2` stays latest).
3. Open a test PR `develop → master` (do not merge) so `Release source` runs once: a ruleset can
   only require a check GitHub has already seen.

### Stage 4: protect `master` and `develop` (rulesets, via `gh api`)

Same JSON as MachineEngine ruleset 25622042 / 25622049, retargeted:

- **`master: releases only`**, active, **no bypass**, `refs/heads/master`:
  deletion, non_fast_forward, pull_request (0 approvals, merge methods `merge` only),
  required_status_checks strict (up to date) with `Build` and `Release source`
  (integration_id 15368 = GitHub Actions).
- **`develop: no force push or delete`**, active, `refs/heads/develop`: deletion, non_fast_forward.
  Direct pushes stay allowed.

Verify: `git push origin master` is refused; `git push --force origin develop` is refused.

### Stage 5: docs

- CONTRIBUTING.md: target `develop`; releases = PR `develop → master`, merge commit, then tag
  `master`; hotfix flow; dev builds at Releases → `dev`; `[skip dev]` / `[skip ci]`.
- README: link to the `dev` pre-release for latest-snapshot testers.
- This file: mark the stages done.

## Later (not in this plan)

- `release.yml` on a version tag (build, check the tag is on `master`, attach the jar to a GitHub
  release), like MachineEngine's.
- A daily build against the newest CraftEngine snapshot (needs a `-Pcraftengine_version` override).
- Hangar Snapshot channel for dev builds.
