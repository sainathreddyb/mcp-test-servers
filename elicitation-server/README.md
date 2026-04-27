# elicitation-server

MCP server demonstrating elicitation capabilities (SEP-1036) from the Java SDK.

## What it does

This server exposes two tools that showcase both elicitation modes:

### `create-profile` — Form-based elicitation
Collects structured user input via a JSON Schema form. The server sends an elicitation request with a schema defining fields (name, age, favorite color), and the client presents a form to the user.

### `connect-github` — URL-based elicitation
Redirects the user to an external URL (simulated GitHub OAuth page). The client opens the URL in a browser, and the server waits for the elicitation to complete.

## Build

Requires the MCP Java SDK 2.0.0-SNAPSHOT to be installed locally:

```bash
# From the java-sdk root
./mvnw clean install -DskipTests

# Then build this server
cd ../mcp-test-servers/elicitation-server
mvn package
```

## Run

```bash
java -jar target/elicitation-server-1.0-SNAPSHOT.jar
```

## MCP configuration

Add to your IDE's MCP config (e.g., `.kiro/settings/mcp.json`):

```json
{
  "mcpServers": {
    "elicitation-demo": {
      "command": "java",
      "args": ["-jar", "/absolute/path/to/elicitation-server-1.0-SNAPSHOT.jar"],
      "disabled": false
    }
  }
}
```
