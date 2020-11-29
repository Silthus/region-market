# RCRegions

[![Build Status](https://github.com/Silthus/region-market/workflows/Build/badge.svg)](../../actions?query=workflow%3ABuild)
[![GitHub release (latest SemVer including pre-releases)](https://img.shields.io/github/v/release/Silthus/region-market?include_prereleases&label=release)](../../releases)
[![codecov](https://codecov.io/gh/Silthus/region-market/branch/master/graph/badge.svg)](https://codecov.io/gh/Silthus/spigot-plugin-template)
[![Commitizen friendly](https://img.shields.io/badge/commitizen-friendly-brightgreen.svg)](http://commitizen.github.io/cz-cli/)
[![semantic-release](https://img.shields.io/badge/%20%20%F0%9F%93%A6%F0%9F%9A%80-semantic--release-e10079.svg)](https://github.com/semantic-release/semantic-release)

RCRegions allows you to easily sell and buy WorldGuard regions with signs and a GUI.

## Getting started

### Installation

This plugin requires the following other plugins:

- [WorldEdit](https://dev.bukkit.org/projects/worldedit)
- [WorldGuard](https://dev.bukkit.org/projects/worldguard)
- [Vault](https://dev.bukkit.org/projects/vault)
- [ProtocolLib](https://dev.bukkit.org/projects/protocollib)
- [ebean-wrapper](https://github.com/Silthus/ebean-wrapper/releases/latest)

After you have installed the required dependencies you can simply drop the [latest release of RCRegions](../../releases/latest) into your plugins folder and restart the server.

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