package com.example;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * MCP server demonstrating elicitation capabilities.
 *
 * <h2>Tools provided</h2>
 * <ul>
 *   <li><b>create-profile</b> — Uses form-based elicitation to collect user
 *       profile information (name, age, favorite color) via a structured
 *       JSON Schema form.</li>
 *   <li><b>connect-github</b> — Uses URL-based elicitation to redirect the
 *       user to a GitHub OAuth authorization page, then waits for the
 *       elicitation to complete via a notification.</li>
 * </ul>
 *
 * <h2>How to run</h2>
 * <pre>
 *   cd elicitation-server
 *   mvn package
 *   java -jar target/elicitation-server-1.0-SNAPSHOT.jar
 * </pre>
 *
 * Or configure as an MCP server in your IDE with:
 * <pre>
 *   "command": "java",
 *   "args": ["-jar", "/path/to/elicitation-server-1.0-SNAPSHOT.jar"]
 * </pre>
 */
public class ElicitationServer {

    public static void main(String[] args) {

        var transport = new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        // --- Tool 1: Form-based elicitation ---
        // Collects structured user input via a JSON Schema form
        Tool createProfileTool = Tool.builder()
                .name("create-profile")
                .description("Create a user profile by collecting information via a form")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "greeting", Map.of("type", "string", "description", "Optional greeting to show the user")
                        )
                ))
                .build();

        var createProfileSpec = McpServerFeatures.AsyncToolSpecification.builder()
                .tool(createProfileTool)
                .callHandler((exchange, request) -> {

                    // Build a form elicitation request with a JSON Schema
                    var elicitRequest = McpSchema.ElicitRequest.builder()
                            .message("Please fill in your profile information")
                            .requestedSchema(Map.of(
                                    "type", "object",
                                    "properties", Map.of(
                                            "name", Map.of(
                                                    "type", "string",
                                                    "description", "Your full name",
                                                    "default", "Guest"
                                            ),
                                            "age", Map.of(
                                                    "type", "integer",
                                                    "description", "Your age"
                                            ),
                                            "color", Map.of(
                                                    "type", "string",
                                                    "description", "Your favorite color",
                                                    "enum", List.of("red", "green", "blue", "purple"),
                                                    "default", "blue"
                                            )
                                    ),
                                    "required", List.of("name")
                            ))
                            .build();

                    return exchange.createElicitation(elicitRequest)
                            .map(result -> {
                                if (result.action() == McpSchema.ElicitResult.Action.ACCEPT) {
                                    Map<String, Object> content = result.content();
                                    String name = content != null ? String.valueOf(content.getOrDefault("name", "unknown")) : "unknown";
                                    String age = content != null ? String.valueOf(content.getOrDefault("age", "not provided")) : "not provided";
                                    String color = content != null ? String.valueOf(content.getOrDefault("color", "not provided")) : "not provided";

                                    return CallToolResult.builder()
                                            .addContent(new TextContent(
                                                    "Profile created!\n  Name: " + name + "\n  Age: " + age + "\n  Favorite color: " + color))
                                            .build();
                                }
                                else if (result.action() == McpSchema.ElicitResult.Action.DECLINE) {
                                    return CallToolResult.builder()
                                            .addContent(new TextContent("User declined to provide profile information."))
                                            .build();
                                }
                                else {
                                    return CallToolResult.builder()
                                            .addContent(new TextContent("Profile creation was cancelled."))
                                            .build();
                                }
                            });
                })
                .build();

        // --- Tool 2: URL-based elicitation ---
        // Redirects the user to an external URL (e.g., OAuth flow)
        Tool connectGithubTool = Tool.builder()
                .name("connect-github")
                .description("Connect your GitHub account via OAuth authorization")
                .inputSchema(Map.of(
                        "type", "object",
                        "properties", Map.of(
                                "scopes", Map.of(
                                        "type", "string",
                                        "description", "Comma-separated OAuth scopes to request",
                                        "default", "repo,user"
                                )
                        )
                ))
                .build();

        var connectGithubSpec = McpServerFeatures.AsyncToolSpecification.builder()
                .tool(connectGithubTool)
                .callHandler((exchange, request) -> {

                    String elicitationId = "elicit-" + UUID.randomUUID().toString().substring(0, 8);

                    // Build a URL elicitation request — sends the user to an external page
                    var elicitRequest = McpSchema.ElicitRequest.builder()
                            .mode("url")
                            .message("Please authorize with GitHub to continue")
                            .url("https://github.com/login/oauth/authorize?client_id=EXAMPLE&state=" + elicitationId)
                            .elicitationId(elicitationId)
                            .build();

                    return exchange.createElicitation(elicitRequest)
                            .map(result -> {
                                if (result.action() == McpSchema.ElicitResult.Action.ACCEPT) {
                                    return CallToolResult.builder()
                                            .addContent(new TextContent(
                                                    "GitHub account connected successfully! Elicitation ID: " + elicitationId))
                                            .build();
                                }
                                else {
                                    return CallToolResult.builder()
                                            .addContent(new TextContent(
                                                    "GitHub authorization was declined or cancelled."))
                                            .build();
                                }
                            });
                })
                .build();

        // --- Build the server ---
        var server = McpServer.async(transport)
                .serverInfo("elicitation-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .tools(true)
                        .build())
                .tools(createProfileSpec, connectGithubSpec)
                .build();

        System.err.println("[SERVER] Elicitation demo server started via stdio");
        System.err.println("[SERVER] Tools: create-profile (form elicitation), connect-github (URL elicitation)");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.closeGracefully().block();
        }));
    }
}
