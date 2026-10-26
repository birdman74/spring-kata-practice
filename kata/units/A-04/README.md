# A-04: Validation and one error format

Every error response from now on uses this shape (`fieldErrors` only for validation failures):
```json
{"status": 400, "error": "Bad Request", "message": "Validation failed",
 "fieldErrors": {"isbn": "must be exactly 13 digits", "title": "must not be blank"}}
```
`error` is the HTTP reason phrase (`Bad Request`, `Not Found`, `Conflict`).

## Build
- Bean Validation on the create and update request DTOs, triggered with `@Valid`:
  - `isbn`: not blank, exactly 13 digits
  - `title`, `author`: not blank
  - `publishedYear`: required, 1450 to 2100
- One `@RestControllerAdvice` that produces the error shape for:
  - validation failure -> 400, `message` = `"Validation failed"`, `fieldErrors` keyed by field name
  - malformed JSON -> 400
  - book not found -> 404, `message` contains the id
  - duplicate ISBN on create -> 409, `message` contains the ISBN

## Done when
`./kata.sh test A-04` is green (8 tests) and `./kata.sh test` is still green, then:
`git add -A && git commit -m "[A-04] validation and error handling" && git push`

## Watch for
- An empty ISBN breaks **two** constraints on the same field. If you build `fieldErrors` with
  `Collectors.toMap` without a merge function, it throws on the duplicate key.
- "Required" year: decide between `int` (missing becomes 0) and `Integer` + `@NotNull`. Both can
  pass, but only one says what you mean.
- Check for a duplicate ISBN before saving, or translate the database's constraint violation.
  Know the race condition the first approach leaves open.
