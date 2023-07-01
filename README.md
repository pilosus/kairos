# org.pilosus/kairos

[![Clojars Project](https://img.shields.io/clojars/v/org.pilosus/kairos.svg)](https://clojars.org/org.pilosus/kairos)
[![codecov](https://codecov.io/gh/pilosus/kairos/branch/main/graph/badge.svg?token=8OKTCKNq17)](https://codecov.io/gh/pilosus/kairos)

Crontab parsing library for Clojure.

Supports [vixie-cron](https://man7.org/linux/man-pages/man5/crontab.5.html)
syntax. Generates a lazy sequence of `java.time.ZonedDateTime` objects
in `UTC` timezone that satisfy given `crontab` constraints.

*Kairos* (καιρός) means the right, critical, or opportune moment.

## Install

[![Clojars Project](https://clojars.org/org.pilosus/kairos/latest-version.svg)](https://clojars.org/org.pilosus/kairos)

## Usage

```clojure
(require '[org.pilosus.kairos :as kairos])

;; 1. Generate a lazy sequence of java.time.ZonedDateTime[UTC] objects
;; that satisfy given crontab constraints:
(first (kairos/get-dt-seq "0 10 3,7 Dec Mon"))

;; #object[java.time.ZonedDateTime 0x1ea5bb00 "2023-12-03T10:00Z[UTC]"]

;; 2. Generate a Date-Time sequence for a range of years,
;; from start (inclusive) to end (exclusive):
(->> (kairos/get-dt-seq "0 10 3,7 Dec Mon" 2030 2032)
     (take 5)
     (map str))

;; ("2030-12-02T10:00Z[UTC]"
;;  "2030-12-03T10:00Z[UTC]"
;;  "2030-12-07T10:00Z[UTC]"
;;  "2030-12-09T10:00Z[UTC]"
;;  "2030-12-16T10:00Z[UTC]")

;; 3. Parse crontab string into a map:
(kairos/parse-cron "12,14,17,35-45/3 */2 27 Feb-Jun *")

;; {:minute (12 14 17 35 38 41 44),
;;  :hour (0 2 4 6 8 10 12 14 16 18 20 22),
;;  :day-of-month (27),
;;  :month (2 3 4 5 6),
;;  :day-of-week ()}
```

## License

See [LICENSE](https://github.com/pilosus/kairos/tree/main/LICENSE)
