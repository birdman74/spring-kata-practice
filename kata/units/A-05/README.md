# A-05: Members, loans, relationships, transactional rules

## Build
**Members** (`Member` entity: `id`, `name` required, `email` required + unique)
| Request | Response |
|---|---|
| `POST /api/members` `{"name","email"}` | `201` + Location, `{"id","name","email"}`; 400 on blank name or invalid email; 409 on duplicate email |
| `GET /api/members/{id}` | `200` or `404` |
| `GET /api/members/{id}/loans` | `200` array of that member's loans; `?active=true` returns only unreturned ones; `404` for unknown member |

**Loans** (`Loan` entity: `@ManyToOne` to `Book` and to `Member`, `loanedAt`, `returnedAt` nullable)
| Request | Response |
|---|---|
| `POST /api/loans` `{"bookId","memberId"}` | `201` + Location, `{"id","bookId","memberId","loanedAt","returnedAt"}` |
| `POST /api/loans/{id}/return` | `200` loan with `returnedAt` set |

Business rules (409 in the A-04 error format unless noted):
1. Both ids required (400 with `fieldErrors`); unknown book or member -> 404.
2. A book can have only **one active** loan.
3. A member can have at most **3 active** loans.
4. A loan can be returned once; returning again -> 409; unknown loan -> 404.
5. A returned book can be loaned again.
6. `DELETE /api/books/{id}` on a book with **any** loan history -> 409, and the book stays.

## Done when
`./kata.sh test A-05` is green (14 tests) and `./kata.sh test` is still green, then:
`git add -A && git commit -m "[A-05] members and loans" && git push`

## Watch for
- Use `FetchType.LAZY` on the `@ManyToOne`s and map entities to DTOs inside the service's
  transaction. Consider `spring.jpa.open-in-view: false` so lazy loading outside a transaction
  fails loudly instead of quietly.
- Rule 6 changes A-03's delete behavior. Your A-03 tests must still pass.
- Every rule check and the save belong in **one** `@Transactional` service method.
