# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]

Nothing yet in `Unreleased` section.

## [v0.2.0] - 2023-08-11

**NB!** Release breaks backward compatibility. See release notes.

### Added
- Parse a crontab entry into a human-readable English text with
  `cron->text` function
  ([#3](https://github.com/pilosus/kairos/issues/3))
- Parse a crontab entry into a lazy sequence of
  `java.time.ZonedDateTime` objects with `cron->dt` function (used
  instead of `get-dt-seq`, year range function arguments dropped)

### Changed
- Function `parse-cron` renamed to `cron->map`

### Removed
- Function `get-dt-seq` removed, use `cron->dt` instead

## [v0.1.14] - 2023-07-01
### Changed
- Release version's [patch](https://semver.org/) part uses `git
  rev-list HEAD --count` on `main` branch.
- Snapshot versions (being generated on merges to `main`) has
  `9999-SNAPSHOT` patch part

## [v0.1.3] - 2023-07-01
### Added
- Better examples in `README.md`
- `cljdoc` config and its check in CI are added

## [v0.1.2] - 2023-06-30
### Fixed
- Generating `pom.xml` from template fixed

## [v0.1.1] - 2023-06-30
### Added
- Crontab string parsing
- Generation of Date-Time objects satisfying crontab
  conditions. Objects get generated as a lazy sequence.
- Project CI/CD workflows

[Unreleased]: https://github.com/pilosus/kairos/compare/v0.2.0...HEAD
[v0.2.0]: https://github.com/pilosus/kairos/compare/v0.1.14...v0.2.0
[v0.1.14]: https://github.com/pilosus/kairos/compare/v0.1.3...v0.1.14
[v0.1.3]: https://github.com/pilosus/kairos/compare/v0.1.2...v0.1.3
[v0.1.2]: https://github.com/pilosus/kairos/compare/v0.1.1...v0.1.2
[v0.1.1]: https://github.com/pilosus/kairos/compare/v0.0.0...v0.1.1
