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

(ns org.pilosus.kairos-test
  (:require [clojure.test :refer :all]
            [org.pilosus.kairos :as kairos]))

;; Helpers

(deftest test-get-current-dt
  (testing "Test get current Date-Time"
    (is (= (type (kairos/get-current-dt)) java.time.ZonedDateTime))))

(def params-get-dt
  [[1969 12 31 23 59 java.time.ZonedDateTime "Date-Time after Epoch"]
   [1970 12 31 23 59 java.time.ZonedDateTime "Date-Time after Epoch"]
   [1292278994 12 31 23 59 nil "Invalid year"]
   [1970 15 31 23 59 nil "Invalid month"]
   [1970 12 35 23 59 nil "Invalid day of month"]
   [1970 12 31 27 59 nil "Invalid hour"]
   [1970 12 31 23 79 nil "Invalid minute"]])

(deftest test-get-dt
  (testing "Test creating Date-Time objects"
    (doseq [[year month day hour minute expected description] params-get-dt]
      (testing description
        (is (= expected (type (kairos/get-dt year month day hour minute))))))))

(def params-dt-valid?
  [[(kairos/get-dt 1970 12 31 0 0)
    []
    []
    false
    "Empty seqs"]
   [(kairos/get-dt 1970 12 31 0 0)
    [30, 31]
    []
    true
    "Days of month match, days of week empty"]
   [(kairos/get-dt 1970 12 31 0 0) ;; Thursday
    []
    [3, 4, 5] ;; Wed, Thu, Fri
    true
    "Days of week match, days of month empty"]
   [(kairos/get-dt 1970 12 31 0 0) ;; Thursday
    [30, 31]
    [3, 4, 5] ;; Wed, Thu, Fri
    true
    "Days of week match, days of month match"]
   [(kairos/get-dt 1970 12 31 0 0) ;; Thursday
    [15, 16]
    [3, 4, 5] ;; Wed, Thu, Fri
    true
    "Days of week match, days of month don't match"]
   [(kairos/get-dt 1970 12 31 0 0) ;; Thursday
    [31]
    [1, 2, 7] ;; Mon, Tue, Sun
    true
    "Days of week don't match, days of month match"]
   [(kairos/get-dt 1970 12 31 0 0) ;; Thursday
    [1, 2, 3]
    [1, 2, 7] ;; Mon, Tue, Sun
    false
    "Days of week don't match, days of month don't match"]
   [nil
    [1, 2, 3]
    [1, 2, 7] ;; Mon, Tue, Sun
    false
    "Date-Time is nil"]])

(deftest test-dt-valid?
  (testing "Test Date-Time validation"
    (doseq [[dt days-of-month days-of-week expected description] params-dt-valid?]
      (testing description
        (is (= expected (kairos/dt-valid? dt days-of-month days-of-week)))))))

;; Parsing cron into a map of ranges

(def params-cron->map-ok
  [["12,14,17,35-45/3 */2 27 2 *"
    {:minute '(12 14 17 35 38 41 44),
     :hour '(0 2 4 6 8 10 12 14 16 18 20 22),
     :day-of-month '(27),
     :month '(2),
     :day-of-week '()}
    "At minute 12, 14, 17, and every 3rd minute from 35 through 45 past every 2nd hour on day-of-month 27 in February"]
   ["39 9 * * wed-fri"
    {:minute '(39),
     :hour '(9),
     :day-of-month '(),
     :month '(1 2 3 4 5 6 7 8 9 10 11 12),
     :day-of-week '(3 4 5)}
    "At 09:39 on every day-of-week from Wednesday through Friday"]
   ["5 23 12 * Wed"
    {:minute '(5),
     :hour '(23),
     :day-of-month '(12),
     :month '(1 2 3 4 5 6 7 8 9 10 11 12),
     :day-of-week '(3)}
    "At 23:05 on day-of-month 12 and on Wednesday"]
   ["0 10 3,7 Dec Mon"
    {:minute '(0),
     :hour '(10),
     :day-of-month '(3 7),
     :month '(12),
     :day-of-week '(1)}
    "At 10:00, on 3rd and 7th or every Monday of December"]
   ["* * * * *"
    {:minute '(0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19
                 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39 40
                 41 42 43 44 45 46 47 48 49 50 51 52 53 54 55 56 57 58 59),
     :hour '(0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23),
     :day-of-month '(1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19
                       20 21 22 23 24 25 26 27 28 29 30 31),
     :month '(1 2 3 4 5 6 7 8 9 10 11 12),
     :day-of-week '(1 2 3 4 5 6 7)}
    "At every minute"]
   ["0 10 3,7 12 Tue-Sat * * *"
    {:minute '(0),
     :hour '(10),
     :day-of-month '(3 7),
     :month '(12),
     :day-of-week '(2 3 4 5 6)}
    "Excessive elements ignored"]])

(deftest test-cron->map-ok
  (testing "Test successful cron string parsing into a map:"
    (doseq [[crontab expected description] params-cron->map-ok]
      (testing crontab
        (is (= expected (kairos/cron->map crontab)) description)))))

(def params-cron->map-fail
  [["something */2 27 2 *"
    {:minute '(),
     :hour '(0 2 4 6 8 10 12 14 16 18 20 22),
     :day-of-month '(27),
     :month '(2),
     :day-of-week '()}
    "Minutes are incorrect"]
   ["what's up 3,7 Dec Mon"
    {:minute '(),
     :hour '(),
     :day-of-month '(3 7),
     :month '(12),
     :day-of-week '(1)}
    "Minutes and hours are incorrect"]
   ["crontab goes brrrr"
    nil
    "The whole crontab string is incorrect"]])

(deftest test-cron->map-fail
  (testing "Test failing to parse cron string"
    (doseq [[crontab expected description] params-cron->map-fail]
      (testing crontab
        (is (= (kairos/cron->map crontab) expected) description)))))

;; Parsing cron into a lazy seq of ZonedDateTime objects

(def params-cron->dt
  [["12,14,17,35-45/3 */2 27 2 *"
    (kairos/get-dt 1970 1 1 0 0)
    10
    [(kairos/get-dt 1970 2 27 0 12)
     (kairos/get-dt 1970 2 27 0 14)
     (kairos/get-dt 1970 2 27 0 17)
     (kairos/get-dt 1970 2 27 0 35)
     (kairos/get-dt 1970 2 27 0 38)
     (kairos/get-dt 1970 2 27 0 41)
     (kairos/get-dt 1970 2 27 0 44)
     (kairos/get-dt 1970 2 27 2 12)
     (kairos/get-dt 1970 2 27 2 14)
     (kairos/get-dt 1970 2 27 2 17)]
    "At minute 12, 14, 17, and every 3rd minute from 35 through 45 past every 2nd hour on day-of-month 27 in February"]
   ["39 9 * * wed-fri"
    (kairos/get-dt 1970 1 1 0 0)
    8
    [(kairos/get-dt 1970 1 1 9 39)
     (kairos/get-dt 1970 1 2 9 39)
     (kairos/get-dt 1970 1 7 9 39)
     (kairos/get-dt 1970 1 8 9 39)
     (kairos/get-dt 1970 1 9 9 39)
     (kairos/get-dt 1970 1 14 9 39)
     (kairos/get-dt 1970 1 15 9 39)
     (kairos/get-dt 1970 1 16 9 39)]
    "At 09:39 on every day-of-week from Wednesday through Friday"]
   ["0 10 3,7 Dec Mon"
    (kairos/get-dt 1970 1 1 0 0)
    10
    [(kairos/get-dt 1970 12 3 10 0)
     (kairos/get-dt 1970 12 7 10 0)
     (kairos/get-dt 1970 12 14 10 0)
     (kairos/get-dt 1970 12 21 10 0)
     (kairos/get-dt 1970 12 28 10 0)
     (kairos/get-dt 1971 12 3 10 0)
     (kairos/get-dt 1971 12 6 10 0)
     (kairos/get-dt 1971 12 7 10 0)
     (kairos/get-dt 1971 12 13 10 0)
     (kairos/get-dt 1971 12 20 10 0)]
    "At 10:00, on 3rd and 7th or every Monday of December"]
   ["0 0 * May 0-3"
    (kairos/get-dt 1970 1 1 0 0)
    8
    [(kairos/get-dt 1970 5 3 0 0)
     (kairos/get-dt 1970 5 4 0 0)
     (kairos/get-dt 1970 5 5 0 0)
     (kairos/get-dt 1970 5 6 0 0)
     (kairos/get-dt 1970 5 10 0 0)
     (kairos/get-dt 1970 5 11 0 0)
     (kairos/get-dt 1970 5 12 0 0)
     (kairos/get-dt 1970 5 13 0 0)]
    "At 00:00, Wednesday through Sunday of May"]
   ["0 0 * May 1,2,3,7"
    (kairos/get-dt 1970 1 1 0 0)
    8
    [(kairos/get-dt 1970 5 3 0 0)
     (kairos/get-dt 1970 5 4 0 0)
     (kairos/get-dt 1970 5 5 0 0)
     (kairos/get-dt 1970 5 6 0 0)
     (kairos/get-dt 1970 5 10 0 0)
     (kairos/get-dt 1970 5 11 0 0)
     (kairos/get-dt 1970 5 12 0 0)
     (kairos/get-dt 1970 5 13 0 0)]
    "At 00:00, Monday, Tuesday, Wednesday, Sunday of May"]
   ["* * * * *"
    (kairos/get-dt 1970 1 1 0 0)
    5
    [(kairos/get-dt 1970 1 1 0 1)
     (kairos/get-dt 1970 1 1 0 2)
     (kairos/get-dt 1970 1 1 0 3)
     (kairos/get-dt 1970 1 1 0 4)
     (kairos/get-dt 1970 1 1 0 5)]
    "At every minute"]])

(deftest test-cron->dt
  (testing "Test generate a sequence of Date-Time objects:"
    (doseq [[crontab dt quantity expected description] params-cron->dt]
      (testing crontab
        ;; redef is needed to override checks for future events only, see dt-future?
        (with-redefs [kairos/get-current-dt (constantly dt)]
          (let [result (take quantity (kairos/cron->dt crontab))]
            (is (= expected result) description)))))))

(deftest test-cron->dt-empty-years
  (testing "Test the for-loop as if years binding were empty to increase coverage"
    (with-redefs [clojure.core/iterate (constantly (range 2010 2021))]
      (let [result (take 10 (kairos/cron->dt "* * * * *"))]
        (is (= 0 (count result)))))))

;; Parsing cront into a human-readable text

(def params-value->ordinal
  [["1" "1st" "First"]
   ["2" "2nd" "Second"]
   ["3" "3rd" "Third"]
   ["4" "4th" "Fourth"]
   ["9" "9th" "Nineth"]
   ["11" "11th" "Eleventh"]
   ["12" "12th" "Twelfth"]
   ["13" "13th" "Thirteenth"]
   ["213" "213th" "Two hundred and thirteenth"]
   ["301" "301st" "Three hundred and first"]
   ["cannot parse" nil "Value cannot be parsed into integer"]])

(deftest test-value->ordinal
  (testing "Test parsing integer into a ordinal integer number with the suffix"
    (doseq [[value expected description] params-value->ordinal]
      (testing description
        (is (= expected (kairos/value->ordinal value)))))))

(def params-values->text
  [["" :minute nil "Empty"]
   ["-20" :minute nil "No start"]
   ["10-" :minute nil "No end"]
   ["/" :minute nil "No step"]
   ["a-z/c" :minute nil "Non-integers"]
   ["*" :minute "every minute" "Asterisk"]
   ["*/5" :minute "every 5th minute" "Asterisk with step"]
   ["10" :minute "minute 10" "Single value"]
   ["10-20" :minute "every minute from 10 through 20" "Range"]
   ["10-20/3" :minute "every 3rd minute from 10 through 20" "Range with step"]
   ["10-20/3" :no-such-field "every 3rd null from 10 through 20"
    "Non existent field name ignored"]])

(deftest test-values->text
  (testing "Test parsing values in a field"
    (doseq [[s field expected description] params-values->text]
      (testing description
        (is (= expected (kairos/values->text s field)))))))

(def params-cron->text-ok
  [["* * * * *"
    "at every minute, past every hour, on every day, in every month"
    "All asterisk values"]
   ["3-17 * * * *"
    "at every minute from 3 through 17, past every hour, on every day, in every month"
    "Simple range value"]
   ["10-20/2 * * * *"
    "at every 2nd minute from 10 through 20, past every hour, on every day, in every month"
    "Explicit range with the step value"]
   ["*/2 * * * *"
    "at every 2nd minute, past every hour, on every day, in every month"
    "Asterisk with the step value"]
   ["1,2,17 * * * *"
    "at minute 1, minute 2, minute 17, past every hour, on every day, in every month"
    "Simple list of values"]
   ["1,2,15-20,45-55/3 * * * *"
    "at minute 1, minute 2, every minute from 15 through 20, every 3rd minute from 45 through 55, past every hour, on every day, in every month"
    "Complex list of values"]
   ["* * 1-15 * *"
    "at every minute, past every hour, on every day of month from 1 through 15, in every month"
    "Day of month only"]
   ["* * * * 1-4"
    "at every minute, past every hour, on every day of week from Monday through Thursday, in every month"
    "Day of week only"]
   ["* * 1-15 * 1-4"
    "at every minute, past every hour, on every day of month from 1 through 15 or every day of week from Monday through Thursday, in every month"
    "Day of month OR day of week"]
   ["* * * Jan-May *"
    "at every minute, past every hour, on every day, in every month from January through May"
    "Named range month"]
   ["1,2,15-20,45-55/3 0,2,3-9/2 1,3,7-11/2 Jan,Mar,Jun,9-12/2 Mon-Wed,7"
    "at minute 1, minute 2, every minute from 15 through 20, every 3rd minute from 45 through 55, past hour 0, hour 2, every 2nd hour from 3 through 9, on day of month 1, day of month 3, every 2nd day of month from 7 through 11 or every day of week from Monday through Wednesday, day of week Sunday, in month January, month March, month June, every 2nd month from September through December"
    "Very complex crontab entry"]
   ["cannot be parsed"
    nil
    "Wrong value"]])

(deftest test-cron->text-ok
  (testing "Test parsing crontab entry into a human-readable text"
    (doseq [[cron expected description] params-cron->text-ok]
      (testing cron
        (is (= expected (kairos/cron->text cron)) description)))))
