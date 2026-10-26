# A-03: Book CRUD REST API

Controller -> service -> repository, with request/response DTOs (records). Don't return
the entity from the controller.

## Build
| Request | Response |
|---|---|
| `POST /api/books` `{"isbn","title","author","publishedYear"}` | `201`, `Location: .../api/books/{id}`, body = book JSON |
| `GET /api/books/{id}` | `200` book, or `404` |
| `GET /api/books` | `200` array of all books, ordered by title |
| `GET /api/books?author=x` | `200` array, author matched case-insensitively, ordered by title |
| `PUT /api/books/{id}` `{"title","author","publishedYear"}` | `200` updated book (ISBN unchanged), or `404` |
| `DELETE /api/books/{id}` | `204`, or `404` |

Book JSON: `{"id": 1, "isbn": "...", "title": "...", "author": "...", "publishedYear": 2018}`

## Done when
`./kata.sh test A-03` is green (9 tests) and `./kata.sh test` (A-01 to A-03) is still green, then:
`git add -A && git commit -m "[A-03] book CRUD API" && git push`

## Watch for
- 404 is just a status code in this unit. A-04 adds the JSON error body, so a custom
  exception now pays off later.
- Build the Location header from the current request (`ServletUriComponentsBuilder`), not by
  string-concatenating a host.
- Transaction boundaries belong on the service, not the controller.
