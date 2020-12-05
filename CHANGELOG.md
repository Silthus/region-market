## [1.17.1](https://github.com/raidcraft/rcregions/compare/v1.17.0...v1.17.1) (2020-12-05)


### Bug Fixes

* **sell:** do not reset region multiplier when selling to players ([9a6e5bf](https://github.com/raidcraft/rcregions/commit/9a6e5bf9823729cdf2dd103c61e34557a3905932)), closes [#22](https://github.com/raidcraft/rcregions/issues/22)

# [1.17.0](https://github.com/raidcraft/rcregions/compare/v1.16.0...v1.17.0) (2020-12-05)


### Bug Fixes

* **sales:** make region occupied when a sale expires ([a54d223](https://github.com/raidcraft/rcregions/commit/a54d223cd4511890643581e8f947816000112c13))


### Features

* **cmd:** add /rcr list command that lists all player regions ([0134a80](https://github.com/raidcraft/rcregions/commit/0134a80d5fef939b5886c814d4559b0c6f27c903))
* **cmd:** add /sales command that shows all player sales ([3c3ab85](https://github.com/raidcraft/rcregions/commit/3c3ab858ceb79cc47ebc469282428628466936c0))
* add task that expires regions on sale ([a1f5bbb](https://github.com/raidcraft/rcregions/commit/a1f5bbb95f9a1d861292ce818b0004dc3ec16eee))

# [1.16.0](https://github.com/raidcraft/rcregions/compare/v1.15.0...v1.16.0) (2020-12-05)


### Features

* add option to execute commands when a region is sold ([24136b3](https://github.com/raidcraft/rcregions/commit/24136b35a44da0c2630a367cca46c1a8d7e58519))
* auto expire selling regions after some time ([04e9c21](https://github.com/raidcraft/rcregions/commit/04e9c214f45f5940535804e07a6059db742bec98))

# [1.15.0](https://github.com/raidcraft/rcregions/compare/v1.14.0...v1.15.0) (2020-12-05)


### Features

* add option to sell regions directly to players ([b1249b9](https://github.com/raidcraft/rcregions/commit/b1249b9b0c0ea091cec7da1931f5952156340b38))
* show active sales and sold regions on login ([427bc1a](https://github.com/raidcraft/rcregions/commit/427bc1a0c141b811da9c99b9b7f9a66212c2036d))

# [1.14.0](https://github.com/raidcraft/rcregions/compare/v1.13.1...v1.14.0) (2020-12-04)


### Features

* **cmd:** add /rcra set factor command to set the individual region factor ([9b167d0](https://github.com/raidcraft/rcregions/commit/9b167d053933881e5ba5907dd3f11834b542f66a))
* save schematic when a player buys a region ([61e1068](https://github.com/raidcraft/rcregions/commit/61e106803e425330ea7d5bfdb5911b1e3928f34d))

## [1.13.1](https://github.com/raidcraft/rcregions/compare/v1.13.0...v1.13.1) (2020-12-04)


### Bug Fixes

* set base price to 0 when switching to dynamic price ([6c36d48](https://github.com/raidcraft/rcregions/commit/6c36d48c282884028980eeb45278ce4bf7915f15))

# [1.13.0](https://github.com/raidcraft/rcregions/compare/v1.12.1...v1.13.0) (2020-12-03)


### Bug Fixes

* **cmd:** make 0 costs regions free ([8302500](https://github.com/raidcraft/rcregions/commit/8302500d0b7afc55c463811750e212bd6fba1e1f))


### Features

* add option to sell regions to the server ([8f6ed0d](https://github.com/raidcraft/rcregions/commit/8f6ed0d96477f892d4f3840a3e240a37bd5f8756))
* **cmd:** add /rcr limits command ([170a1cb](https://github.com/raidcraft/rcregions/commit/170a1cb2e774df852ba0dbae8059110e0ed46cc5))
* autoset default group if region group is empty ([000df9f](https://github.com/raidcraft/rcregions/commit/000df9f2a64c010ff17188f617b74ac86feb6873))
* **cmd:** add option to specify region group on creation ([5c86c72](https://github.com/raidcraft/rcregions/commit/5c86c7211801435a9ce0dd8e79ac13925b2eb73b))

## [1.12.1](https://github.com/raidcraft/rcregions/compare/v1.12.0...v1.12.1) (2020-12-03)


### Bug Fixes

* cache costs loading ([82b5036](https://github.com/raidcraft/rcregions/commit/82b50367f19bd466e080c062398dc16b1b16f709))

# [1.12.0](https://github.com/raidcraft/rcregions/compare/v1.11.2...v1.12.0) (2020-12-02)


### Bug Fixes

* allow null groups ([69cf71f](https://github.com/raidcraft/rcregions/commit/69cf71fa6679d67a5db46ffa16d4f62390b5ca1f))
* **api:** add static handlerlist to base events ([533324f](https://github.com/raidcraft/rcregions/commit/533324fa26d8de66bda22916417233ab852d3ed8))


### Features

* implement region groups price modifier and limits ([8997efa](https://github.com/raidcraft/rcregions/commit/8997efa6114e2538fc2f07234ab8c9a440ba523f))
* **cmd:** add basic /rcr sell dialog ([acd9f8b](https://github.com/raidcraft/rcregions/commit/acd9f8b1481a67696296861bcdefb33356afcdb7))

## [1.11.2](https://github.com/raidcraft/rcregions/compare/v1.11.1...v1.11.2) (2020-12-02)


### Bug Fixes

* **api:** add static handlerlist to base events ([4d8985d](https://github.com/raidcraft/rcregions/commit/4d8985d290e57128a95b4a7d1c57062f35774315))

## [1.11.1](https://github.com/raidcraft/rcregions/compare/v1.11.0...v1.11.1) (2020-12-02)


### Bug Fixes

* **db:** fix pending drops migration of owner_id on regions table ([2521384](https://github.com/raidcraft/rcregions/commit/25213842064759e6238eb2335fb146019b21f642))

# [1.11.0](https://github.com/raidcraft/rcregions/compare/v1.10.1...v1.11.0) (2020-12-02)


### Features

* add saving of created and deleted regions as schematics ([c40c795](https://github.com/raidcraft/rcregions/commit/c40c79591c4628f566e3cc64bfca5c44ad446a4c))
* **api:** add events for buy and create region ([8824bfe](https://github.com/raidcraft/rcregions/commit/8824bfe4117258cd5f5b5991da25f1b960e36cda))

## [1.10.1](https://github.com/raidcraft/rcregions/compare/v1.10.0...v1.10.1) (2020-12-02)


### Bug Fixes

* **db:** add missing group world and wg region migrations ([fc909f3](https://github.com/raidcraft/rcregions/commit/fc909f37f24b4417c5dc3f903b96bfd8d047e66b))

# [1.10.0](https://github.com/raidcraft/rcregions/compare/v1.9.0...v1.10.0) (2020-12-02)


### Features

* **cmd:** add option to auto set worldguard parent or group ([b102071](https://github.com/raidcraft/rcregions/commit/b10207129d20839462644a9df4a1edaa21bd7d3d))

# [1.9.0](https://github.com/raidcraft/rcregions/compare/v1.8.1...v1.9.0) (2020-12-02)


### Bug Fixes

* rougly calculate polygon region ([20d16aa](https://github.com/raidcraft/rcregions/commit/20d16aa887f7715b271df5d975be9286257d2a42))
* **info:** npe in player hover if player lastonline is null ([8f71b73](https://github.com/raidcraft/rcregions/commit/8f71b73aa5c782a61a2fc67d13f28ffc9ec5ca9f))


### Features

* **cmd:** add command to set price and size ([df1a1cc](https://github.com/raidcraft/rcregions/commit/df1a1cc6a660589f024a29e56f04fbb84124cff3))

## [1.8.1](https://github.com/raidcraft/rcregions/compare/v1.8.0...v1.8.1) (2020-12-01)


### Bug Fixes

* **info:** fix region costs info message ([5edeadd](https://github.com/raidcraft/rcregions/commit/5edeadd4a1a93003300f9ce4e4dc6eb256609881))

# [1.8.0](https://github.com/raidcraft/rcregions/compare/v1.7.1...v1.8.0) (2020-12-01)


### Features

* **cmd:** add /rcra set parent command ([98300e3](https://github.com/raidcraft/rcregions/commit/98300e345556a47e8139a031b59c52c0668a4050))

## [1.7.1](https://github.com/raidcraft/rcregions/compare/v1.7.0...v1.7.1) (2020-12-01)


### Bug Fixes

* show cost breakdown hover ([cdfd2f1](https://github.com/raidcraft/rcregions/commit/cdfd2f156de5f0e85b89128dff0020356eaa1fa7))

# [1.7.0](https://github.com/raidcraft/rcregions/compare/v1.6.0...v1.7.0) (2020-12-01)


### Bug Fixes

* time formatting error in region info ([f249618](https://github.com/raidcraft/rcregions/commit/f2496186b2073f3d576a3a3307b09768a49a8ef3))


### Features

* add detailed cost breakdown view ([75b1c94](https://github.com/raidcraft/rcregions/commit/75b1c940ef04ae19849dafb453eebb113e9a9f0d))

# [1.6.0](https://github.com/raidcraft/rcregions/compare/v1.5.1...v1.6.0) (2020-12-01)


### Bug Fixes

* **cmd:** correctly display abort buy cmd ([a5e9720](https://github.com/raidcraft/rcregions/commit/a5e9720cb966d56f834d8ee186e2450b77445fed))


### Features

* **cmd:** add /rcr info command and the option to show worldguard info on click ([fb7aaa7](https://github.com/raidcraft/rcregions/commit/fb7aaa7ba5a2252ffbc827f936cc7b7207f5fae7))
* **gui:** display player limits in region overview ([da8dd16](https://github.com/raidcraft/rcregions/commit/da8dd16191034d87fdf33f3d9ee2aafdb1136d81))

## [1.5.1](https://github.com/raidcraft/rcregions/compare/v1.5.0...v1.5.1) (2020-12-01)


### Bug Fixes

* player regions calculation based off actual regions ([332fc22](https://github.com/raidcraft/rcregions/commit/332fc22b8430de00a5886a2c9d7e8317e1aa29a5))
* **db:** remove unused player_name field from owned_region ([f84d167](https://github.com/raidcraft/rcregions/commit/f84d16736cb33253266f28c4a8ad684ed6782271))

# [1.5.0](https://github.com/raidcraft/rcregions/compare/v1.4.1...v1.5.0) (2020-12-01)


### Bug Fixes

* correctly display playername on sign and info text ([ebd2d5e](https://github.com/raidcraft/rcregions/commit/ebd2d5ee12777cd0bdc71d2f851093ec745cd1ad))


### Features

* add nice formatting and improve overall user experience ([8181fd7](https://github.com/raidcraft/rcregions/commit/8181fd712f23894054b3c1c1290987926f924860))
* add pretty formatting for regions ([88b8386](https://github.com/raidcraft/rcregions/commit/88b8386571b950c0742b01a9a71b01211d4a518f))

## [1.4.1](https://github.com/raidcraft/rcregions/compare/v1.4.0...v1.4.1) (2020-11-30)


### Bug Fixes

* **region:** volume is now correctly calculated ([a4da2e4](https://github.com/raidcraft/rcregions/commit/a4da2e42981e561e3ade6b1eb6ce76b4fcc09a98))

# [1.4.0](https://github.com/raidcraft/rcregions/compare/v1.3.1...v1.4.0) (2020-11-29)


### Features

* add progressive region group costs ([79b2e8e](https://github.com/raidcraft/rcregions/commit/79b2e8e3e2f4d567249125db422259f97f014856)), closes [#11](https://github.com/raidcraft/rcregions/issues/11) [#10](https://github.com/raidcraft/rcregions/issues/10) [#8](https://github.com/raidcraft/rcregions/issues/8) [#9](https://github.com/raidcraft/rcregions/issues/9)

## [1.3.1](https://github.com/raidcraft/rcregions/compare/v1.3.0...v1.3.1) (2020-11-24)


### Bug Fixes

* **limits:** apply default limit to all players ([b3fc45f](https://github.com/raidcraft/rcregions/commit/b3fc45f7a18e1244879293d0033d1cca104bf82c)), closes [#8](https://github.com/raidcraft/rcregions/issues/8)

# [1.3.0](https://github.com/raidcraft/rcregions/compare/v1.2.2...v1.3.0) (2020-11-24)


### Features

* **regions:** add option to define region limits that can be assigned with permissions ([225ed17](https://github.com/raidcraft/rcregions/commit/225ed17ac4a656493f53bdf014c1e2976ec9489d)), closes [#8](https://github.com/raidcraft/rcregions/issues/8)

## [1.2.2](https://github.com/raidcraft/rcregions/compare/v1.2.1...v1.2.2) (2020-11-24)


### Bug Fixes

* **db:** rename tables to rcregions_ and create new migrations ([7363b2a](https://github.com/raidcraft/rcregions/commit/7363b2a0d42fb39ab370140c481d314a9e693619))

## [1.2.1](https://github.com/raidcraft/rcregions/compare/v1.2.0...v1.2.1) (2020-11-24)


### Bug Fixes

* **build:** properly shade interactive messenger ([80fdfac](https://github.com/raidcraft/rcregions/commit/80fdfac3fb16e3eb919603ce07c64cb3ec677dc4))

# [1.2.0](https://github.com/raidcraft/rcregions/compare/v1.1.1...v1.2.0) (2020-11-23)


### Features

* add new experimental messaging library ([66deb6c](https://github.com/raidcraft/rcregions/commit/66deb6c4535a491d2e24c021b290e13e42f21ae0))

## [1.1.1](https://github.com/Silthus/region-market/compare/v1.1.0...v1.1.1) (2020-11-21)


### Bug Fixes

* **signs:** show correct error messages ([15d16b1](https://github.com/Silthus/region-market/commit/15d16b1060610c4435168404a9b3e3e3bdfec5ff))

# [1.1.0](https://github.com/Silthus/region-market/compare/v1.0.0...v1.1.0) (2020-11-21)


### Bug Fixes

* **signs:** update sign on first placement ([e8a6370](https://github.com/Silthus/region-market/commit/e8a6370e24ab2467d3dd1db84b3b2767c265c446))


### Features

* **cmd:** add reload command ([2b23bc1](https://github.com/Silthus/region-market/commit/2b23bc1c0228aa0d85b9164b27b681118f6e120b))

# 1.0.0 (2020-11-21)


### Bug Fixes

* **db:** add initial migrations ([abea4a1](https://github.com/Silthus/region-market/commit/abea4a1b98562920dbbf9dbc196efe8955833388))
* **regions:** add option to buy regions via signs ([aebc39a](https://github.com/Silthus/region-market/commit/aebc39a8cc213fcd88e25a95e57e9e126cfd7599))


### Features

* add dynamic and static cost calculations based on m2 or m3 ([aa6a4ce](https://github.com/Silthus/region-market/commit/aa6a4cec63330fac9bcecee4f7aa6fbaaaec953a))
* add region creation via signs ([a36297f](https://github.com/Silthus/region-market/commit/a36297fffcae4d76abe01041ee15bf8e93083acb))
* support multiple database types with ebean ([919fa2e](https://github.com/Silthus/region-market/commit/919fa2eec9e313f19b5c7fa21587e5bb34df1ec7))
