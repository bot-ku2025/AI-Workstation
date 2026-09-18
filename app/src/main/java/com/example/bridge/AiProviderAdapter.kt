package com.example.bridge

import com.example.data.model.AiProviderInfo
import com.example.data.model.ProviderAvailability

object AiProviderRegistry {

    fun getAllProviders(): List<AiProviderInfo> {
        return listOf(
            AiProviderInfo(
                id = "gemini",
                name = "Gemini",
                description = "Google Gemini multimodal intelligence engine. Native integration inside AI Studio workstation with high-token context window.",
                availability = ProviderAvailability.AVAILABLE,
                supportsYolo = true,
                cliCommand = "gemini --yolo --prompt",
                configKeyOrUrl = "Server-Side Gemini API / Google AI SDK",
                notes = "Supports autonomous execution & full context injection from Shared Checkpoint."
            ),
            AiProviderInfo(
                id = "copilot",
                name = "Copilot",
                description = "GitHub Copilot CLI agent. Operates on local Git repositories with repository-level code understanding.",
                availability = ProviderAvailability.EXTERNAL,
                supportsYolo = true,
                cliCommand = "gh copilot --allow-all-tools",
                configKeyOrUrl = "GitHub Auth / Token via Termux or CLI",
                notes = "Runs externally through Termux CLI bridge or workstation bridge pipe."
            ),
            AiProviderInfo(
                id = "hermes",
                name = "Hermes",
                description = "Hermes Agent CLI. Autonomous tool-calling agent optimized for recursive problem solving and debugging.",
                availability = ProviderAvailability.EXTERNAL,
                supportsYolo = true,
                cliCommand = "hermes-agent -y --project",
                configKeyOrUrl = "Local Hermes Daemon / Socket",
                notes = "Requires active Hermes agent runner in Linux/Termux environment."
            ),
            AiProviderInfo(
                id = "claude",
                name = "Claude",
                description = "Anthropic Claude Code CLI. Deep architectural refactoring, planning, and multi-file reasoning.",
                availability = ProviderAvailability.EXTERNAL,
                supportsYolo = true,
                cliCommand = "claude --dangerously-skip-permissions",
                configKeyOrUrl = "ANTHROPIC_API_KEY via environment",
                notes = "Supported in external terminal bridge; accepts shared context payload."
            ),
            AiProviderInfo(
                id = "openai",
                name = "OpenAI",
                description = "OpenAI GPT-4o / Codex CLI tool. Generalist reasoning and codebase synthesis.",
                availability = ProviderAvailability.EXTERNAL,
                supportsYolo = false,
                cliCommand = "openai api chat.completions.create",
                configKeyOrUrl = "OPENAI_API_KEY via environment",
                notes = "YOLO auto-approval requires custom shell wrapper script."
            )
        )
    }

    fun getProviderById(id: String): AiProviderInfo {
        return getAllProviders().find { it.id.equals(id, ignoreCase = true) }
            ?: AiProviderInfo(
                id = id.lowercase(),
                name = id,
                description = "Custom external AI agent adapter",
                availability = ProviderAvailability.EXTERNAL,
                supportsYolo = false,
                cliCommand = "$id --exec",
                configKeyOrUrl = "Custom configuration",
                notes = "Custom adapter configured for this project."
            )
    }
}
