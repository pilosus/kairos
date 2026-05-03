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

(ns org.pilosus.kairos-cron-to-text-test
  (:require [clojure.test :refer [deftest is testing]]
            [org.pilosus.kairos :as kairos])
  (:import (java.time ZonedDateTime ZoneId)))

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

(def params-value->text
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

(deftest test-value->text
  (testing "Test parsing values in a field"
    (doseq [[s field expected description] params-value->text]
      (testing description
        (is (= expected (kairos/value->text s field)))))))

(def params-cron->text-simple-ok
  [["* * * * *"
    "every minute"
    "All stars value"]
   ["*/5 * * * *"
    "every 5 minutes"
    "Minutes with step only"]
   ["*/25 * * * *"
    "every 25 minutes"
    "Double-digit minutes with step only"]
   ["0 */2 * * *"
    "every 2 hours"
    "Hours with step only"]
   ["0 */12 * * *"
    "every 12 hours"
    "Double-digit hours with step only"]
   ["0 9 * * *"
    "every day at 9:00"
    "Fixed time-only"]
   ["0 9 * * 1-5"
    "every weekday at 9:00"
    "Fixed time, all days, all months, specific day of week"]
   ["0 9 * * 1-5"
    "every weekday at 9:00"
    "Fixed time, all days, all months, specific day of week"]
   ["0 9 * * 6,7"
    "every weekend at 9:00"
    "Fixed time, all days, all months, specific day of week"]
   ["30 8 15 * *"
    "every 15th of the month at 8:30"
    "Fixed time, specific day of month, all months, all days of week"]
   ["0 0 25 12 *"
    "every December 25th at midnight"
    "Fixed time, specific day of month, specific month, all days of week"]
   ["30 8 * * *"
    "every day at 8:30"
    "Fixed time, all days, all months, all days of week"]])

(deftest test-cron->text-simple-ok
  (testing "Test parsing crontab entry into a human-readable text"
    (doseq [[cron expected description] params-cron->text-simple-ok]
      (testing cron
        (is (= expected (kairos/cron->text cron)) description)))))

(def params-cron->text-complex-ok
  [["3-17 * * * *"
    "at every minute from 3 through 17, past every hour, on every day, in every month"
    "Simple range value"]
   ["10-20/2 * * * *"
    "at every 2nd minute from 10 through 20, past every hour, on every day, in every month"
    "Explicit range with the step value"]
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

(deftest test-cron->text-complex-ok
  (testing "Test parsing crontab entry into a human-readable text"
    (doseq [[cron expected description] params-cron->text-complex-ok]
      (testing cron
        (is (= expected (kairos/cron->text cron)) description)))))
