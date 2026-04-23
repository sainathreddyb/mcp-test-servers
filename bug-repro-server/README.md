# MCP Java SDK Completion Handler NPE Bug Repro

Minimal stdio MCP server that reproduces a `NullPointerException` in the MCP Java SDK's completion handler.

## The Bug

When a `Prompt` is registered with `null` arguments and a `completion/complete` request arrives for that prompt, the server crashes with a `NullPointerException` because `McpAsyncServer.completionCompleteRequestHandler` calls `prompt.arguments().stream()` without a null guard.

**Location:** `McpAsyncServer.java`, in the `completionCompleteRequestHandler` lambda — the line that does:

```java
promptSpec.prompt().arguments().stream()
    .filter(arg -> arg.name().equals(argumentName))
    .findFirst()
```

When `prompt.arguments()` returns `null`, calling `.stream()` on it throws `NullPointerException`.

## Build

```bash
cd bug-repro-server
mvn package
```

Requires the MCP Java SDK `2.0.0-SNAPSHOT` installed in your local Maven repo (`~/.m2`).

## Reproduce

Run the server:

```bash
java -jar target/bug-repro-server-1.0-SNAPSHOT.jar
```

Then send these three JSON-RPC messages to stdin (one per line):

**1. Initialize:**
```json
{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{"roots":{"listChanged":true}},"clientInfo":{"name":"test","version":"1.0"}}}
```

**2. Initialized notification:**
```json
{"jsonrpc":"2.0","method":"notifications/initialized"}
```

**3. Completion request (triggers the bug):**
```json
{"jsonrpc":"2.0","id":2,"method":"completion/complete","params":{"ref":{"type":"ref/prompt","name":"summarize"},"argument":{"name":"query","value":"sum"}}}
```

### One-liner test

```bash
echo '{"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{"roots":{"listChanged":true}},"clientInfo":{"name":"test","version":"1.0"}}}
{"jsonrpc":"2.0","method":"notifications/initialized"}
{"jsonrpc":"2.0","id":2,"method":"completion/complete","params":{"ref":{"type":"ref/prompt","name":"summarize"},"argument":{"name":"query","value":"sum"}}}' | java -jar target/bug-repro-server-1.0-SNAPSHOT.jar
```

## Expected

A proper error response or empty completion result.

## Actual

The server returns an internal error with the NPE:

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "error": {
    "code": -32603,
    "message": "Cannot invoke \"java.util.List.stream()\" because the return value of \"io.modelcontextprotocol.spec.McpSchema$Prompt.arguments()\" is null",
    "data": "NullPointerException: Cannot invoke \"java.util.List.stream()\" because the return value of \"io.modelcontextprotocol.spec.McpSchema$Prompt.arguments()\" is null"
  }
}
```

## Fix

The SDK should add a null guard before streaming prompt arguments. Something like:

```java
List<PromptArgument> args = promptSpec.prompt().arguments();
if (args == null || args.stream().noneMatch(arg -> arg.name().equals(argumentName))) {
    logger.warn("Argument not found: {} in prompt: {}", argumentName, promptRef.name());
    return EMPTY_COMPLETION_RESULT;
}
```
