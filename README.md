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

hide-namespaced-commands: true
allowed-namespaces:
  - "minecraft"
  - "brigadier"

hidden-root-commands:
  - "lp"
  - "we"
  - "rg"
  - "mv"
  - "npc"
  - "papi"
  - "home"
  - "warp"
  - "msg"
  - "spark"
```

### Options

`bypass-permission`

Permission that lets a player bypass PluginHider filtering. Operators always bypass filtering; leave this blank to disable additional permission-based bypasses.

`blocked-command-message`

Message sent when a blocked command is used.

`blocked-command-paths`

Commands or command paths to hide and block. Multi-word paths such as `paper plugins` are supported.

`hide-namespaced-commands`

When enabled, commands like `pluginname:command` are hidden unless their namespace is allowed.

`allowed-namespaces`

Namespaces that stay visible when namespaced command hiding is enabled.

`hidden-root-commands`

Unnamespaced root commands to hide from command trees and root suggestions. This is useful for plugin roots and aliases such as `luckperms`, `grimac`, `lp`, `we`, `home`, `spark`, or similar commands that scanners can use as plugin fingerprints. Commands listed here are hidden from completion surfaces, but they are not blocked from direct execution unless they are also listed in `blocked-command-paths`.

Suggestions inside `/help` and `/?` are hidden from non-bypassed players because some client scanners probe `/help a`, `/help b`, and similar prefixes to infer plugin names from any returned command.

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
