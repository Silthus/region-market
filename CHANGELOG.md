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
