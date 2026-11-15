# data-api-tools

CLI toolset (`dh`) for testing and managing Data API clients: running documentation example
snippets against a target database, running each client library's own integration test suite,
database/keyspace management, and local Stargate orchestration.

## Setup

```bash
git clone https://github.com/datastax/data-api-tools.git
cd data-api-tools
. scripts/alias.sh
```

Sourcing `scripts/alias.sh` defines a `dh` shell function that builds the fat jar and runs it on
every invocation, and loads shell completions. From then on, just run `dh <command>`.

## Configuration

`dh` loads env vars from dotenv files in the current directory (or `CLI_DIR`):
- `.env.dh.common` — always loaded. Database connection defaults (`ASTRA_TOKEN`, `API_ENDPOINT`),
  external program overrides, etc. See [`.env.dh.common.example`](.env.dh.common.example).
- `.env.dh.<subcommand>` — additionally loaded when that top-level subcommand is being run, e.g.
  `.env.dh.docs` for `dh docs ...`, `.env.dh.clients` for `dh clients ...`. See
  [`.env.dh.docs.example`](.env.dh.docs.example) and
  [`.env.dh.clients.example`](.env.dh.clients.example).

Any value can be overridden per-invocation via the command's own flags (e.g. `-e/--api-endpoint`).

## Commands

| Command | Description |
|---|---|
| `dh docs test` / `run` / `review` / `gen table-errors` | Run or verify documentation example snippets against a target database — see [docs/docs-testing.md](docs/docs-testing.md) |
| `dh clients test` | Run each client library's own integration test suite against a target database — see [docs/client-testing.md](docs/client-testing.md) |
| `dh db bootstrap` / `clean` | Create keyspaces on, or drop collections/tables/UDTs/keyspaces from, a target database |
| `dh startgate` | Spin up or down Stargate (Data API) on DSE or HCD via Docker/Podman Compose |
| `dh logs` | Print the path to the logs directory or the last generated log file |
| `dh compgen` | Generate a shell completions script |

Run `dh <command> --help` (or `dh <command> <subcommand> --help`) for the full flag reference of
any command.
