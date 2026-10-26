# A-01: Bootstrap, JSON endpoints, configuration, first security config

You start with only `pom.xml`. Everything below is yours to write.

## Build
1. `src/main/java/dev/brian/kata/KataApplication.java`: the `@SpringBootApplication` class.
2. `src/main/resources/application.yml` with `kata.greeting: Hello`.
3. `dev.brian.kata.config.KataProperties`: a **record** with one component `greeting`, bound with
   `@ConfigurationProperties(prefix = "kata")`. Register it (`@ConfigurationPropertiesScan` or
   `@EnableConfigurationProperties`).
4. A controller with:
   - `GET /api/ping` -> `200 {"status": "UP"}`
   - `GET /api/greeting?name=Brian` -> `200 {"message": "Hello, Brian"}`; `name` defaults to `World`.
     The prefix comes from `KataProperties`, never hard-coded.
   - `POST /api/echo` with body `{"text": "..."}` -> `200` with the same body back.
5. A `SecurityFilterChain` bean that, **for now**, permits every request and disables CSRF
   (this is a stateless JSON API). Spring Security is already on the classpath, so without this
   every request gets 401. A-07 replaces "permit everything" with real rules.

## Done when
`./kata.sh test A-01` is green (6 tests), then:
`git add -A && git commit -m "[A-01] bootstrap, config properties, security baseline" && git push`

## Watch for
- Without CSRF disabled, the anonymous `POST /api/echo` fails with 403.
- `A01ConfigurationPropertiesTest` overrides `kata.greeting=Howdy`. If the greeting is
  hard-coded anywhere, that test catches it.
- First run downloads dependencies, so give Maven a minute.
