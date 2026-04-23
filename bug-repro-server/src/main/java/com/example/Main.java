package com.example;

import java.util.List;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import io.modelcontextprotocol.spec.McpSchema.PromptMessage;
import io.modelcontextprotocol.spec.McpSchema.PromptReference;
import reactor.core.publisher.Mono;

/**
 * Minimal stdio MCP server that reproduces a NullPointerException in the
 * MCP Java SDK's completion handler.
 *
 * <h2>The Bug</h2>
 * When a {@link Prompt} is registered with {@code null} arguments and a
 * {@code completion/complete} request arrives for that prompt, the server
 * crashes with a {@link NullPointerException} because
 * {@code McpAsyncServer.completionCompleteRequestHandler} calls
 * {@code prompt.arguments().stream()} without checking for null first.
 *
 * <h2>How to reproduce</h2>
 * <ol>
 *   <li>Build: {@code mvn package}</li>
 *   <li>Run: {@code java -jar target/bug-repro-server-1.0-SNAPSHOT.jar}</li>
 *   <li>Send the initialize request (see README)</li>
 *   <li>Send the initialized notification</li>
 *   <li>Send a completion/complete request for the "summarize" prompt</li>
 * </ol>
 *
 * <h2>Expected</h2>
 * A proper error response or empty completion result.
 *
 * <h2>Actual</h2>
 * The server crashes with:
 * {@code NullPointerException} at {@code McpAsyncServer.java} because
 * {@code prompt.arguments()} is null and {@code .stream()} is called on it.
 */
public class Main {

    public static void main(String[] args) {

        // 1. Create stdio transport using Jackson 2 (auto-discovered via SPI)
        var transport = new StdioServerTransportProvider(McpJsonDefaults.getMapper());

        // 2. Register a prompt with NULL arguments — this is the trigger
        Prompt summarizePrompt = new Prompt(
                "summarize",                          // name
                "Summarize a document",               // description
                (List<PromptArgument>) null            // arguments = null  <-- BUG TRIGGER
        );

        var promptSpec = new McpServerFeatures.AsyncPromptSpecification(
                summarizePrompt,
                (exchange, request) -> Mono.just(new GetPromptResult(
                        "Summarize the given document",
                        List.of(new PromptMessage(
                                McpSchema.Role.USER,
                                new McpSchema.TextContent("Please summarize this document.")
                        ))
                ))
        );

        // 3. Register a completion handler for the "summarize" prompt
        var completionSpec = new McpServerFeatures.AsyncCompletionSpecification(
                new PromptReference("summarize"),      // reference key
                (exchange, request) -> {
                    // This handler never gets called — the NPE happens before it
                    var completion = new CompleteResult.CompleteCompletion(
                            List.of("option1", "option2"),
                            2,
                            false
                    );
                    return Mono.just(new CompleteResult(completion));
                }
        );

        // 4. Build and start the server
        var server = McpServer.async(transport)
                .serverInfo("bug-repro-server", "1.0.0")
                .capabilities(McpSchema.ServerCapabilities.builder()
                        .prompts(true)
                        .completions()
                        .build())
                .prompts(promptSpec)
                .completions(completionSpec)
                .build();

        System.err.println("[SERVER] Bug repro server started via stdio");
        System.err.println("[SERVER] Send a completion/complete request for prompt 'summarize' to trigger the NPE");

        // Keep the process alive
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.closeGracefully().block();
        }));
    }
}
