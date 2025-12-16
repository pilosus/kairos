# org.pilosus/kairos

[![Clojars Project](https://img.shields.io/clojars/v/org.pilosus/kairos.svg)](https://clojars.org/org.pilosus/kairos)
[![codecov](https://codecov.io/gh/pilosus/kairos/branch/main/graph/badge.svg?token=8OKTCKNq17)](https://codecov.io/gh/pilosus/kairos)

Crontab parser for Clojure with plain-English cron explanations.

- Supports [vixie-cron](https://man7.org/linux/man-pages/man5/crontab.5.html) syntax
- Parses a `crontab` entry into a lazy sequence of `java.time.ZonedDateTime` objects in `UTC` timezone
- Explains a `crontab` entry in plain English

*Kairos* (καιρός) means the right, critical, or opportune moment.

## Install

[![Clojars Project](https://clojars.org/org.pilosus/kairos/latest-version.svg)](https://clojars.org/org.pilosus/kairos)

## Usage

```clojure
(require '[org.pilosus.kairos :as kairos])

;; 1. Generate a sequence of Date Time objects for a given crontab entry
(kairos/cron->dt "0 10 3,7 Dec Mon")

;; (#object[java.time.ZonedDateTime 0x55eb9b05 "2023-12-03T10:00Z[UTC]"]
;;  #object[java.time.ZonedDateTime 0x2ed291ba "2023-12-04T10:00Z[UTC]"]
;;  ...
;;  #object[java.time.ZonedDateTime 0x749adbda "2024-12-30T10:00Z[UTC]"])


;; 2. Explain a crontab enrty in plain English
(kairos/cron->text "0 6,10-18/2,22 * * Mon-Fri")

;; at minute 0, past hour 6, every 2nd hour from 10 through 18, hour 22, on every day of week from Monday through Friday, in every month
```

## License

See [LICENSE](https://github.com/pilosus/kairos/tree/main/LICENSE).
