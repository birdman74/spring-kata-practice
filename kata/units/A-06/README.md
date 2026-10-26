# A-06: Paging, sorting, JPQL

## Build
| Request | Response |
|---|---|
| `GET /api/books/paged?page=0&size=20&sort=title,asc` | page body below. Defaults: page 0, size 20, sorted by title |
| `GET /api/books/available` | `200` array of books with no active loan, ordered by title |
| `GET /api/books/stats/authors` | `200` array of `{"author": "...", "bookCount": 2}`, most books first, ties by author name |

Page body (don't serialize Spring's `Page`/`PageImpl` directly; map it to your own record):
```json
{"content": [ ...book JSON... ], "page": 0, "size": 20, "totalElements": 5, "totalPages": 1}
```

## Done when
`./kata.sh test A-06` is green (5 tests) and `./kata.sh test` is still green, then:
`git add -A && git commit -m "[A-06] paging, sorting, JPQL" && git push`

## Watch for
- Take a `Pageable` controller parameter and use `@PageableDefault`. Let Spring Data parse
  `page`, `size` and `sort` for you.
- `available` needs a custom `@Query`. Write it with `not exists` (a subquery on Loan), then
  think about how a `left join` version would differ.
- `stats/authors` is a JPQL aggregate: `group by`, `count`, and a constructor expression
  (`select new ...`) or an interface projection.
- `/api/books/paged` vs `/api/books/{id}`: know why Spring picks the literal path.
