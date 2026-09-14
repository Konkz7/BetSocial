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

## Known Traps

These have each cost real debugging time. Check them before writing similar code.

**A failed `save()` does not throw what you expect.** Spring Data JDBC's executor
catches the translated `DataIntegrityViolationException` and rethrows it wrapped in
`DbActionExecutionException`, which extends `RuntimeException` and is *not* a
`DataAccessException`. So `catch (DataIntegrityViolationException e)` around a
`save()` compiles, reads correctly, and never fires — the failure escapes to
`ApiErrorHandler`'s catch-all and comes back as a 500.

Catch `DbActionExecutionException | DataAccessException` and walk the cause chain
with `getCause()` to find the real failure; the constraint name only appears in the
driver's message at the bottom, and how many wrappers sit above it is not a number
to depend on. Repository `@Query` methods *do* throw the translated exception
directly, which is why both are caught. Always rethrow what you did not mean to
handle — a catch wide enough for this is wide enough to swallow a dead database,
and reporting an outage as the caller's mistake hides it. Worked examples:
`AccountController.takenDetail` and `Startup.ensureAdmin`.

**A soft-deleted row still holds its unique values.** Every `exists*` and lookup
query in `UserRepository` filters `deleted_at IS NULL`, and none of the unique
constraints on `user_name`, `email` or `phone_number` do. So a suspended account
answers "no" to being asked whether its email is taken while still holding it. A
pre-check cannot see this, and neither can it win a race between two concurrent
inserts — only the insert itself knows. Pre-checks are for naming which field is
wrong, not for correctness.

**Test classes own a block of phone numbers.** `phone_number` is unique across the
shared test database, so a class that borrows another's block fails whichever runs
second — the whole class, on a constraint violation that looks nothing like the
thing under test. `SecurityRegressionTest` has a comment listing blocks, but it is
not maintained and is already well behind. Find the real answer instead:

```
grep -rhoE 'new AtomicLong\(2_[0-9]{3}' --include=*.java BetSocial/src/test/java | sort -u
```

Take the next number after the highest that returns, and leave a comment on the
constant saying the block is yours.

## Git

Keep changes focused and easy to review.

Do not make large unrelated changes in the same commit.

## Security

Never expose secrets, API keys, passwords, tokens, or credentials.

Do not commit `.env` files or other secret configuration.

## Important

Before making major architectural changes, stop and ask for approval.
