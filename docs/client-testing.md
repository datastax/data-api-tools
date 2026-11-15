# Client integration testing (`dh clients`)

`dh clients test` runs each selected Data API client library's **own** integration test suite
against a live target database. This is distinct from [docs testing](docs-testing.md): it doesn't
touch documentation example snippets at all, it just checks out (or reuses a local copy of) a
client's repo and runs whatever test harness that repo already ships with.

Supported languages: `java`, `python`, `typescript`, `csharp`, `go`. `bash` is excluded — there's no
client repo behind it.

## What it actually does

For each selected language, there's a `ClientSuite` implementation
(`src/main/java/com/dtsx/dh/core/clients/impls/`) that knows:
- which repo to clone, and which branch/ref by default
- how to install its dependencies (`setup`)
- the exact command + environment variables to run its integration suite (`invocation`)
- whether it needs its target keyspaces wiped empty first

| Language   | Default repo       | Command run                                                          | Needs empty DB first | Vectorize provider used |
|------------|---------------------|-----------------------------------------------------------------------|-----------------------|--------------------------|
| `java`     | `astra-db-java`      | `mvn test -pl astra-db-java -am` (excludes `*Test.java` via Surefire) | no                    | self-skips per provider |
| `python`   | `astrapy`            | `uv run pytest tests/base/integration`                                | **yes**               | `voyageAI`               |
| `typescript` | `astra-db-ts`      | `npm run test -- ...` (repo's own `scripts/test.ts` wrapper)           | no                    | `openai`                 |
| `csharp`   | `astra-db-csharp`    | `dotnet test test/DataStax.AstraDB.DataApi.IntegrationTests`          | **yes**               | `voyageAI`               |
| `go`       | `astra-db-go`        | `go run ./integration -L` (skips the legacy suite)                     | no                    | `openai`                 |

Run order:
1. **`ClientsRunner.prepareRepos`** — clones (or reuses a local path for) and runs `setup` for every
   selected language **concurrently**. Runs even on a dry run, so the plan below can show the real
   command that would execute.
2. **`ClientsPlan.build`** — resolves the exact invocation (cwd, argv, env) for each language and
   prints it as a plan.
3. Without `-y/--yes`, the run stops here (dry run). With `-y`, it bootstraps `default_keyspace`
   (creating it if missing), then runs each language's suite **sequentially** (not in parallel —
   each writes to a live shared database), wiping the target DB first for any suite that needs one.
   `--bail` stops after the first failing language.

Each language's output streams to its own log file under the CLI's logs directory (see `dh logs`),
tailable live while the suite runs.

## Running it

```bash
dh clients test -d java,python --local -y
dh clients test -d all -e https://<db>.apps.astra.datastax.com -t AstraCS:... -y
```

Without `-y`, it just prints the plan (repos, commands, env keys that would be set) and exits —
useful for sanity-checking before actually hitting a database.

### Key options

| Flag | Description |
|---|---|
| `DRIVER...` (positional) | Comma-separated client languages to test, or `all`. |
| `-e, --api-endpoint` / `-t, --astra-token` / `--local` | Target database connection (shared with `dh docs`/`dh db`). `--local` defaults to the local Stargate stack. |
| `-y, --yes` | Actually run the suites. Without it, prints the plan and exits. |
| `-b, --bail` | Stop after the first failing language. |
| `--clean` | Delete the managed repo clones under the CLI's tmp folder and re-clone from scratch. |
| `-R, --repo` | Override a language's repo/ref, e.g. `-Rgo=someone/astra-db-go@feature/foo`, or point it at a local path. Repeatable. |
| `-K, --embedding-key` | Embedding provider credentials, keyed by the Data API's own provider name, e.g. `-K openai=sk-...`. Repeatable. |
| `--reranking-key` | Reranking API key. Falls back to `RERANKING_API_KEY`, then the Astra token on Astra; required explicitly on HCD. |
| `--cassandra-contact-point` | CQL contact point for astrapy's own tests against a non-local HCD target. Defaults to `LOCAL_CASSANDRA_CONTACT_POINT`, then (with `--local`) to `127.0.0.1`. |
| `--vectorize` / `--no-vectorize` | Run vectorize tests. Default **on**. |
| `--exhaustive-vectorize` / `--no-exhaustive-vectorize` | Run the full per-provider vectorize matrix. Default **off**. |
| `--reranking` / `--no-reranking` | Run reranking tests. Default **off**. |
| `--admin` / `--no-admin` | Run admin tests. Default **off**. |

These toggles are honored per-suite — each `ClientSuite` builds its own filter/flag logic from them,
so exact behavior (which pytest `-k` expression, which `-F`/`-tags` filter, etc.) varies by language;
read the relevant `*Suite.java` for specifics if something isn't being included/excluded as expected.

## Configuration

`dh clients ...` loads `.env.dh.common` then `.env.dh.clients` (see the root
[README](../README.md#configuration) for how env scoping works). See
[`.env.dh.clients.example`](../.env.dh.clients.example) for the full list — notably:

```bash
CLIENT_DRIVERS=java,python,typescript,go,csharp   # or 'all'
EMBEDDING_API_KEY_OPENAI=sk-...
EMBEDDING_API_KEY_NVIDIA=nvapi-...
RERANKING_API_KEY=...
LOCAL_CASSANDRA_CONTACT_POINT=127.0.0.1
```
