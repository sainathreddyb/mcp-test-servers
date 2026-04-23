# mcp-test-servers

Collection of test MCP servers for reproducing bugs and experimenting with the MCP protocol.

## Servers

### bug-repro-server

Reproduces a `NullPointerException` in the MCP Java SDK's completion handler when a `Prompt` is registered with `null` arguments.

See [bug-repro-server/README.md](bug-repro-server/README.md) for details.
