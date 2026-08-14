# Generated Replit MVP

Authoritative engineering rules are in `CLAUDE.md` and `.claude/`. Replit Agent
also follows `replit.md` for environment-specific setup.

Read `AI-DEVELOPMENT-GUIDE.md` to choose between focused Claude skills and the
optional GSD lifecycle. GSD is never initialized automatically by Replit.
Read `.claude/agent_docs/project_shape_decision.md` before deciding frontend-only
vs full-stack work.
Read `docs/architecture-overview.md` before cross-cutting implementation,
integration, data, caching, deployment, or handoff decisions, and update it when
the implemented product architecture changes.

## Decision ownership

The user owns business goals, priorities, and acceptance—not technical design.
Do not ask them to choose frameworks, architecture, persistence, API, caching,
or test mechanisms. Resolve technical ambiguity with the strongest available
reasoning role and record the rationale. Ask only for missing business behavior,
credentials/access, or an irreversible product decision, phrased in business
terms. Follow `.claude/agent_docs/agent-operating-model.md` for role routing and
the fresh final-review loop. Keep user-visible work locally demonstrable; when
the user asks to see it, invoke `local-preview` and supply safe local fixtures
when the flow otherwise has no useful data.

## Agent runtime surfaces

- **Replit Agent** uses `AGENTS.md`, `replit.md`, and `.agents/skills/`.
- **Claude Code** uses `CLAUDE.md`, `.claude/skills/`, `.claude/rules/`, and
  `.claude/agent_docs/`.
- `.agents/skills/` contains reusable workflows for Replit Agent.
- `.claude/rules/` and `.claude/agent_docs/` contain authoritative shared
  project rules and documentation.
- `.claude/skills/` is installed for local Claude Code and should not be
  treated as Replit's skill registry.

## Quick start

1. Add Clerk Auth in Replit (`CLERK_PUBLISHABLE_KEY`, `CLERK_SECRET_KEY`).
2. Set `AUTH_AUTHORIZED_PARTIES` to your app origins.
3. Run `bash scripts/configure-clerk-development.sh`.
4. Run `bash scripts/local-verify.sh`.
5. Use the Replit **Run** button.

For version-sensitive library/framework documentation, use Context7 when the
Replit account has it connected. Add it once through the Context7 link in
`replit.md`; if unavailable, use official vendor documentation and do not guess
APIs.

## Upgrading the pinned shared skills (llm-aux)

`.claude/skills/` and `.agents/skills/` were generated from a pinned revision
of the `AIAE-llm-aux` common-skills source, recorded in `llm-aux.lock`
(currently `690a9748657adf81d01702dafa2c7ecc8afcf5c5`, standard version
`0.2.0`). The standard is frozen at this revision for the duration of the
AIAE convergence migration (`docs/aiae-migration-plan.md`); once that is
complete, use the path below rather than hand-editing skills in place —
hand-edited skills drift silently, and the next real sync would silently
overwrite them.

**This repository does not carry the sync tooling itself** — `llm-aux.lock`
is a provenance record, not a runnable setup. `scripts/sync-llm-aux.sh`,
`.claude/.llm-aux-manifest`, `.agents/.llm-aux-manifest`, and
`.llm-aux-managed-skills` (the selection file) live at the standard's own
repository root, not under `templates/generated-project/scaffold/`, so they
were never copied into this project by any earlier phase. To upgrade:

1. From a checkout of the standard (`AIAE-replit-llm-aux`) at the desired
   revision, copy `scripts/sync-llm-aux.sh` into this repository's
   `scripts/`, and create a `.llm-aux-managed-skills` selection file listing
   the skill names this project actually uses (one per line) — the standard's
   own copy of that file is its own selection, not necessarily this
   project's.
2. `bash scripts/sync-llm-aux.sh --update-lock` — rewrites only the
   `revision=` line in `llm-aux.lock` to the source's current `HEAD`. Nothing
   else changes yet.
3. `bash scripts/sync-llm-aux.sh` — regenerates `.claude/skills/` and
   `.agents/skills/` for the selected skills at the pinned revision, and
   writes `.claude/.llm-aux-manifest` / `.agents/.llm-aux-manifest`.
4. Review the diff like any other dependency bump before committing —
   `git add llm-aux.lock .claude/.llm-aux-manifest .agents/.llm-aux-manifest
   .claude/skills .agents/skills` and read what actually changed. This is
   the point of pinning: the next template revision arrives as a reviewed
   diff, not as silent drift.

To roll back, set `revision=` in `llm-aux.lock` back to the previous SHA and
re-run step 3. `llm-aux.lock`'s own header comment carries this same
procedure verbatim, in case this section and that file ever disagree —
`llm-aux.lock` wins, since it is the file the script itself reads.

## Engineering handoff

When transferring this project to engineering ownership, run
`bash scripts/prepare-engineering-handoff.sh` to remove Replit control-plane
files (`AGENTS.md`, `replit.md`, `.agents/`) while preserving `CLAUDE.md` and
`.claude/`.
