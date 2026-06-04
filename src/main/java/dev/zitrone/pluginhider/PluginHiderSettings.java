package dev.zitrone.pluginhider;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public record PluginHiderSettings(
    String bypassPermission,
    Component blockedCommandMessage,
    List<List<String>> blockedCommandPaths,
    boolean hideNamespacedCommands,
    Set<String> allowedNamespaces
) {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public static PluginHiderSettings fromConfig(FileConfiguration config) {
        List<List<String>> blockedPaths = config.getStringList("blocked-command-paths").stream()
            .map(PluginHiderSettings::parsePath)
            .filter(path -> !path.isEmpty())
            .toList();

        Set<String> allowedNamespaces = config.getStringList("allowed-namespaces").stream()
            .map(PluginHiderSettings::normalizeTokenStatic)
            .filter(token -> !token.isEmpty())
            .collect(Collectors.toUnmodifiableSet());

        return new PluginHiderSettings(
            config.getString("bypass-permission", "pluginhider.bypass"),
            LEGACY_SERIALIZER.deserialize(config.getString("blocked-command-message", "&cUnknown command.")),
            blockedPaths,
            config.getBoolean("hide-namespaced-commands", true),
            allowedNamespaces
        );
    }

    public boolean shouldBypass(Player player) {
        return !this.bypassPermission.isBlank() && player.hasPermission(this.bypassPermission);
    }

    public boolean shouldBlockInput(String input) {
        List<String> tokens = parseInput(input);
        if (tokens.isEmpty()) {
            return false;
        }

        return this.blockedCommandPaths.stream().anyMatch(path -> startsWith(tokens, path));
    }

    public boolean shouldHideTopLevelCommand(String command) {
        String normalized = normalizeToken(command);
        if (normalized.isEmpty()) {
            return false;
        }

        if (isHiddenNamespace(command)) {
            return true;
        }

        return this.blockedCommandPaths.stream()
            .filter(path -> path.size() == 1)
            .anyMatch(path -> path.getFirst().equals(normalized));
    }

    public boolean shouldHideSuggestion(String buffer, String suggestion) {
        String normalizedSuggestion = normalizeToken(suggestion);
        if (normalizedSuggestion.isEmpty()) {
            return false;
        }

        if (isHiddenNamespace(suggestion)) {
            return true;
        }

        List<String> tokens = parseInput(buffer);
        if (tokens.isEmpty()) {
            return this.blockedCommandPaths.stream().anyMatch(path -> !path.isEmpty() && path.getFirst().startsWith(normalizedSuggestion));
        }

        List<String> contextTokens = tokens;
        if (!buffer.endsWith(" ")) {
            contextTokens = tokens.subList(0, tokens.size() - 1);
        }

        List<String> finalContextTokens = contextTokens;
        if (this.blockedCommandPaths.stream().anyMatch(path -> startsWith(finalContextTokens, path))) {
            return true;
        }

        int suggestionIndex = finalContextTokens.size();
        return this.blockedCommandPaths.stream().anyMatch(path ->
            path.size() > suggestionIndex
                && startsWith(finalContextTokens, path.subList(0, suggestionIndex))
                && path.get(suggestionIndex).startsWith(normalizedSuggestion)
        );
    }

    public boolean isBlockedPath(List<String> path) {
        return this.blockedCommandPaths.stream().anyMatch(blocked -> blocked.equals(path));
    }

    public String normalizeToken(String token) {
        return normalizeTokenStatic(token);
    }

    public boolean isHiddenNamespace(String literal) {
        if (!this.hideNamespacedCommands) {
            return false;
        }

        int separator = literal.indexOf(':');
        if (separator <= 0) {
            return false;
        }

        String namespace = normalizeTokenStatic(literal.substring(0, separator));
        return !this.allowedNamespaces.contains(namespace);
    }

    private static List<String> parsePath(String rawPath) {
        return List.of(rawPath.trim().split("\\s+")).stream()
            .map(PluginHiderSettings::normalizeTokenStatic)
            .filter(token -> !token.isEmpty())
            .toList();
    }

    private static List<String> parseInput(String input) {
        String cleaned = input.startsWith("/") ? input.substring(1) : input;
        cleaned = cleaned.trim();
        if (cleaned.isEmpty()) {
            return List.of();
        }

        return List.of(cleaned.split("\\s+")).stream()
            .map(PluginHiderSettings::normalizeTokenStatic)
            .filter(token -> !token.isEmpty())
            .toList();
    }

    private static String normalizeTokenStatic(String token) {
        String cleaned = token.trim().toLowerCase(Locale.ROOT);
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        int namespaceSeparator = cleaned.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < cleaned.length()) {
            cleaned = cleaned.substring(namespaceSeparator + 1);
        }

        return cleaned;
    }

    private static boolean startsWith(List<String> input, List<String> prefix) {
        if (prefix.size() > input.size()) {
            return false;
        }

        for (int index = 0; index < prefix.size(); index++) {
            if (!input.get(index).equals(prefix.get(index))) {
                return false;
            }
        }

        return true;
    }
}
