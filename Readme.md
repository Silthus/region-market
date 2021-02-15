# RCRegions

[![Build Status](https://github.com/Silthus/region-market/workflows/Build/badge.svg)](../../actions?query=workflow%3ABuild)
[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/Silthus/region-market?include_prereleases&label=release)](../../releases)
[![codecov](https://codecov.io/gh/Silthus/region-market/branch/master/graph/badge.svg)](https://codecov.io/gh/Silthus/spigot-plugin-template)
[![Commitizen friendly](https://img.shields.io/badge/commitizen-friendly-brightgreen.svg)](http://commitizen.github.io/cz-cli/)
[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg)](https://github.com/semantic-release/semantic-release)

RCRegions ist das Grundstücksplugin des [Raid-Craft Servers](https://raid-craft.de). Es ermöglicht den intuitiven An- und Verkauf von WorldGuard Regionen über Schilder und eine Chat GUI.

* [Getting started](#getting-started)
  * [Installation](#installation)
* [Configuration](#configuration)
  * [config.yml](#configyml)
  * [limits.yml](#limitsyml)
  * [groups.yml](#groupsyml)
* [Usage](#usage)
  * [Creating Regions](#creating-regions)
  * [Region Price Calculation](#region-price-calculation)
* [Achievement](#achievement)

## Getting started

### Installation

Folgende Plugins werden benötigt, damit RCRegions funktioniert.

* [WorldEdit](https://dev.bukkit.org/projects/worldedit) - *Zum Speichern der Schematics*
* [WorldGuard](https://dev.bukkit.org/projects/worldguard) - *Es werden WorldGuard Regionen verkauft...*
* [Vault](https://dev.bukkit.org/projects/vault) - *Für die Geld Transaktionen*
* [ebean-wrapper](https://github.com/Silthus/ebean-wrapper/releases/latest) - *Für die Datenbank Verbindung*

Wenn alle Dependencies installiert sind die [aktuellste Version herunterladen](../../releases/latest) und analog dem [RC-Install-Guide](https://github.com/raidcraft/minecraft-server/blob/main/README.md#plugins-installieren) installieren.

## Configuration

RCRegions hat drei Configs die angepasst werden müssen:

* `config.yml` - *Generelle Einstellungen, Datenbank Verbindung, Task Delays*
* `limits.yml` - *Erstellung von mehreren Grundstückslimits die dann per Permission zugewiesen werden*
* `groups.yml` - *Erstellung von Grundstücksgruppen (Stadtteilen) und Konfiguration der Kosten*

### config.yml

```yml
# The relative path or config file where your skill region groups are located.
region_groups_config: groups.yml
# The name of the limits config file
limits_config: limits.yml
# The location where schematics of regions should be stored.
schematics: schematics
# The time in ticks how long a player has to confirm the buying of a region.
buy_time_ticks: 600
# Set to true to automatically set the parent WorldGuard region defined in the group config.
# You can always manually update the parent regions with the command /rcra wgparents <group>
autoset_world_guard_parent: true
# Set to true to automatically map groups based on existing WorldGuard parents.
# You can always manually trigger this with the command /rcra autogroup <group>
auto_map_parent: true
# Display all open sales to a player after login in
display_sales_login_notification: true
# The delay in ticks on how long to wait until sales are displayed after logging in
sales_login_delay: 60
# a list of worldguard regions that should be ignored by rcregions
# this is useful when using the autolink mode
ignored_regions:
- rccity
- rcmap
- slums
# commands that are executed when a region is sold
# you can use the following placeholders:
#   - %player% ~> Silthus
#   - %region% ~> test1
#   - %group%  ~> default
#   - %world%  ~> world
sell_commands:
- 'lwc admin purgeregion %region% %world%'
# the connection details for the database
# see the ebean-wrapper documentation for details
database:
  username: ${CFG_DB_USER}
  password: ${CFG_DB_PASSWORD}
  driver: ${CFG_DB_DRIVER}
  url: jdbc:mariadb://${CFG_DB_HOST}:${CFG_DB_PORT}/${CFG_DB_NAME}
```

### limits.yml

```yml
# The default limit is automatically applied to all players regardless of their permissions.
# Set the option to a blank string to disable the default limit.
default_limit: default
# Define your limits in this config.
# Each limit has a unique key that can be assigned to players by giving them the rcregions.limits.<your-limit> permission.
# You can overwrite specific limits per player with the following permissions:
#    - rcregions.player-limits.total.<limit>
#    - rcregions.player-limits.regions.<group>.<limit>
#    - rcregions.player-limits.groups.<limit>
limits:
  # the identifier of the limit that is used in the permission
  # permission: rcregions.limits.default
  default:
    # the total number of regions a player is allowed to own
    # -1 means infinite
    total: -1
    # list your groups here and their limits
    # use -1 for infinite regions
    # the total limit always overrides this
    group_regions:
      slums: 1
      zentrum: 2
      default: 0
      hills: 1
    # the number of groups where a player can own regions in
    groups: -1
    # higher priority limits override each other
    # this only applies when the player has multiple permissions
    priority: 1
```

### groups.yml

```yml
# The default group that for all new regions
# You can set the region limit of the default group to 0 in the limits.yml to avoid players claiming default regions
default-group: default
groups:
  ############################################ä
  # DO NOT REMOVE OR RENAME THE DEFAULT GROUP #
  ############################################ä
  # 'default' is the key of the group.
  # The key is used in commands to assign regions to a group.
  default:
    # The player friendly display name of this group.
    name: Default
    # A short and precise description what regions this group contains.
    description: The default region group contains all regions that have no group assigned to them.
    # The name of the world where this group is active
    world: world
    # The name of the worldguard region that should be used for automapping
    # and setting the parent region on creation
    worldguard-region: default
    # The value in percent how much a player gets back when selling the region
    # the player always only gets back the base price of the region
    # this is to avoid money duping exploits
    sell-modifier: 1.0
    # The default type of the regions that are created in this group
    # available price types:
    #   - dynamic: the price is automatically calculated based on the size of the region
    #   - static: a static price is used
    #   - free: all regions in this group are free by default
    price-type: dynamic
    # Here you can specify a list of costs that apply to all regions in this group.
    # Currently there is only the 'money' cost available, but more will follow.
    costs:
      # The type key of the cost.
      money:
        # How should the base_price of the region be calculated?
        #   - per2m: base_price = base * m2
        #   - per3m: base_price = base * m3
        #   - static: base_price = base
        type: per2m
        # The price per m2, m3 or a static price depending on the type of the region
        base: 10.0
        # the calculated base price is then increased depending on the number of regions the player has
        # and the individual modifier of the player and region
        # region_price = SUMΣ[base_price * ((count ^ power) * multiplier) - base_price] * player_permission_multiplier * region_multiplier
        # ###################################
        # Use the values below to fine tune the calculation for this group
        #
        # multipliers for the total count of player regions
        region-count-multiplier: 0.0
        region-count-multiplier-power: 1.0
        # multiplier for the total count of groups where a player owns regions
        region-group-count-multiplier: 0.0
        region-group-count-multiplier-power: 1.0
        # the count of regions inside this group
        same-group-count-multiplier: 0.0
        same-group-count-multiplier-power: 1.0
```

## Usage

All RCRegion commands start with `/rcregions` or `/rcr` as a shorthand. Admin commands can be accessed with `/rcregions:admin` or `/rcra`.
Use the smart tab completion and help command (`/rcregions help`) to find our more about each command.

### Creating Regions

Every region in RCRegions requires a valid WorldGuard region as the container of the region and to manage permissions and ownership.

### Region Price Calculation

You can specify a price modifier for groups that gets applied for players that want to buy multiple regions. The following logic is used when calculating the price for a region.

```
region_price = SUM(base_price * [(count ^ power) * multiplier + 1] - base_price) * player_permission_multiplier * region_multiplier
```

## Achievement

The RCRegions plugin comes with a built-in achievement type for [RCAchievements](https://github.com/raidcraft/rcachievements).

Use the `region` type with the following achievement configuration:

| Config | Default | Description |
| ------ | ------- | ----------- |
| `count` | `0` | How many regions must the player own. Ignored if `0`. |
| `groups` | `[]` | A list region groups the player must have a region in. |
| `regions` | `[]` | A list of regions the player must own. |
| `money_progress` | `false` | Show how much money the player needs until he can buy a region in the list. |
