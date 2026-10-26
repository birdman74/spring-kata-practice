# Spring Boot Kata: Library API

A test-first kata. You build a small library lending API from an empty `src/` folder.
Each unit unlocks a set of **failing tests**; the unit is done when they pass.
You write all of the main code. The tests are the spec.

Stack: Java 21, Spring Boot 3.5, Spring Web, Spring Data JPA, Bean Validation, Spring Security, H2.

## The loop (every unit)

```
cd /mnt/c/Users/brian/IdeaProjects/spring-kata-practice
./kata.sh next                 # unlocks the next unit's failing tests
# read kata/units/A-0N/README.md, then write code until:
./kata.sh test A-0N            # this unit's tests
./kata.sh test                 # everything unlocked so far (must stay green)
git add -A && git commit -m "[A-0N] <what you built>" && git push
```

Not finished at the end of a session? Commit with `wip` and pick it up next time:
`git add -A && git commit -m "[A-0N wip] <where you got to>" && git push`

`./kata.sh status` shows where you are. Earlier units' tests keep running in every later
unit, so a change that breaks A-03 while you work on A-06 shows up right away.

## Units

| ID | You build | Tests | Est. |
|---|---|---|---|
| A-01 | Boot app, JSON endpoints, `@ConfigurationProperties`, first `SecurityFilterChain` | `A01*` | 45-60 min |
| A-02 | `Book` JPA entity and Spring Data repository with derived queries | `A02*` | 45-60 min |
| A-03 | Book CRUD REST API: controller, service, DTOs, 201 + Location, 204, 404 | `A03*` | 60-90 min |
| A-04 | Bean Validation and one JSON error format via `@RestControllerAdvice` | `A04*` | 60 min |
| A-05 | Members and Loans: `@ManyToOne`, transactional business rules, 409s | `A05*` | 90-120 min |
| A-06 | Paging and sorting with `Pageable`, a JPQL query, a JPQL aggregate projection | `A06*` | 60-90 min |
| A-07 | Real HTTP Basic auth, BCrypt, role rules, JSON 401/403 | `A07*` | 60-90 min |

After A-07, move on to the RealWorld runs (B-00 in the control-center track).

## Rules

- **Do not edit the test files.** If a test looks wrong, it probably is the kata's fault:
  run `./kata.sh verify` (below) and tell Claude.
- The tests fix names only where they must: package `dev.brian.kata`, the classes a test
  imports (`config.KataProperties` in A-01, `book.Book` and `book.BookRepository` in A-02),
  URLs, and JSON shapes. Everything else (services, DTOs, package layout) is your design.
- Base package is `dev.brian.kata`. Put your `@SpringBootApplication` class there.

## One-time check: is the kata itself sound?

`./kata.sh verify` copies a reference solution and **all** unit tests into a temp folder
and builds them there. Your own `src/` is never touched. Run it once before A-01:

```
cd /mnt/c/Users/brian/IdeaProjects/spring-kata-practice && ./kata.sh verify
```

The reference solution lives in `kata/reference/`. Don't open it while doing a unit.
It's there to prove the tests are passable, and for comparing designs **after** you've
committed your own version.

## Commit convention (the only tracking)

The control-center morning ritual reads this repo's commits. A commit counts only if its
message **starts** with a bracketed unit ID: `[A-01]` to `[A-07]`. Add ` wip` inside the brackets
while a unit is in progress. Only the commit without `wip` marks the unit complete.
No catch-up: a missed day is never "behind"; the next unit is just the next unit.
