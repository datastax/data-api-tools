# Documentation example testing (`dh docs`)

`dh docs` reads code examples from an examples folder (e.g. the `astra-vector-docs` repo, or
`resources/mock_examples` for local dev) and runs/verifies them across client languages, to catch
unintentional breaking changes or documentation errors before they ship.

Supported languages: `java`, `python`, `typescript`, `csharp`, `go`, `bash`.

## Examples folder structure

```
examples/
  _base/
    prelude.js          # shared setup logic imported by fixture files
    meta.schema.json     # JSON Schema for meta.yml (authoritative reference for the fields below)
  _fixtures/
    <fixture>.js         # base fixtures, reused across many test roots
  <example>/              # a test root: any dir containing meta.yml
    meta.yml
    example.ts
    example.py
    example.sh
    [fixture.js]          # optional per-test fixture
    java/                 # compiled-language examples can be nested project dirs
    csharp/
  ...
```

A *test root* is any directory (at any depth, found via `-ef/--examples-folder`) containing a
`meta.yml`. Within it, example files are named `example.<ext>` per language (found at any depth for
compiled languages that need their own project structure, e.g. `java/src/main/java/Example.java`).

## Fixture files

Fixture files are JS files exporting any of `Setup`, `Reset`, `Teardown` (all optional):

```js
import * as $ from '../_base/prelude';

export async function Setup() {
  // expensive one-time setup, e.g. creating a collection
}

export async function Reset() {
  // cheap per-test reset, e.g. clearing rows/documents
}

export async function Teardown() {
  // final cleanup
}
```

There are two kinds:
- **Base fixture** — lives in `_fixtures/`, referenced by `meta.yml`'s `fixtures.base`. Set up once
  and reused across every test root that references it (pooled), with `Reset` clearing state
  between tests.
- **Per-test fixture** — lives in the test root itself, always named `fixture.js`. Builds specific
  data on top of the base fixture for that one example.

## `meta.yml`

Each test root's `meta.yml` is validated against `_base/meta.schema.json` — that's the authoritative
source for every field; summarized here:

```yaml
$schema: "../_base/meta.schema.json"
test:
  type: snapshot          # or 'compiles'
  skip: false              # bool, or per-language: { PYTHON: all, JAVA: none }
  vars: {}                 # placeholder vars: { NAME: { regex: ..., value: ... } }
execution: shared          # 'sequential' | 'shared' | 'isolated' (optional)
fixtures:
  base: <fixture>.js        # required for snapshot tests that need one
snapshots:
  share: true               # bool, or per-language
  sources:
    stdout::matches: { regex: '^$' }
    collection::documents: { projection: { "$vector": true } }
```

- `test.type: compiles` — just verifies the example compiles/runs without error; no snapshot taken.
- `test.type: snapshot` — captures the configured `snapshots.sources` and diffs against an approved
  `.approved.txt` file (see [Snapshot testing](#snapshot-testing)).
- `execution` controls how a test root's languages run relative to each other and the fixture pool:
  `sequential` (one language at a time), `shared` (parallel, sharing one fixture instance), or
  `isolated` (parallel, each language gets its own fixture instance).
- `snapshots.sources` keys are `<category>::<kind>`, each with its own params shape (see the schema
  file for the full list). Real examples in this repo: `stdout::matches`, `stdout::jsonify`,
  `stderr::matches`, `collection::documents`, `collection::definition`, `table::rows`,
  `table::definition`, `udt::definitions`, `table::index::definitions`.
- `snapshots.share` — whether the same snapshot file is reused across all languages for that test
  root (default) or split per-language, for when output legitimately differs by client.

## Snapshot testing

Snapshot (approval) testing captures a test's configured output/state on a run and stores it as a
`.approved.txt` file, used as the reference for future runs. A run that differs from its approved
snapshot fails, surfacing a potential regression; if the change is intentional, the snapshot gets
re-approved with the new content (see [Reviewing snapshot changes](#reviewing-snapshot-changes)).

## Commands

### `dh docs test`

Runs every discovered test root and verifies against approved snapshots.

```bash
dh docs test -d java,python --local
dh docs test -d all -e <endpoint> -t <token> -f delete-many -r only_failures
```

| Flag | Description |
|---|---|
| `DRIVER...` (positional) | Languages to test, or `all`. |
| `-e/-t/--local` | Target database connection. |
| `-ef, --examples-folder` | Path to the examples folder. Auto-discovers `_base/`/`_fixtures/` under it, `modules/api-reference/examples/`, or `resources/mock_examples/`. |
| `-m, --verify-mode` | `normal` (default) \| `verify_only` \| `compile_only` \| `dry_run`. |
| `-D, --dry-run` | Shorthand for `-m dry_run`. |
| `-r, --test-reporter` | `all_tests` (default) \| `only_failures`. |
| `-f, --filter` / `-F, --filter-not` | Comma-separated regex filters to include/exclude tests. Repeatable; OR'd by default, `-fand` switches to AND. |
| `-n, --max-fixture-instances` | Max base-fixture instances created for isolated execution (default 5). |
| `-A, --client-artifact` | Override a language's client library version/path, e.g. `-Atypescript=@datastax/astra-db-ts@v2.0.0`. |
| `--clean` | Clean the execution environment before running. |
| `-b, --bail` | Stop after the first failing test. |

### `dh docs run`

Just executes script file(s) and prints output — no fixtures, no snapshot verification.

```bash
dh docs run -d typescript --local path/to/example.ts
```

Takes the same connection/examples-folder/artifact-override options as `dh docs test`, plus:

| Flag | Description |
|---|---|
| `FILE...` (positional) | Script file(s) to run, comma-separated. |
| `-c, --collection` / `--table` / `-k, --keyspace` | Placeholder values substituted into the script. |
| `-r, --replace` | Arbitrary `KEY=value` placeholder overrides. |
| `-p, --plain` | Plain output, no colors/formatting (single file only). |

### `dh docs review`

Launches the snapshot review dashboard: runs `npm install` then `npm run dev` inside
`snapshot-dashboard/`, automatically setting `EXAMPLES_DIR` and `PORT` for it and opening a browser.

```bash
dh docs review -ef resources/mock_examples
dh docs review -p 4000 --no-open -d   # custom port, no browser, background process
```

| Flag | Description |
|---|---|
| `-ef, --examples-folder` | Which examples folder's snapshots to review. |
| `-p, --port` | Dashboard port (default 3000). |
| `--no-open` | Don't auto-open the browser. |
| `-d, --detached` | Run in the background; prints the PID and a `kill` command to stop it. |

#### Reviewing snapshot changes

The dashboard scans the examples folder for `*.received.txt` files (produced by a failing/updated
snapshot test) and lets you approve or reject each one against its `.approved.txt` counterpart.

Navigation is a flat sequential list across every changed file in every test root:

| Key | Action |
|---|---|
| `←` / `→` | Previous / next changed file |
| `↑` / `↓` | Previous / next diff group |
| `a` (hold) / `Shift+A` | Approve (copies received → approved, deletes received) |
| `r` (hold) / `Shift+R` | Reject (deletes received, leaves approved untouched) |
| `v` | Toggle raw view |
| `w` | Toggle whitespace in diff |

Approve/reject requests are guarded against concurrent modification via a `last-modified.txt`
timestamp check — if something else touched the snapshots folder since the page loaded, you get a
409 and a reload prompt instead of silently clobbering it.

Running the dashboard standalone (outside `dh docs review`, e.g. while developing the dashboard
itself) needs `EXAMPLES_DIR` and `PORT` exported manually — see `snapshot-dashboard/README.md`.

### `dh docs gen table-errors`

Generates an AsciiDoc error-code reference table from a YAML source (e.g. the Data API's own
`errors.yaml`).

```bash
dh docs gen table-errors src/main/resources/errors.yaml -o build/docs/table-errors
```

| Flag | Description |
|---|---|
| `INPUT_FILE` (positional) | Input YAML file path or URL. |
| `-o, --output` | Output AsciiDoc file (default `build/docs/table-errors`). |

## Configuration

`dh docs ...` loads `.env.dh.common` then `.env.dh.docs` (see the root
[README](../README.md#configuration)). See [`.env.dh.docs.example`](../.env.dh.docs.example) for
the full list — notably:

```bash
CLIENT_DRIVERS=java,typescript,python
EXAMPLES_FOLDER=./resources/mock_examples
TEST_REPORTER=all_tests
VERIFY_MODE=normal
TYPESCRIPT_ARTIFACT=@datastax/astra-db-ts@v2.0.0
```
