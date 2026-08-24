import { createMcpServer } from "copilot-sdk/mcp";
import { joinSession } from "copilot-sdk/extension";

await joinSession({
    mcpServers: [
        createMcpServer({
            id: "firebase",
            command: "firebase",
            args: ["experimental:mcp", "--only", "crashlytics"],
        }),
    ],
});
