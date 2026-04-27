# mcp-test-servers

Collection of test MCP servers for reproducing bugs and experimenting with the MCP protocol.

## Servers

### bug-repro-server

Reproduces a `NullPointerException` in the MCP Java SDK's completion handler when a `Prompt` is registered with `null` arguments.

See [bug-repro-server/README.md](bug-repro-server/README.md) for details.

### elicitation-server

Demonstrates MCP elicitation capabilities (SEP-1036) with two tools:
- **create-profile** — Form-based elicitation: collects structured user input via a JSON Schema form
- **connect-github** — URL-based elicitation: redirects the user to an external OAuth page

See [elicitation-server/README.md](elicitation-server/README.md) for details.
