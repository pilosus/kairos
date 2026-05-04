;; Copyright (c) Vitaly Samigullin and contributors. All rights reserved.
;;
;; This program and the accompanying materials are made available under the
;; terms of the Eclipse Public License 2.0 which is available at
;; http://www.eclipse.org/legal/epl-2.0.
;;
;; This Source Code may also be made available under the following Secondary
;; Licenses when the conditions for such availability set forth in the Eclipse
;; Public License, v. 2.0 are satisfied: GNU General Public License as published by
;; the Free Software Foundation, either version 2 of the License, or (at your
;; option) any later version, with the GNU Classpath Exception which is available
;; at https://www.gnu.org/software/classpath/license.html.
;;
;; SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0

(ns org.pilosus.kairos
  "Crontab format parsing

  Format follows that of the Vixie cron:
  https://man7.org/linux/man-pages/man5/crontab.5.html

  including support for the following Date-Time matching:
  (month AND hour AND minute AND (day-of-month OR day-of-week))

  Short 3-letter names for months and week days are supported,
  e.g. Jan, Wed.

  Sunday's day of week number can be either 0 or 7.

  Date-Time entities are java.time.ZonedDateTime objects in given
  timezone or UTC by default.


  Definitions:

  field - time and date fragments of the cron entry.

  values - each field contains allowed values in the form of single
  number or a name, their ranges, ranges with step values, lists, or
  an asterisk.

  named value - a 3-letter name for a month or a day of week.

  nickname value - one of predefined aliases for useful cron entries.

  range - two numbers or named values separated by a hyphen.

  step value - range of values with a given step.

  list of values - values separated by comma."
  (:gen-class)
  (:require [clojure.string :as string])
  (:import (java.time ZonedDateTime ZoneId)))

;; Crontab parsing

(def skip-field "")

(def list-regex #",\s*")

(def number-with-step-regex #"\*/(\d+)")

(def value-regex
  #"(?s)(?<start>([0-9|*]+))(?:-(?<end>([0-9]+)))?(?:/(?<step>[0-9]+))?")

(def substitute-values
  {#"mon" "1" #"tue" "2" #"wed" "3" #"thu" "4" #"fri" "5" #"sat" "6" #"sun" "0"
   #"jan" "1" #"feb" "2" #"mar" "3" #"apr" "4" #"may" "5" #"jun" "6"
   #"jul" "7" #"aug" "8" #"sep" "9" #"oct" "10" #"nov" "11" #"dec" "12"})

(def nickname->cron
  {"@yearly" "0 0 1 1 *"
   "@annually" "0 0 1 1 *"
   "@monthly" "0 0 1 * *"
   "@weekly" "0 0 * * 0"  ;; on Sundays
   "@daily" "0 0 * * *"
   "@hourly" "0 * * * *"})

(def nickname->text
  {"@yearly" "every January 1st at midnight"
   "@annually" "every January 1st at midnight"
   "@monthly" "every 1st of the month at midnight"
   "@weekly" "every Sunday at midnight"
   "@daily" "every day at midnight"
   "@hourly" "every hour"})

;; ISO-8601 day-of-week mapping
(def day-number->name
  {1 "Monday"
   2 "Tuesday"
   3 "Wednesday"
   4 "Thursday"
   5 "Friday"
   6 "Saturday"
   7 "Sunday"})

(def month-number->name
  {1 "January"
   2 "February"
   3 "March"
   4 "April"
   5 "May"
   6 "June"
   7 "July"
   8 "August"
   9 "September"
   10 "October"
   11 "November"
   12 "December"})

(def field->range
  {:minute {:start 0 :end (+ 59 1)}
   :hour {:start 0 :end (+ 23 1)}
   :day-of-month {:start 1 :end (+ 31 1)}
   :month {:start 1 :end (+ 12 1)}
   :day-of-week {:start 0 :end (+ 7 1)}})

(def field->name
  {:minute "minute"
   :hour "hour"
   :day-of-month "day of month"
   :month "month"
   :day-of-week "day of week"})

(defn validate-value
  "Return true if value for the given field is valid, throw an exception
  otherwise"
  [start end field]
  (let [start-expected (get-in field->range [field :start])
        end-expected (get-in field->range [field :end])
        valid? (cond
                 (or (nil? start-expected)
                     (nil? end-expected)) false
                 (and (some? start)
                      (some? end)) (and
                                    (>= start start-expected)
                                    (<= start end-expected)
                                    (<= end end-expected)
                                    (>= end start-expected)
                                    (<= start end))
                 (some? start) (and
                                (>= start start-expected)
                                (<= start end-expected))
                 (some? end) (and
                              (<= end end-expected)
                              (>= end start-expected))
                 :else false)]
    (or
     valid?
     (throw
      (ex-info
       "Wrong value"
       {:field field
        :given {:start start :end end}
        :expected {:start start-expected :end end-expected}})))))

(defmulti value->int
  "Parse a string with the values into a range of integers"
  (fn [_ field] field))

(defn- zero->seven
  "Translate cron's day-of-week format with the week starting on Sunday
  as a day 0 into an ISO-8601 with the week starting on Monday as a
  day 1"
  [n]
  (if (= n 0) 7 n))

(defmethod value->int :day-of-week
  [s field]
  (let [matcher (re-matcher value-regex s)]
    (if (.matches matcher)
      (let [start (.group matcher "start")
            range-start
            (cond
              (= start "*") (get-in field->range [field :start])
              :else (Integer/parseInt (.group matcher "start")))
            step (.group matcher "step")
            range-step (if step (Integer/parseInt step) 1)
            end (.group matcher "end")
            range-end
            (cond
              end (+ (Integer/parseInt end) 1)
              (or step (= start "*")) (get-in field->range [field :end])
              :else (+ range-start 1))]
        (when (validate-value range-start range-end field)
          (map zero->seven (range range-start range-end range-step))))
      nil)))

(defmethod value->int :default
  [s field]
  (let [matcher (re-matcher value-regex s)]
    (if (.matches matcher)
      (let [start (.group matcher "start")
            range-start
            (cond
              (= start "*") (get-in field->range [field :start])
              :else (Integer/parseInt (.group matcher "start")))
            step (.group matcher "step")
            range-step (if step (Integer/parseInt step) 1)
            end (.group matcher "end")
            range-end
            (cond
              end (+ (Integer/parseInt end) 1)
              (or step (= start "*")) (get-in field->range [field :end])
              :else (+ range-start 1))]
        (when (validate-value range-start range-end field)
          (range range-start range-end range-step)))
      nil)))

(defn value->ordinal
  "Return an ordinal integer number with the proper suffix in English,
  e.g. 1 -> 1st, 2 -> 2nd, 3 -> 3rd, 15 -> 15th, etc."
  [s]
  (when-let [number (try (Integer/parseInt s) (catch Exception _ nil))]
    (let [last-one-digit (mod number 10)
          last-two-digits (mod number 100)
          suffix (cond
                   (contains? #{11 12 13} last-two-digits) "th"
                   (= last-one-digit 1) "st"
                   (= last-one-digit 2) "nd"
                   (= last-one-digit 3) "rd"
                   :else "th")]
      (format "%s%s" number suffix))))

(defn field->values
  "Parse a string into a sequence of numbers that represent a given
  Date-Time field's type, e.g. minutes, hours, days of month, etc."
  [s field]
  (let [lists (string/split s list-regex)
        parsed-values (map #(value->int % field) lists)
        flat (apply concat parsed-values)
        sorted (-> flat
                   distinct
                   sort)]
    sorted))

(defn- replace-names-with-numbers
  [s substitutions]
  (if (empty? substitutions)
    s
    (let [[match replacement] (first substitutions)
          s-replaced (string/replace s match replacement)]
      (recur s-replaced (rest substitutions)))))

(defn- names->numbers
  "Replace month and day of week names with respective numeric values"
  [^String s]
  (if-let [expanded (nickname->cron s)]
    expanded
    (replace-names-with-numbers s (seq substitute-values))))

(defn- split-cron-string
  "Split cron string to [minute hour day-of-month month day-of-week]"
  [s]
  (-> s
      string/trim
      string/lower-case
      names->numbers
      (string/split #"\s+")))

;; Locales

(defn- ordinal-en
  "English ordinal: 1 -> 1st, 2 -> 2nd, 3 -> 3rd, 11 -> 11th, etc."
  [n]
  (let [last-one (mod n 10)
        last-two (mod n 100)
        suffix (cond
                 (contains? #{11 12 13} last-two) "th"
                 (= last-one 1) "st"
                 (= last-one 2) "nd"
                 (= last-one 3) "rd"
                 :else "th")]
    (format "%s%s" n suffix)))

(def locale-en
  {:day-names          day-number->name
   :month-names        month-number->name
   :field-names        field->name
   :nicknames          nickname->text
   :ordinal-fn         ordinal-en
   :time-fn            nil
   :midnight           "midnight"
   :every-day          "every day"
   :every-minute       "every minute"
   :weekday            "weekday"
   :weekend            "weekend"
   :fmt/verbose        "at %s, past %s, on %s, in %s"
   :fmt/or             "%s or %s"
   :fmt/every-unit     "every %s"
   :fmt/every-nth      "every %s %s"
   :fmt/unit-value     "%s %s"
   :fmt/range          "every %s from %s through %s"
   :fmt/range-step     "every %s %s from %s through %s"
   :fmt/every-n-minutes    "every %s minutes"
   :fmt/every-n-hours      "every %s hours"
   :fmt/every-dow-at       "every %s at %s"
   :fmt/every-dom-at       "every %s of the month at %s"
   :fmt/every-month-dom-at "every %s %s at %s"
   :fmt/every-day-at       "every day at %s"})

(defn- resolve-locale [locale]
  (merge locale-en (or locale {})))

;; Parsing cron into human-readable text

(defn- value-fragment->text*
  "Parse a fragment of the value into human-readable text using locale.
   case-form can be :nominative (default) or :genitive for range start values."
  ([s field locale] (value-fragment->text* s field locale :nominative))
  ([s field locale case-form]
   (case field
     :month
     (when-let [number (try (Integer/parseInt s) (catch Exception _ nil))]
       (let [names (if (and (= case-form :genitive) (:month-names-genitive locale))
                     (:month-names-genitive locale)
                     (:month-names locale))]
         (get names number)))
     :day-of-week
     (when-let [number (try (Integer/parseInt s) (catch Exception _ nil))]
       (let [names (if (and (= case-form :genitive) (:day-names-genitive locale))
                     (:day-names-genitive locale)
                     (:day-names locale))]
         (get names (zero->seven number))))
     s)))

(defmulti value-fragment->text
  "Parse a frament of the value into a human readable text"
  (fn [_ field] field))

(defmethod value-fragment->text :month [s _]
  (value-fragment->text* s :month locale-en))

(defmethod value-fragment->text :day-of-week [s _]
  (value-fragment->text* s :day-of-week locale-en))

(defmethod value-fragment->text :default [s _]
  (value-fragment->text* s :default locale-en))

(defn- value->text*
  "Parse a string with the values into human-readable text using locale"
  [s field locale]
  (let [matcher (re-matcher value-regex s)]
    (if (.matches matcher)
      (let [field-fmts (get-in locale [:field-fmts field])
            unit (get (:field-names locale) field)
            article (get-in locale [:field-articles field])
            qual-unit (if article (str article " " unit) unit)
            start (.group matcher "start")
            step (.group matcher "step")
            end (.group matcher "end")
            parsed-start (value-fragment->text* start field locale)
            parsed-start-gen (value-fragment->text* start field locale :genitive)
            ordinal-fn (or (get-in locale [:field-ordinals field])
                           (:ordinal-fn locale))
            parsed-step (when step (ordinal-fn (Integer/parseInt step)))
            parsed-end (value-fragment->text* end field locale)]
        (cond
          (and
           (= start "*")
           (nil? step)) (if field-fmts
                          (:fmt/every-unit field-fmts)
                          (format (:fmt/every-unit locale) qual-unit))
          (and
           (= start "*")
           (some? step)) (if field-fmts
                           (format (:fmt/every-nth field-fmts) parsed-step)
                           (if article
                             (format (:fmt/every-nth locale)
                                     (str article " " parsed-step " " unit))
                             (format (:fmt/every-nth locale) parsed-step unit)))
          (and
           (some? start)
           (nil? end)) (if field-fmts
                         (format (:fmt/unit-value field-fmts) parsed-start)
                         (format (:fmt/unit-value locale) unit parsed-start))
          (and
           (some? start)
           (some? end)
           (nil? step)) (if field-fmts
                          (format (:fmt/range field-fmts)
                                  parsed-start-gen parsed-end)
                          (format (:fmt/range locale)
                                  qual-unit parsed-start parsed-end))
          (and
           (some? start)
           (some? end)
           (some? step)) (if field-fmts
                           (format (:fmt/range-step field-fmts)
                                   parsed-step parsed-start-gen parsed-end)
                           (if article
                             (format (:fmt/range-step locale)
                                     (str article " " parsed-step " " unit)
                                     parsed-start parsed-end)
                             (format (:fmt/range-step locale)
                                     parsed-step unit parsed-start parsed-end)))
          :else nil))
      nil)))

(defmulti value->text
  "Parse a string with the values into a human-readable text"
  (fn [_ field] field))

(defmethod value->text :default
  [s field]
  (value->text* s field locale-en))

(defn- field->text*
  "Parse field values into human-readable text using locale"
  [s field locale]
  (let [lists (string/split s list-regex)
        parsed-values (map #(value->text* % field locale) lists)]
    (string/join ", " parsed-values)))

(defn field->text
  "Parse field values into a human readable text"
  [s field]
  (field->text* s field locale-en))

;; Date-Time sequence generation

(def utc-tz (ZoneId/of "UTC"))

(defn get-current-dt
  "Get current date time"
  (^java.time.ZonedDateTime [] (get-current-dt utc-tz))
  (^java.time.ZonedDateTime [^java.time.ZoneId tz]
   (ZonedDateTime/now ^java.time.ZoneId tz)))

(defn get-dt
  "Return ZonedDateTime in given time zone/UTC or nil for invalid Date-Time"
  ([year month day hour minute] (get-dt year month day hour minute utc-tz))
  ([year month day hour minute tz]
   (let [second 0
         nanosecond 0]
     (try (ZonedDateTime/of year month day hour minute second nanosecond tz)
          (catch java.time.DateTimeException _ nil)))))

(defn- range-days
  "Get a seq of days of month"
  []
  (range (get-in field->range [:day-of-month :start])
         (get-in field->range [:day-of-month :end])))

(defn seq-contains?
  "Return true if a sequence contains the element"
  [coll e]
  (if (some #(= e %) coll) true false))

(defn dt-valid?
  "Return true if a given ZonedDateTime satisfies day-of-month OR day-of-week constrains"
  [^java.time.ZonedDateTime dt days-of-month days-of-week]
  (if dt
    (let [dt-day-of-month (-> dt .getDayOfMonth)
          dt-day-of-week (-> dt .getDayOfWeek .getValue)]
      (or (seq-contains? days-of-month dt-day-of-month)
          (seq-contains? days-of-week dt-day-of-week)))
    false))

(defn dt-future?
  "Return true if provided ZonedDateTime is in the future"
  [^java.time.ZonedDateTime current-dt ^java.time.ZonedDateTime another-dt]
  (.isAfter another-dt current-dt))

(defn- cron->map-unsafe
  "Parse a crontab string into a map of ranges, throw an exception if
  parsing fails"
  [s]
  (let [[minute hour day-of-month month day-of-week] (split-cron-string s)
        ignore-day-of-month (and (= day-of-month "*") (not (= day-of-week "*")))
        day-of-month' (if ignore-day-of-month skip-field day-of-month)
        ignore-day-of-week (and (= day-of-week "*") (not (= day-of-month "*")))
        day-of-week' (if ignore-day-of-week skip-field day-of-week)
        cron
        {:minute (field->values minute :minute)
         :hour (field->values hour :hour)
         :day-of-month (field->values day-of-month' :day-of-month)
         :month (field->values month :month)
         :day-of-week (field->values day-of-week' :day-of-week)}]
    cron))

;; Simplified text

(defn- simple-time->text
  [minute hour locale]
  (if-let [time-fn (:time-fn locale)]
    (time-fn hour minute)
    (cond
      (and (= minute "0") (= hour "0")) (:midnight locale)
      (and (= minute "0") (not= hour "0")) (format "%s:00" hour)
      (and (re-matches #"\d+" minute) (re-matches #"\d+" hour))
      (format "%s:%02d" hour (Integer/parseInt minute))
      :else nil)))

(defn- simple-day-of-month->text
  [day-of-month locale]
  (when-let [number (try (Integer/parseInt day-of-month) (catch Exception _ nil))]
    ((:ordinal-fn locale) number)))

(defn- simple-month->text
  [month locale]
  (value-fragment->text* month :month locale))

(defn- simple-day-of-week->text
  [day-of-week locale]
  (cond
    (or (= day-of-week "1-5")
        (= day-of-week "1,2,3,4,5")) (:weekday locale)
    (or (= day-of-week "6,0")
        (= day-of-week "6,7")
        (= day-of-week "6-7")) (:weekend locale)
    :else nil))

(defn- cron->simple-text
  [s locale]
  (let [nicknamed (get (:nicknames locale) (-> s string/trim string/lower-case))]
    (if nicknamed
      nicknamed
      (try
        (cron->map-unsafe s)
        (let [[minute hour day-of-month month day-of-week] (split-cron-string s)
              time (simple-time->text minute hour locale)
              dow (simple-day-of-week->text day-of-week locale)
              dom (simple-day-of-month->text day-of-month locale)
              mon (simple-month->text month locale)]
          (cond
            ;; */5 * * * *  ->  "every 5 minutes"
            (and (= hour "*")
                 (= day-of-month "*")
                 (= month "*")
                 (= day-of-week "*")
                 (re-matches number-with-step-regex minute))
            (let [v (second (re-matches number-with-step-regex minute))
                  fmt (:fmt/every-n-minutes locale)]
              (if (fn? fmt) (fmt v) (format fmt v)))

             ;; * * * * *  ->  "every minute"
            (and (= minute "*")
                 (= hour "*")
                 (= day-of-month "*")
                 (= month "*")
                 (= day-of-week "*"))
            (:every-minute locale)

            ;; 0 */2 * * *  ->  "every 2 hours"
            (and (= minute "0")
                 (= day-of-month "*")
                 (= month "*")
                 (= day-of-week "*")
                 (re-matches number-with-step-regex hour))
            (let [v (second (re-matches number-with-step-regex hour))
                  fmt (:fmt/every-n-hours locale)]
              (if (fn? fmt) (fmt v) (format fmt v)))

            ;; fixed time + all days + all months + specific dow
            ;; 0 9 * * 1-5 -> "every weekday at 9:00"
            (and time
                 (= day-of-month "*")
                 (= month "*")
                 dow)
            (format (:fmt/every-dow-at locale) dow time)

            ;; fixed time + specific dom + all months + all dow
            ;; 30 8 15 * *  ->  "every 15th of the month at 8:30"
            (and time
                 dom
                 (= month "*")
                 (= day-of-week "*"))
            (format (:fmt/every-dom-at locale) dom time)

            ;; fixed time + specific dom + specific month + all dow
            ;; 0 0 25 12 *  ->  "every December 25th at midnight"
            (and time
                 dom
                 mon
                 (= day-of-week "*"))
            (let [fmt (:fmt/every-month-dom-at locale)]
              (if (fn? fmt)
                (fmt {:day-of-month day-of-month :month month :time time :locale locale})
                (format fmt mon dom time)))

            ;; fixed time + all days + all months + all dow
            ;; 30 8 * * *  ->  "every day at 8:30"
            (and time
                 (= day-of-month "*")
                 (= month "*")
                 (= day-of-week "*"))
            (format (:fmt/every-day-at locale) time)

            ;; nothing matched -> nil, fall through to verbose
            :else nil))
        (catch Exception _ nil)))))

;; Entrypoints

(defn cron->map
  "Parse a crontab string into a map of ranges"
  [s]
  (try (cron->map-unsafe s)
       (catch Exception _ nil)))

(defn cron-valid?
  "Return true if a crontab entry is valid"
  [s]
  (some? (cron->map s)))

(defn cron-validate
  "Return validation status and error message for a given crontab entry"
  [s]
  (try (cron->map-unsafe s)
       {:ok? true}
       (catch clojure.lang.ExceptionInfo e
         (let [{:keys [field given expected]} (ex-data e)
               msg (format "Value error in '%s' field. Given value: [%s, %s). Expected: [%s, %s)"
                           (name field)
                           (:start given) (:end given)
                           (:start expected) (:end expected))]
           {:ok? false :error msg}))
       (catch Exception _
         {:ok? false :error "Invalid crontab format"})))

(defn cron->dt
  "Parse crontab string into a lazy seq of ZonedDateTime objects
  opts:
    :start - ZonedDateTime after which to generate times (default: now).
             Only the instant matters, not its timezone - results after
             this point in time are included regardless of zone.
    :tz    - ZoneId for generated datetimes (default: UTC)

  "
  ([s] (cron->dt s {}))
  ([s {:keys [start tz]}]
   (let [parsed-cron (cron->map s)
         tz (or tz utc-tz)
         start (or start (get-current-dt tz))
         current-year (.getYear ^java.time.ZonedDateTime (.withZoneSameInstant ^java.time.ZonedDateTime start ^java.time.ZoneId tz))
         years (iterate inc current-year)
         results
         (when parsed-cron
           (for [year years
                 month (:month parsed-cron)
                 day (range-days)
                 hour (:hour parsed-cron)
                 minute (:minute parsed-cron)
                 :let [dt (get-dt year month day hour minute tz)]
                 :when (and
                        (dt-valid?
                         dt
                         (:day-of-month parsed-cron)
                         (:day-of-week parsed-cron))
                        (dt-future? start dt))]
             dt))]
     results)))

(defn cron->text
  "Parse crontab string into a human-readable text.
   opts:
     :locale - a locale map for translations (default: English).
               Partial maps are merged with the English defaults.
               See locale-en for the full schema."
  ([s] (cron->text s {}))
  ([s {:keys [locale]}]
   (let [resolved (resolve-locale locale)]
     (or (cron->simple-text s resolved)
         (try
           (cron->map-unsafe s)
           (let [[minute hour day-of-month month day-of-week] (split-cron-string s)
                 day-of-month' (field->text* day-of-month :day-of-month resolved)
                 day-of-week' (field->text* day-of-week :day-of-week resolved)
                 minute' (field->text* minute :minute resolved)
                 hour' (field->text* hour :hour resolved)
                 day' (cond
                        (and (= day-of-month "*")
                             (not (= day-of-week "*"))) (field->text* day-of-week :day-of-week resolved)
                        (and (not (= day-of-month "*"))
                             (= day-of-week "*")) (field->text* day-of-month :day-of-month resolved)
                        (and (not (= day-of-month "*"))
                             (not (= day-of-week "*"))) (format (:fmt/or resolved) day-of-month' day-of-week')
                        :else (:every-day resolved))
                 month' (field->text* month :month resolved)
                 text (if-let [verbose-fn (:fmt/verbose-fn resolved)]
                        (verbose-fn {:minute minute' :hour hour'
                                     :day day' :month month'
                                     :minute-raw minute})
                        (format (:fmt/verbose resolved) minute' hour' day' month'))]
             text)
           (catch Exception _ nil))))))
