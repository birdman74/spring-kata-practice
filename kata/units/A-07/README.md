# A-07: Real security

Replace A-01's "permit everything" with real authentication and authorization.
These tests use **real** HTTP Basic credentials, not mock users. Earlier units' tests use
`@WithMockUser(roles = "LIBRARIAN")`, so they must keep passing under your new rules.

## Build
- Users (in-memory `UserDetailsService` for now):
  - `librarian` / `librarian-pass`, role `LIBRARIAN`
  - `member` / `member-pass`, role `MEMBER`
- A `PasswordEncoder` bean using BCrypt. Stored passwords are encoded with it.
- HTTP Basic, stateless sessions, CSRF still disabled.
- Rules:
  | Path | Who |
  |---|---|
  | `/api/ping`, `/api/greeting`, `/api/echo` | anyone |
  | `GET /api/books/**` | anyone |
  | any other method on `/api/books/**` | LIBRARIAN |
  | `/api/members/**` | LIBRARIAN |
  | `/api/loans/**` | any authenticated user |
  | anything else | authenticated |
- 401 (no credentials or wrong password) and 403 (wrong role) return the A-04 error shape:
  `{"status": 401, "error": "Unauthorized", "message": "..."}`

## Done when
`./kata.sh test A-07` is green (8 tests) and the **full** `./kata.sh test` is green, then:
`git add -A && git commit -m "[A-07] security" && git push`

That's the whole kata. Next is B-00 (RealWorld template) in the control-center track.

## Watch for
- Rule order matters: the first matching rule wins.
- A JSON 401 for a **wrong password** comes from the entry point configured on
  `httpBasic(...)`, not only the one in `exceptionHandling(...)`. A test checks exactly this.
- 401 vs 403: unauthenticated vs authenticated-but-not-allowed. Know which filter produces each.
