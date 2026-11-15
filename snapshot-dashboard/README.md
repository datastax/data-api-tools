# Snapshot Review Dashboard

A web-based dashboard for reviewing snapshot test results with an intuitive interface and keyboard-driven navigation.

## Features

- **Flat Sequential Navigation**: Navigate through all approved files across all test roots in a linear sequence
- **Arrow Key Navigation**: 
  - `←` `→` Navigate between approved files
  - `↑` `↓` Navigate between diff groups
- **Client-Side Diffing**: Instant diff rendering with toggle options
- **Stable IDs**: Path-based IDs ensure consistent identification across scans
- **Global Staleness Detection**: Automatic detection of external snapshot modifications
- **Keyboard Shortcuts**: Full keyboard control for efficient reviewing

## Prerequisites

- Node.js 18+ and npm
- Snapshots directory with `last-modified.txt` file

## Installation

```bash
cd snapshot-dashboard
npm install
```

## Configuration

### Required Environment Variables

```bash
# Absolute path to your snapshots directory
export EXAMPLES_DIR=/absolute/path/to/snapshots

# Optional: Server port (default: 3000)
export PORT=3000
```

### Example

```bash
export EXAMPLES_DIR=/Users/me/work/data-api-tools/snapshots
export PORT=3000
```

## Usage

### Development Mode

```bash
npm run dev
```

This starts the server with `ts-node` for development.

### Production Mode

```bash
# Build TypeScript
npm run build

# Start server
npm start
```

### Access the Dashboard

Open your browser to: `http://localhost:3000`

## Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `←` | Previous approved file |
| `→` | Next approved file |
| `↑` | Previous diff group |
| `↓` | Next diff group |
| `0-9` | Select diff group by number |
| `a` (hold 0.5s) | Approve current file (all diff groups) - shows green progress spinner |
| `Shift+A` | Approve immediately (bypasses hold-to-confirm) |
| `r` (hold 0.5s) | Reject current file (all diff groups) - shows red progress spinner |
| `Shift+R` | Reject immediately (bypasses hold-to-confirm) |
| `v` | Toggle raw view |
| `w` | Toggle whitespace in diff |
| `F5` / `Cmd+R` / `Ctrl+R` | Refresh page |

**Hold-to-Confirm:** The `a` (approve) and `r` (reject) actions require holding the key for 0.5 seconds. A circular progress spinner appears (green for approve, red for reject) that fills as you hold. Release the key before completion to cancel.

**Note:** Letter shortcuts (a, r, w, v) only work when pressed WITHOUT modifier keys (Ctrl, Cmd, Alt, Shift). This prevents conflicts with native browser shortcuts like `Cmd+R` (refresh) or `Ctrl+W` (close tab).

## How It Works

### Test Root Discovery

The dashboard scans the `EXAMPLES_DIR` directory to find all test roots with changes:

1. Looks for directories containing `*.received.txt` files
2. Groups shared received files by content hash
3. Creates diff groups for identical content
4. Sorts everything (test roots, approved files, languages)

### Sorting Rules

- **Test Roots**: Lexicographic by `rootName`
- **Approved Files**: Shared first, then language-specific alphabetically
- **Languages**: Alphabetically within diff groups

### Navigation Model

All approved files form a flat sequential list:

```
1. delete-many/colls/all/shared.approved.txt
2. delete-many/colls/all/java.approved.txt
3. delete-many/colls/all/python.approved.txt
4. delete-many/colls/filter/shared.approved.txt
...
```

The display shows dual context:
```
Test Root 3 of 12 (Approved File 2 of 3)
delete-many/colls/all — python.approved.txt
```

### Approval/Rejection

- **Approve**: Updates the approved file with received content, deletes all received files
- **Reject**: Deletes all received files without updating approved file
- Both actions affect ALL diff groups for the approved file
- Auto-advances to next approved file after action

### Staleness Detection

The dashboard uses `last-modified.txt` for global staleness detection:

1. Client stores timestamp on initial load
2. Before approve/reject, sends timestamp to server
3. Server validates against current `last-modified.txt`
4. If mismatch: returns 409 Conflict
5. Client shows modal and reloads page

## API Endpoints

### GET /api/test-roots

Returns all test roots with changes and global timestamp.

**Response:**
```json
{
  "testRoots": [...],
  "totalTestRoots": 12,
  "lastModified": "2026-02-02T05:54:06.684Z"
}
```

### POST /api/approve

Approves all diff groups for an approved file.

**Request:**
```json
{
  "approvedFileId": "shared.approved.txt@delete-many/colls/all",
  "lastModified": "2026-02-02T05:54:06.684Z"
}
```

**Success Response (200):**
```json
{
  "success": true,
  "message": "Approved shared.approved.txt affecting 3 languages",
  "lastModified": "2026-02-02T05:55:12.123Z"
}
```

**Stale Data Response (409):**
```json
{
  "error": "STALE_DATA",
  "message": "Snapshots have been modified. Please refresh.",
  "currentLastModified": "2026-02-02T05:55:00.000Z"
}
```

### POST /api/reject

Rejects all diff groups for an approved file.

Same request/response format as approve.

### GET /api/health

Health check endpoint.

**Response:**
```json
{
  "status": "ok",
  "timestamp": "2026-02-02T06:00:00.000Z"
}
```

## Project Structure

```
snapshot-dashboard/
├── src/
│   ├── server.ts              # Express server entry point
│   ├── scanner.ts             # Filesystem scanner
│   ├── types.ts               # TypeScript type definitions
│   └── routes/
│       ├── test-roots.ts      # GET /api/test-roots
│       ├── approve.ts         # POST /api/approve
│       └── reject.ts          # POST /api/reject
├── public/
│   ├── index.html             # Main dashboard UI
│   ├── styles.css             # Styling
│   └── app.js                 # Frontend logic
├── package.json
├── tsconfig.json
└── README.md
```

## Troubleshooting

### Server won't start

**Error: EXAMPLES_DIR environment variable is not set**
- Solution: Set the `EXAMPLES_DIR` environment variable

**Error: EXAMPLES_DIR does not exist**
- Solution: Verify the path exists and is correct

**Error: last-modified.txt not found**
- Solution: Run the docs testing CLI to generate snapshots first

### No snapshots to review

If you see "All Clear! No snapshot changes to review":
- Run your tests to generate `.received.txt` files
- Ensure tests are actually producing different output

### Stale data errors

If you frequently see "Snapshots have changed":
- Another process is modifying snapshots while you review
- Wait for other processes to complete before reviewing

## Development

### Watch Mode

```bash
npm run watch
```

Compiles TypeScript in watch mode.

### Type Checking

```bash
npx tsc --noEmit
```

## License

MIT
