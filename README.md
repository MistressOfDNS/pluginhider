# PluginHider

PluginHider is a Paper plugin that hides plugin-enumeration commands and command suggestions from regular players.

It is meant for servers that do not want players, tab-completion tools, or client-side scanners to easily discover installed plugins through common Bukkit, Paper, or namespaced command surfaces.

## Features

- Blocks direct use of common plugin-listing commands such as `/plugins`, `/pl`, `/version`, `/ver`, `/about`, and `/icanhasbukkit`.
- Filters command suggestions and tab completions for blocked commands.
- Prunes Paper/Brigadier command trees before they are sent to players.
- Hides namespaced commands by default, while keeping trusted namespaces such as `minecraft` and `brigadier`.
- Hides configured root command aliases that client scanners commonly use to infer plugins.
- Supports a bypass permission for staff or trusted users.
- Uses a simple YAML config.

## Supported Versions

This repository keeps separate branches for each supported server/API version:

| Branch | Paper API |
| --- | --- |
| `1.21.1` | `1.21.1-R0.1-SNAPSHOT` |
| `1.21.5` | `1.21.5-R0.1-SNAPSHOT` |
| `1.21.8` | `1.21.8-R0.1-SNAPSHOT` |
| `1.21.11` | `1.21.11-R0.1-SNAPSHOT` |
| `version/26.1.2` | `26.1.2.build.69-stable` |

Use the branch that matches your server version.

## Installation

1. Download or build the jar for your server version.
2. Place the jar in your server's `plugins/` folder.
3. Start or restart the server.
4. Edit `plugins/PluginHider/config.yml` if needed.
5. Run a full restart after changing command-filtering behavior.

## Configuration

Default config:

```yaml
bypass-permission: ""
blocked-command-message: "&cUnknown command."

# Hidden from completions/suggestions and blocked when manually typed.
# Includes plugin/admin roots that scanner clients may execute directly.
# Keep normal player-facing commands like /home, /warp, /msg, /rtp, and /spawn
# out of this list so players keep command QoL by default.
# remove commands that you want to allow normal users to run
blocked-command-paths:
  - "plugins"
  - "pl"
  - "version"
  - "ver"
  - "about"
  - "icanhasbukkit"
  - "paper plugins"
  - "paper version"
  - "paper ver"
  - "luckperms"
  - "lp"
  - "worldedit"
  - "we"
  - "worldguard"
  - "rg"
  - "grimac"
  - "grim"
  - "vulcan"
  - "matrix"
  - "karhu"
  - "verus"
  - "nocheatplus"
  - "ncp"
  - "spark"
  - "viaversion"
  - "viabackwards"
  - "viarewind"
  - "viaver"
  - "protocollib"
  - "packetevents"
  - "protocolsupport"
  - "coreprotect"
  - "co"
  - "placeholderapi"
  - "papi"
  - "geysermc"
  - "geyser"
  - "floodgate"
  - "litebans"
  - "lb"
  - "advancedban"
  - "ab"
  - "authme"
  - "tab"
  - "tablist"
  - "tabtps"
  - "plan"
  - "vault"

# Hide pluginname:command unless the namespace is allowed.
hide-namespaced-commands: true
allowed-namespaces:
  - "minecraft"
  - "brigadier"

# Optional hide-only roots. Commands already in blocked-command-paths are already
# hidden, so do not duplicate them here.
hidden-root-commands: []
```

### Options

`bypass-permission`

Permission that lets a player bypass PluginHider filtering. Operators always bypass filtering; leave this blank to disable additional permission-based bypasses.

`blocked-command-message`

Message sent when a blocked command is used.

`blocked-command-paths`

Commands or command paths to hide and block for non-bypassed players.

Entries here are hidden from tab completion, Brigadier command suggestions, and command trees. They are also blocked when manually typed. Multi-word paths are token-based: `example test` blocks `/example test` and `/example test anything`, but it does not block `/example test2`.

Use this list for commands that regular players should not be able to execute at all. The default list includes common plugin/admin roots such as `luckperms`, `grimac`, `vulcan`, `spark`, and `viaversion` because scanner clients may execute those commands directly to fingerprint installed plugins.

`hide-namespaced-commands`

When enabled, commands like `pluginname:command` are hidden unless their namespace is allowed.

`allowed-namespaces`

Namespaces that stay visible when namespaced command hiding is enabled. The default allowed namespaces are `minecraft` and `brigadier`.

`hidden-root-commands`

Unnamespaced root commands to hide from command trees and root suggestions for non-bypassed players. This is useful for plugin roots and aliases such as `luckperms`, `grimac`, `lp`, `we`, `spark`, or similar commands that scanners can use as plugin fingerprints.

Commands listed here are not blocked from direct execution. If a player has permission for `/home`, they can still type and run `/home`; it just will not be advertised through root command completions. To deny execution too, add the command or command path to `blocked-command-paths`.

The default list is empty because the plugin/admin scanner roots are already in `blocked-command-paths`, which hides and blocks them. Use `hidden-root-commands` only for commands you want to hide while still allowing manual execution.

Suggestions inside `/help` and `/?` are hidden from non-bypassed players because some client scanners probe `/help a`, `/help b`, and similar prefixes to infer plugin names from any returned command.

### Behavior Examples

| Config entry | Hidden from tab complete? | Blocks manual execution? |
| --- | ---: | ---: |
| `hidden-root-commands: ["debug"]` | yes | no |
| `blocked-command-paths: ["debug"]` | yes | yes |
| `blocked-command-paths: ["example test"]` | hides/blocks `/example test ...` | does not block `/example test2` |

### Existing Configs

Bukkit/Paper plugins usually do not rewrite an existing `plugins/PluginHider/config.yml` after an update. If your server generated an older config, new `blocked-command-paths` entries or the `hidden-root-commands` section may not appear automatically.

Add the new entries manually or regenerate the config if you want the updated defaults. If `hidden-root-commands` is missing entirely, no extra hide-only roots are configured.

## Permissions

| Permission | Description | Default |
| --- | --- | --- |
| `pluginhider.bypass` | Allows a player to bypass PluginHider filtering. | `false` |

Operators always bypass PluginHider filtering. If `bypass-permission` is blank in the config, no extra bypass permission is checked.

## Building

Requirements:

- Gradle
- A JDK matching the target branch

Build the current branch:

```sh
gradle build
```

The compiled jar will be written to:

```text
build/libs/
```

## Development

Switch to the branch for the version you want to work on:

```sh
git switch 1.21.11
```

Then build:

```sh
gradle build
```

When porting to another Paper version, update:

- `version` in `build.gradle.kts`
- the `paper-api` dependency in `build.gradle.kts`
- `api-version` in `src/main/resources/plugin.yml`
- Java toolchain/release if the target server requires a different Java version

## Notes

PluginHider reduces common ways players can inspect installed plugins, but it cannot guarantee complete plugin secrecy. Some plugins may still expose identifying behavior, messages, permissions, commands, packets, or other side channels.
