(ns org.pilosus.kairos
  (:gen-class)
  (:require [clojure.string :as string]))

;; crontab parsing

(def skip-range "")

(def list-regex #",\s*")

(def range-regex
  #"(?s)(?<start>([0-9|*]+))(?:-(?<end>([0-9]+)))?(?:/(?<step>[0-9]+))?")

(def substitute-values
  {#"mon" "1" #"tue" "2" #"wed" "3" #"thu" "4" #"fri" "5" #"sat" "6" #"sun" "7"
   #"jan" "1" #"feb" "2" #"mar" "3" #"apr" "4" #"may" "5" #"jun" "6" #"jul" "7"
   #"aug" "8" #"sep" "9" #"oct" "10" #"nov" "11" #"dec" "12"})

(def range-values
  {:minute {:start 0 :end (+ 59 1)}
   :hour {:start 0 :end (+ 23 1)}
   :day-of-month {:start 1 :end (+ 31 1)}
   :month {:start 1 :end (+ 12 1)}
   :day-of-week {:start 1 :end (+ 7 1)}})

(defn parse-range
  [^String s
   ^clojure.lang.Keyword type]
  "Parse range of values with possible step value"
  (let [matcher (re-matcher range-regex s)]
    (if (.matches matcher)
      (let [start (.group matcher "start")
            range-start
            (cond
              (= start "*") (get-in range-values [type :start])
              :else (Integer/parseInt (.group matcher "start")))

            step (.group matcher "step")
            range-step (if step (Integer/parseInt step) 1)

            end (.group matcher "end")
            range-end
            (cond
              end (+ (Integer/parseInt end) 1)
              (or step (= start "*")) (get-in range-values [type :end])
              :else (+ range-start 1))]
        (range range-start range-end range-step))
      nil)))

(defn parse-value
  "Parse single value (e.g. minutes, hours, months, etc.)"
  [^String s
   ^clojure.lang.Keyword type]
  (let [ranges (string/split s list-regex)
        values (map #(parse-range % type) ranges)
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

(defn names->numbers
  "Replace month and day of week names with respective numeric values"
  [^String s]
  (replace-names-with-numbers s (seq substitute-values)))

(defn- substitute-zero-day-of-week
  "Substitute 0 day of week for Sunday with 7 and return sorted sequence"
  [day-of-week-range]
  (->>
   day-of-week-range
   (map #(if (= % 0) 7 %))
   sort))

(defn parse-cron
  "Parse crontab string"
  [^String s]
  (try
    (let
     [[minute hour day-of-month month day-of-week]
      (-> s
          string/trim
          string/lower-case
          names->numbers
          (string/split #"\s+"))
      day-of-month'
      (cond
        (and (= day-of-month "*") (not (= day-of-week "*"))) skip-range
        :else day-of-month)
      day-of-week'
      (cond
        (and (= day-of-week "*") (not (= day-of-month "*"))) skip-range
        :else day-of-week)
      cron
      {:minute (parse-value minute :minute)
       :hour (parse-value hour :hour)
       :day-of-month (parse-value day-of-month' :day-of-month)
       :month (parse-value month :month)
       :day-of-week
       (->
        day-of-week'
        (parse-value :day-of-week)
        substitute-zero-day-of-week)}]
      cron)
    (catch Exception _ nil)))
