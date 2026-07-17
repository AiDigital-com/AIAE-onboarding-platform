# GSD Workflow for this repo

This repository ships with GSD Core, a spec-driven development framework for AI
coding agents. The framework lives in `.claude/` and is committed to the repo,
so the team shares the same rules, commands, agents, and hooks.

## One-time setup

1. Install Node.js 18+ and Claude Code.
2. Refresh local Claude settings when needed:

```bash
npx @opengsd/gsd-core@latest --claude --local
```

3. Open the repo in Claude Code and reload so it picks up `.claude/commands`,
   `.claude/agents`, and `.claude/settings.json`.

Verify the install:

```bash
cat .claude/gsd-core/VERSION
ls .claude/commands/gsd | wc -l
```

## Engineering rules integration

GSD and the shared engineering contract coexist in `.claude/`:

- GSD owns `agents/`, `commands/`, `gsd-core/`, hooks, and install metadata.
- The engineering contract owns `agent_docs/`, `rules/`, custom `skills/`, and
  the repository-root `CLAUDE.md`.
- GSD planners, executors, reviewers, and fixers must treat `CLAUDE.md` and
  project skills as hard constraints.
- Do not remove or replace GSD when updating engineering rules.

After a GSD upgrade, verify that both surfaces still exist:

```bash
test -f .claude/gsd-core/VERSION
test -f CLAUDE.md
test -f .claude/rules/00-backend-hard-rules.md
test -f .claude/skills/task-workflow/SKILL.md
```

## Phase loop

Run these inside Claude Code:

| Step | Command | What it does |
| --- | --- | --- |
| 1. Discuss | `/gsd-discuss-phase` | Capture decisions before code is written |
| 2. Plan | `/gsd-plan-phase` | Research and decompose work into `PLAN.md` |
| 3. Execute | `/gsd-execute-phase` | Build the plan |
| 4. Verify | `/gsd-verify-work` | Validate the result and generate fix plans if needed |
| 5. Ship | `/gsd-ship` | Open a PR and archive the completed phase |

Helpful anytime:

- `/gsd-help`
- `/gsd-progress`
- `/gsd-stats`

## Maintenance

```bash
npx @opengsd/gsd-core@latest --claude --local
npx @opengsd/gsd-core@latest --claude --local --dry-run
npx @opengsd/gsd-core@latest --claude --local --uninstall
```
