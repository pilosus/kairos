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

  Date-Time entities use java.time.ZonedDateTime for UTC timezone.


  Definitions:

  field - time and date fragments of the cron entry.

  values - each field contains allowed values in the form of single
  number or a name, their ranges, ranges with step values, lists, or
  an asterisk.

  named value - a 3-letter name for a month or a day of week.

  range - two numbers or named values separated by a hyphen.

  step value - range of values with a given step.

  list of values - values separated by comma."
  (:gen-class)
  (:require [clojure.string :as string])
  (:import (java.time ZonedDateTime ZoneId)))

;; Crontab parsing

(def skip-field "")

(def list-regex #",\s*")

(def value-regex
  #"(?s)(?<start>([0-9|*]+))(?:-(?<end>([0-9]+)))?(?:/(?<step>[0-9]+))?")

(def substitute-values
  {#"mon" "1" #"tue" "2" #"wed" "3" #"thu" "4" #"fri" "5" #"sat" "6" #"sun" "0"
   #"jan" "1" #"feb" "2" #"mar" "3" #"apr" "4" #"may" "5" #"jun" "6"
   #"jul" "7" #"aug" "8" #"sep" "9" #"oct" "10" #"nov" "11" #"dec" "12"})

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
  (replace-names-with-numbers s (seq substitute-values)))

(defn- split-cron-string
  "Split cron string to [minute hour day-of-month month day-of-week]"
  [s]
  (-> s
      string/trim
      string/lower-case
      names->numbers
      (string/split #"\s+")))

;; Parsing cron into human-readable text

(defmulti value-fragment->text
  "Parse a frament of the value into a human readable text"
  (fn [_ field] field))

(defmethod value-fragment->text :month [s _]
  (when-let [number (try (Integer/parseInt s) (catch Exception _ nil))]
    (->> number
         (get month-number->name))))

(defmethod value-fragment->text :day-of-week [s _]
  (when-let [number (try (Integer/parseInt s) (catch Exception _ nil))]
    (->> number
         zero->seven
         (get day-number->name))))

(defmethod value-fragment->text :default [s _]
  s)

(defmulti value->text
  "Parse a string with the values into a human-readable text"
  (fn [_ field] field))

(defmethod value->text :default
  [s field]
  (let [matcher (re-matcher value-regex s)]
    (if (.matches matcher)
      (let [unit (get field->name field)
            start (.group matcher "start")
            step (.group matcher "step")
            end (.group matcher "end")
            parsed-start (value-fragment->text start field)
            parsed-step (value->ordinal step)
            parsed-end (value-fragment->text end field)]
        ;; start is guaranteed to be non-empty by the regex,
        ;; yet we keep it in the cond for readability
        (cond
          (and
           (= start "*")
           (nil? step)) (format "every %s" unit)
          (and
           (= start "*")
           (some? step)) (format "every %s %s" parsed-step unit)
          (and
           (some? start)
           (nil? end)) (format "%s %s" unit parsed-start)
          (and
           (some? start)
           (some? end)
           (nil? step)) (format "every %s from %s through %s"
                                unit parsed-start parsed-end)
          (and
           (some? start)
           (some? end)
           (some? step)) (format "every %s %s from %s through %s"
                                 parsed-step unit parsed-start parsed-end)
          :else nil))
      nil)))

(defn field->text
  "Parse field values into a human readable text"
  [s field]
  (let [lists (string/split s list-regex)
        parsed-values (map #(value->text % field) lists)
        result (string/join ", " parsed-values)]
    result))

;; Date-Time sequence generation

(def utc-tz (ZoneId/of "UTC"))

(defn get-current-dt
  "Get current date time"
  ^java.time.ZonedDateTime []
  (ZonedDateTime/now ^java.time.ZoneId utc-tz))

(defn get-dt
  "Return ZonedDateTime in UTC or nil for invalid Date-Time"
  [year month day hour minute]
  (let [second 0
        nanosecond 0
        tz utc-tz]
    (try (ZonedDateTime/of year month day hour minute second nanosecond tz)
         (catch java.time.DateTimeException _ nil))))

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

(defn- cron->map-throwable
  "Parse crontab string into map of ranges, throw an exception if
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

;; Entrypoints

(defn cron->map
  "Parse crontab string into map of ranges safely"
  [s]
  (try (cron->map-throwable s)
       (catch Exception _ nil)))

(defn cron-valid?
  "Return true if a crontab entry is valid"
  [s]
  (some? (cron->map s)))

(defn cron-validate
  "Return validation status and error message for a given crontab entry"
  [s]
  (try (cron->map-throwable s)
       {:ok? true}
       (catch clojure.lang.ExceptionInfo e
         (let [{:keys [field given expected]} (ex-data e)
               msg (format "Value error in '%s' field. Given value: [%s, %s). Expected: [%s, %s)"
                           (name field)
                           (:start given) (:end given)
                           (:start expected) (:end expected))]
           {:ok? false :error msg}))))

(defn cron->dt
  "Parse crontab string into a lazy seq of ZonedDateTime objects"
  [s]
  (let [parsed-cron (cron->map s)
        now (get-current-dt)
        current-year (.getYear now)
        years (iterate inc current-year)]
    (when parsed-cron
      (for [year years
            month (:month parsed-cron)
            day (range-days)
            hour (:hour parsed-cron)
            minute (:minute parsed-cron)
            :let [dt (get-dt year month day hour minute)]
            :when (and
                   (dt-valid? dt (:day-of-month parsed-cron) (:day-of-week parsed-cron))
                   (dt-future? now dt))]
        dt))))

(defn cron->text
  "Parse crontab string into a human-readable text"
  [s]
  (try
    (cron->map-throwable s)
    (let [[minute hour day-of-month month day-of-week] (split-cron-string s)
          day-of-month' (field->text day-of-month :day-of-month)
          day-of-week' (field->text day-of-week :day-of-week)
          minute' (field->text minute :minute)
          hour' (field->text hour :hour)
          day' (cond
                 (and (= day-of-month "*")
                      (not (= day-of-week "*"))) (field->text day-of-week :day-of-week)
                 (and (not (= day-of-month "*"))
                      (= day-of-week "*")) (field->text day-of-month :day-of-month)
                 (and (not (= day-of-month "*"))
                      (not (= day-of-week "*"))) (format "%s or %s" day-of-month' day-of-week')
                 :else "every day")
          month' (field->text month :month)
          text (format "at %s, past %s, on %s, in %s" minute' hour' day' month')]
      text)
    (catch Exception _ nil)))
