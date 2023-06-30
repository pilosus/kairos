# org.pilosus/kairos

[![Clojars Project](https://img.shields.io/clojars/v/org.pilosus/kairos.svg)](https://clojars.org/org.pilosus/kairos)
[![codecov](https://codecov.io/gh/pilosus/kairos/branch/main/graph/badge.svg?token=8OKTCKNq17)](https://codecov.io/gh/pilosus/kairos)

Crontab parsing library for Clojure.

Supports [vixie-cron](https://man7.org/linux/man-pages/man5/crontab.5.html)
syntax. Generates a lazy sequence of `java.time.ZonedDateTime` objects
in UTC timezone that satisfy given `crontab` conditions.

## Install

[![Clojars Project](https://clojars.org/org.pilosus/kairos/latest-version.svg)](https://clojars.org/org.pilosus/kairos)

## Usage

```clojure
(require '[org.pilosus.kairos :as kairos])

;; parse crontab string into a map
(kairos/parse-cron "39 9 * * wed-fri")

;; generate a lazy sequence of java.time.ZonedDateTime[UTC] objects
;; that satisfy given crontab constraints
(kairos/get-dt-seq "0 10 3,7 Dec Mon")

;; generate Date-Time sequence for a range of years
;; range from/to behaves like the one in clojure.core/range
(kairos/get-dt-seq "39 9 * * wed-fri" 2023 2029)
```

## License

See [LICENSE](https://github.com/pilosus/kairos/tree/main/LICENSE)
