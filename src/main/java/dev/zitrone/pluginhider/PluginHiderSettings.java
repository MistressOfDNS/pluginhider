package dev.zitrone.pluginhider;

import java.util.HashSet;
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
    Set<String> allowedNamespaces,
    Set<String> hiddenRootCommands
) {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final Set<String> DEFAULT_HIDDEN_ROOT_COMMANDS = Set.of(
        "essentials", "essentialsx", "worldedit", "worldguard", "luckperms", "vault",
        "citizens", "cmi", "cmilib", "multiverse-core", "multiverse", "viaversion",
        "viabackwards", "viarewind", "geysermc", "geyser", "floodgate", "protocollib",
        "coreprotect", "griefprevention", "shopkeepers", "dynmap", "placeholderapi",
        "skinsrestorer", "skript", "advancedanticheat", "vulcan", "grimac", "matrix",
        "spartan", "aac", "karhu", "verus", "nocheatplus", "authme", "deluxemenus",
        "plotsquared", "supervanish", "packetevents", "oraxen", "itemsadder",
        "fawe", "fastasyncworldedit", "luckpermsbukkit", "essentialsgeoip",
        "essentialsprotect", "essentialsspawn", "essentialsxspawn",
        "multiverse-inventories", "multiverse-netherportals", "worldborder",
        "votifier", "nuvotifier", "votingplugin", "excellentcrates", "crazycrates",
        "cratekeys", "jobs", "jobsreborn", "mcmmo", "towny", "factions",
        "factionsuuid", "lands", "residence", "claimchunk", "quickshop",
        "quickshop-hikari", "chestshop", "shopgui", "auctionhouse", "combatlogx",
        "litebans", "advancedban", "libertybans", "luckpermsgui", "tab", "tablist",
        "scoreboard", "animatedscoreboard", "ajleaderboards", "ajqueue", "spark",
        "sparkbukkit", "plan", "minimotd", "protocolsupport", "excellentenchants",
        "eco", "ecoenchants", "mythicmobs", "mythiclib", "modelengine", "mmoitems",
        "mmocore", "denizen", "citizenscmd", "sentinel", "npcs", "vulcanbungee",
        "grimacbukkit", "negativity", "intave", "polar", "horizon", "themis",
        "libreforge", "autotreechop", "axgraves", "curios", "freedomchat",
        "minertrack", "probablybackpacks", "tabtps", "veinminer", "lp", "we", "rg",
        "mv", "npc", "papi", "co", "grim", "viaver", "sr", "dm", "plots", "sv",
        "spawn", "home", "homes", "warp", "warps", "tpa", "tpahere", "bal",
        "balance", "money", "ban", "kick", "mute", "jail", "seen", "ptime",
        "pweather", "tppos", "near", "back", "afk", "msg", "r", "reply", "mail",
        "pay", "sell", "worth", "kit", "kits", "lb", "ab", "vote", "votes", "f",
        "res", "qs", "ah", "mm", "ncp"
    );

    public static PluginHiderSettings fromConfig(FileConfiguration config) {
        List<List<String>> blockedPaths = config.getStringList("blocked-command-paths").stream()
            .map(PluginHiderSettings::parsePath)
            .filter(path -> !path.isEmpty())
            .toList();

        Set<String> allowedNamespaces = config.getStringList("allowed-namespaces").stream()
            .map(PluginHiderSettings::normalizeTokenStatic)
            .filter(token -> !token.isEmpty())
            .collect(Collectors.toUnmodifiableSet());

        Set<String> hiddenRootCommands = new HashSet<>(DEFAULT_HIDDEN_ROOT_COMMANDS);
        hiddenRootCommands.addAll(config.getStringList("hidden-root-commands").stream()
            .map(PluginHiderSettings::normalizeTokenStatic)
            .filter(token -> !token.isEmpty())
            .collect(Collectors.toSet()));

        return new PluginHiderSettings(
            config.getString("bypass-permission", "pluginhider.bypass"),
            LEGACY_SERIALIZER.deserialize(config.getString("blocked-command-message", "&cUnknown command.")),
            blockedPaths,
            config.getBoolean("hide-namespaced-commands", true),
            allowedNamespaces,
            Set.copyOf(hiddenRootCommands)
        );
    }

    public boolean shouldBypass(Player player) {
        return player.isOp() || (!this.bypassPermission.isBlank() && player.hasPermission(this.bypassPermission));
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

        if (this.hiddenRootCommands.contains(normalized)) {
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

        if (isHiddenNamespaceContext(buffer)) {
            return true;
        }

        List<String> tokens = parseInput(buffer);
        if (tokens.isEmpty()) {
            return shouldHideRootSuggestion(normalizedSuggestion)
                || this.blockedCommandPaths.stream().anyMatch(path -> !path.isEmpty() && path.getFirst().startsWith(normalizedSuggestion));
        }

        List<String> contextTokens = tokens;
        if (!buffer.endsWith(" ")) {
            contextTokens = tokens.subList(0, tokens.size() - 1);
        }

        if (isRootSuggestionContext(contextTokens) && shouldHideRootSuggestion(normalizedSuggestion)) {
            return true;
        }

        if (isHelpSuggestionContext(contextTokens) && shouldHideHelpSuggestion(contextTokens, normalizedSuggestion)) {
            return true;
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

    private boolean isHiddenNamespaceContext(String buffer) {
        if (!this.hideNamespacedCommands || buffer == null) {
            return false;
        }

        String cleaned = buffer.trim();
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }

        int spaceIndex = cleaned.indexOf(' ');
        String firstToken = spaceIndex >= 0 ? cleaned.substring(0, spaceIndex) : cleaned;
        return isHiddenNamespace(firstToken);
    }

    private boolean shouldHideRootSuggestion(String normalizedSuggestion) {
        return this.hiddenRootCommands.contains(normalizedSuggestion)
            || this.blockedCommandPaths.stream()
                .filter(path -> path.size() == 1)
                .anyMatch(path -> path.getFirst().equals(normalizedSuggestion));
    }

    private boolean isRootSuggestionContext(List<String> contextTokens) {
        return contextTokens.isEmpty();
    }

    private boolean isHelpSuggestionContext(List<String> contextTokens) {
        if (contextTokens.isEmpty()) {
            return false;
        }

        String root = contextTokens.getFirst();
        return "help".equals(root) || "?".equals(root);
    }

    private boolean shouldHideHelpSuggestion(List<String> contextTokens, String normalizedSuggestion) {
        return true;
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
