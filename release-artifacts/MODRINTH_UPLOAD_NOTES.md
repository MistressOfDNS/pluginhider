# PluginHider Modrinth Upload Notes

Use one file per supported game/server version.

| File | Suggested Modrinth version name | Game version | Loader |
| --- | --- | --- | --- |
| `PluginHider-1.0-Paper-1.21.1.jar` | `PluginHider 1.0 for 1.21.1` | `1.21.1` | `Paper` |
| `PluginHider-1.0-Paper-1.21.5.jar` | `PluginHider 1.0 for 1.21.5` | `1.21.5` | `Paper` |
| `PluginHider-1.0-Paper-1.21.8.jar` | `PluginHider 1.0 for 1.21.8` | `1.21.8` | `Paper` |
| `PluginHider-1.0-Paper-1.21.11.jar` | `PluginHider 1.0 for 1.21.11` | `1.21.11` | `Paper` |
| `PluginHider-1.0-Paper-26.1.2.jar` | `PluginHider 1.0 for 26.1.2` | `26.1.2` | `Paper` |

Suggested release notes:

```text
PluginHider scanner hardening update for this Paper version.

- Blocks common plugin-listing commands
- Filters tab completion and command suggestions
- Hides namespaced commands unless allowed in config
- Hides common plugin root commands from non-op command trees and root suggestions
- Suppresses /help and /? suggestion probes for non-op players
- Operators always bypass filtering so admin/plugin commands remain usable
- Keeps direct execution available unless a command is listed in blocked-command-paths
```
