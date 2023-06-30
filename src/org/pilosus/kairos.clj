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

  Date-Time entities use java.time.ZonedDateTime for UTC timezone."
  (:gen-class)
  (:require [clojure.string :as string])
  (:import (java.time ZonedDateTime ZoneId)))

(set! *warn-on-reflection* true)

;; Crontab parsing

(def skip-range "")

(def list-regex #",\s*")

(def range-regex
  #"(?s)(?<start>([0-9|*]+))(?:-(?<end>([0-9]+)))?(?:/(?<step>[0-9]+))?")

(def substitute-values
  {#"mon" "1" #"tue" "2" #"wed" "3" #"thu" "4" #"fri" "5" #"sat" "6" #"sun" "7"
   #"jan" "1" #"feb" "2" #"mar" "3" #"apr" "4" #"may" "5" #"jun" "6"
   #"jul" "7" #"aug" "8" #"sep" "9" #"oct" "10" #"nov" "11" #"dec" "12"})

(def range-values
  {:minute {:start 0 :end (+ 59 1)}
   :hour {:start 0 :end (+ 23 1)}
   :day-of-month {:start 1 :end (+ 31 1)}
   :month {:start 1 :end (+ 12 1)}
   :day-of-week {:start 1 :end (+ 7 1)}})

(defn parse-range
  "Parse a string with the range of values with an optional step value"
  [s range-type]
  (let [matcher (re-matcher range-regex s)]
    (if (.matches matcher)
      (let [start (.group matcher "start")
            range-start
            (cond
              (= start "*") (get-in range-values [range-type :start])
              :else (Integer/parseInt (.group matcher "start")))
            step (.group matcher "step")
            range-step (if step (Integer/parseInt step) 1)
            end (.group matcher "end")
            range-end
            (cond
              end (+ (Integer/parseInt end) 1)
              (or step (= start "*")) (get-in range-values [range-type :end])
              :else (+ range-start 1))]
        (range range-start range-end range-step))
      nil)))

(defn get-range
  "Parse a string into a sequence of numbers that represent a given
  Date-Time fragment's type, e.g. minutes, hours, days of month, etc."
  [s range-type]
  (let [ranges (string/split s list-regex)
        values (map #(parse-range % range-type) ranges)
        flat (apply concat values)
        sorted (sort flat)]
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

(defn- substitute-zero-day-of-week
  "Substitute 0 day of week for Sunday with 7 and return a sorted sequence"
  [day-of-week-range]
  (->>
   day-of-week-range
   (map #(if (= % 0) 7 %))
   sort))

(defn- split-cron-string
  "Split cron string to [minute hour day-of-month month day-of-week]"
  [s]
  (-> s
      string/trim
      string/lower-case
      names->numbers
      (string/split #"\s+")))

(defn parse-cron
  "Parse crontab string into map of ranges"
  [s]
  (try
    (let [[minute hour day-of-month month day-of-week] (split-cron-string s)
          ignore-day-of-month (and (= day-of-month "*") (not (= day-of-week "*")))
          day-of-month' (if ignore-day-of-month skip-range day-of-month)
          ignore-day-of-week (and (= day-of-week "*") (not (= day-of-month "*")))
          day-of-week' (if ignore-day-of-week skip-range day-of-week)
          cron
          {:minute (get-range minute :minute)
           :hour (get-range hour :hour)
           :day-of-month (get-range day-of-month' :day-of-month)
           :month (get-range month :month)
           :day-of-week (-> day-of-week'
                            (get-range :day-of-week)
                            substitute-zero-day-of-week)}]
      cron)
    (catch Exception _ nil)))

;; Date-Time sequence generation

(def utc-tz (ZoneId/of "UTC"))

(defn get-current-dt
  "Get current date time"
  ^java.time.ZonedDateTime []
  (ZonedDateTime/now ^java.time.ZoneId utc-tz))

(defn- get-current-year
  "Get current year for current ZonedDateTime in UTC"
  []
  (.getYear (get-current-dt)))

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
  (range (get-in range-values [:day-of-month :start])
         (get-in range-values [:day-of-month :end])))

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

;; Entrypoint

(defn get-dt-seq
  "Get a lazy sequence of future ZonedDateTime objects that satisfy crontab string.

  When years range is ommited get a sequence for 1 year starting from the current one."
  ([s]
   (let [year-from (get-current-year)
         year-to (+ year-from 1 1)]
     (get-dt-seq s year-from year-to)))
  ([s year-from year-to]
   (let [parsed-cron (parse-cron s)
         now (get-current-dt)]
     (when parsed-cron
       (for [year (range year-from year-to)
             month (:month parsed-cron)
             day (range-days)
             hour (:hour parsed-cron)
             minute (:minute parsed-cron)
             :let [dt (get-dt year month day hour minute)]
             :when (and
                    (dt-valid? dt (:day-of-month parsed-cron) (:day-of-week parsed-cron))
                    (dt-future? now dt))]
         dt)))))
