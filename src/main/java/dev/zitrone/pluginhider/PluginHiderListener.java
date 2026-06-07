package dev.zitrone.pluginhider;

import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendCommandsEvent;
import com.destroystokyo.paper.event.brigadier.AsyncPlayerSendSuggestionsEvent;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.server.TabCompleteEvent;

public final class PluginHiderListener implements Listener {
    private static final Method REMOVE_COMMAND_METHOD = findRemoveCommandMethod();
    private static final Field CHILDREN_FIELD = findField("children");
    private static final Field LITERALS_FIELD = findField("literals");
    private static final Field ARGUMENTS_FIELD = findField("arguments");

    private final PluginHiderSettings settings;

    public PluginHiderListener(PluginHiderSettings settings) {
        this.settings = settings;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (this.settings.shouldBypass(player) || !this.settings.shouldBlockInput(event.getMessage())) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage(this.settings.blockedCommandMessage());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        if (this.settings.shouldBypass(event.getPlayer())) {
            return;
        }

        event.getCommands().removeIf(this.settings::shouldHideTopLevelCommand);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerSendCommands(AsyncPlayerSendCommandsEvent<?> event) {
        if (this.settings.shouldBypass(event.getPlayer())) {
            return;
        }

        if (!(event.isAsynchronous() || !event.hasFiredAsync())) {
            return;
        }

        pruneNode(event.getCommandNode(), List.of());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncPlayerSendSuggestions(AsyncPlayerSendSuggestionsEvent event) {
        if (this.settings.shouldBypass(event.getPlayer())) {
            return;
        }

        Suggestions filtered = filterSuggestions(event.getBuffer(), event.getSuggestions());
        event.setSuggestions(filtered);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        if (!(event.getSender() instanceof Player player) || this.settings.shouldBypass(player) || !event.isCommand()) {
            return;
        }

        event.getCompletions().removeIf(completion -> this.settings.shouldHideSuggestion(event.getBuffer(), completion));
    }

    private void pruneNode(CommandNode<?> node, List<String> path) {
        Collection<CommandNode<?>> snapshot = new ArrayList<>(node.getChildren());
        for (CommandNode<?> child : snapshot) {
            String childName = child.getName();
            List<String> childPath = new ArrayList<>(path);
            childPath.add(this.settings.normalizeToken(childName));

            if ((path.isEmpty() && this.settings.shouldHideTopLevelCommand(childName))
                || this.settings.isHiddenNamespace(childName)
                || this.settings.isBlockedPath(childPath)) {
                removeCommand(node, childName);
                continue;
            }

            pruneNode(child, childPath);
        }
    }

    private Suggestions filterSuggestions(String buffer, Suggestions suggestions) {
        List<Suggestion> kept = suggestions.getList().stream()
            .filter(suggestion -> !this.settings.shouldHideSuggestion(buffer, suggestion.getText()))
            .toList();

        if (kept.size() == suggestions.getList().size()) {
            return suggestions;
        }

        return new Suggestions(suggestions.getRange(), kept);
    }

    @SuppressWarnings("unchecked")
    private static void removeCommand(CommandNode<?> node, String commandName) {
        try {
            if (REMOVE_COMMAND_METHOD != null) {
                REMOVE_COMMAND_METHOD.invoke(node, commandName);
                return;
            }

            CommandNode<?> child = node.getChild(commandName);
            if (child == null) {
                return;
            }

            ((Map<String, CommandNode<?>>) CHILDREN_FIELD.get(node)).remove(commandName, child);
            ((Map<String, CommandNode<?>>) LITERALS_FIELD.get(node)).remove(commandName, child);
            ((Map<String, CommandNode<?>>) ARGUMENTS_FIELD.get(node)).remove(commandName, child);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to remove brigadier command node " + commandName, exception);
        }
    }

    private static Method findRemoveCommandMethod() {
        try {
            Method method = CommandNode.class.getDeclaredMethod("removeCommand", String.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Field findField(String name) {
        try {
            Field field = CommandNode.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access brigadier field " + name, exception);
        }
    }
}
