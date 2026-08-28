# Spook

[![Build](https://github.com/burnet01/Spook/actions/workflows/build.yml/badge.svg)](https://github.com/burnet01/Spook/actions/workflows/build.yml)

A lightweight Minecraft hub plugin built for Paper/Spigot-style servers. This project is older and may not be fully compatible with newer server versions or modern plugin APIs. It is provided as-is, without warranty, support guarantees, or liability.

## Features

- Server selector GUI
- Double jump support
- EnderButt-style launch mechanic
- Always-day world setting
- Simple configuration-based setup

## Installation

1. Build or download the plugin jar.
2. Place the jar in your server's `plugins/` folder.
3. Start or reload the server.
4. Adjust the settings in `plugins/Spook/config.yml` as needed.

## Configuration

Example configuration:

```yaml
servers:
  lobby:
    name: "&6&lLobby"
    material: "GRASS_BLOCK"
    lore:
      - "&7Main server lobby"
      - "&aClick to join!"
    command: "server lobby"

  survival:
    name: "&a&lSurvival"
    material: "DIAMOND_PICKAXE"
    lore:
      - "&7Survival gameplay"
      - "&aClick to join!"
    command: "server survival"
```

## Commands

This plugin is primarily item-driven, with actions triggered through configured items or server features.

- Server selector: use the configured selector item
- EnderButt: use the configured ender pearl item
- Double Jump: automatically enabled for players

## Compatibility

- Paper: supported
- Spigot: supported in many setups
- Folia: designed to be broadly compatible with modern async server environments, though it may still need a quick check in your setup
- Minecraft: originally built for older versions and may not work cleanly on newer releases

## Important Note

This project is older code and may not work as expected on modern servers or newer Minecraft versions. The author does not guarantee compatibility, functionality, or ongoing support.

If it works for you, you are free to use, modify, adapt, redistribute, or claim the code as your own. No liability is accepted for any issues, damage, loss, or server problems that may arise from its use.

## Contributors

- @Burnet01
- @SaifSharof (forked from [HubCore](https://github.com/SaifSharof/HubCore))

## License

See the [LICENSE](LICENSE) file for the full terms.

## Acknowledgments

- Forked from [SaifSharof's HubCore](https://github.com/SaifSharof/HubCore)
- Built for a specific Minecraft hub setup
- Older code, but still useful for learning or adaptation

---

Spook
