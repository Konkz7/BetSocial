# BetSocial Development Guidelines

## Project Goal

Revive and modernize this existing application while preserving useful existing functionality.

Do not rewrite the entire application unless there is a clear technical reason.

## Development Philosophy

* Investigate before modifying.
* Prefer small, incremental changes.
* Do not modify unrelated code.
* Do not introduce dependencies without explaining why they are necessary.
* Preserve existing functionality unless we explicitly decide to change it.
* Prefer simple solutions over unnecessary abstraction.
* Do not assume code is broken without testing it.
* Run relevant tests/builds after significant changes.
* Explain architectural decisions before implementing them.

## Working With Me

I am using Claude Code to learn software engineering as well as develop this application.

When implementing something:

1. Explain the problem.
2. Explain the proposed solution.
3. Identify the files that will change.
4. Implement the smallest sensible change.
5. Test the change.
6. Explain what changed.

Do not hide important architectural decisions from me.

## Git

Keep changes focused and easy to review.

Do not make large unrelated changes in the same commit.

## Security

Never expose secrets, API keys, passwords, tokens, or credentials.

Do not commit `.env` files or other secret configuration.

## Important

Before making major architectural changes, stop and ask for approval.
