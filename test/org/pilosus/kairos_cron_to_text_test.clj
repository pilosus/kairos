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
            [org.pilosus.kairos :as kairos]
            [org.pilosus.kairos.locale :as locale])
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
    "Fixed time, all days, all months, all days of week"]
   ["@weekly"
    "every Sunday at midnight"
    "Nickname @weekly"]])

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

;; Locale tests

(def params-cron->text-de-simple
  [["@yearly"
    "jeden 1. Januar um Mitternacht"
    "Nickname @yearly"]
   ["@annually"
    "jeden 1. Januar um Mitternacht"
    "Nickname @annually"]
   ["@monthly"
    "jeden 1. des Monats um Mitternacht"
    "Nickname @monthly"]
   ["@weekly"
    "jeden Sonntag um Mitternacht"
    "Nickname @weekly"]
   ["@daily"
    "jeden Tag um Mitternacht"
    "Nickname @daily"]
   ["@hourly"
    "jede Stunde"
    "Nickname @hourly"]
   ["* * * * *"
    "jede Minute"
    "Every minute"]
   ["*/5 * * * *"
    "alle 5 Minuten"
    "Every 5 minutes"]
   ["*/25 * * * *"
    "alle 25 Minuten"
    "Double-digit minutes with step"]
   ["0 */2 * * *"
    "alle 2 Stunden"
    "Every 2 hours"]
   ["0 */12 * * *"
    "alle 12 Stunden"
    "Double-digit hours with step"]
   ["0 9 * * *"
    "jeden Tag um 9:00"
    "Every day at 9:00"]
   ["30 8 * * *"
    "jeden Tag um 8:30"
    "Every day at 8:30"]
   ["0 0 * * *"
    "jeden Tag um Mitternacht"
    "Every day at midnight"]
   ["0 9 * * 1-5"
    "jeden Werktag um 9:00"
    "Every weekday at 9:00"]
   ["0 9 * * 6,7"
    "jedes Wochenende um 9:00"
    "Every weekend at 9:00"]
   ["0 9 * * 6,0"
    "jedes Wochenende um 9:00"
    "Every weekend at 9:00 (sun=0)"]
   ["30 8 15 * *"
    "jeden 15. des Monats um 8:30"
    "Every 15th of month at 8:30"]
   ["0 0 1 * *"
    "jeden 1. des Monats um Mitternacht"
    "Every 1st of the month at midnight"]
   ["0 0 25 12 *"
    "jeden 25. Dezember um Mitternacht"
    "Every December 25th at midnight"]
   ["0 0 1 1 *"
    "jeden 1. Januar um Mitternacht"
    "Every January 1st at midnight"]
   ["0 0 14 2 *"
    "jeden 14. Februar um Mitternacht"
    "Every February 14th at midnight"]
   ["0 12 3 10 *"
    "jeden 3. Oktober um 12:00"
    "Every October 3rd at noon"]])

(deftest test-cron->text-de-simple
  (testing "Test German locale simple patterns"
    (doseq [[cron expected description] params-cron->text-de-simple]
      (testing cron
        (is (= expected (kairos/cron->text cron {:locale locale/de}))
            description)))))

(def params-cron->text-de-complex
  [["3-17 * * * *"
    "in jeder Minute von 3 bis 17, in jeder Stunde, an jedem Tag, in jedem Monat"
    "Simple range value"]
   ["10-20/2 * * * *"
    "in jeder 2. Minute von 10 bis 20, in jeder Stunde, an jedem Tag, in jedem Monat"
    "Explicit range with step"]
   ["1,2,17 * * * *"
    "in Minute 1, Minute 2, Minute 17, in jeder Stunde, an jedem Tag, in jedem Monat"
    "Simple list of values"]
   ["* * 1-15 * *"
    "in jeder Minute, in jeder Stunde, an jedem Tag des Monats von 1 bis 15, in jedem Monat"
    "Day of month only"]
   ["* * * * 1-4"
    "in jeder Minute, in jeder Stunde, an jedem Wochentag von Montag bis Donnerstag, in jedem Monat"
    "Day of week only"]
   ["* * 1-15 * 1-4"
    "in jeder Minute, in jeder Stunde, an jedem Tag des Monats von 1 bis 15 oder an jedem Wochentag von Montag bis Donnerstag, in jedem Monat"
    "Day of month OR day of week"]
   ["* * * Jan-May *"
    "in jeder Minute, in jeder Stunde, an jedem Tag, in jedem Monat von Januar bis Mai"
    "Month range"]
   ["cannot be parsed"
    nil
    "Wrong value"]])

(deftest test-cron->text-de-complex
  (testing "Test German locale verbose patterns"
    (doseq [[cron expected description] params-cron->text-de-complex]
      (testing cron
        (is (= expected (kairos/cron->text cron {:locale locale/de}))
            description)))))

(def params-cron->text-ru-simple
  [["@yearly"
    "каждое 1 января в полночь"
    "Nickname @yearly"]
   ["@annually"
    "каждое 1 января в полночь"
    "Nickname @annually"]
   ["@monthly"
    "1-го числа каждого месяца в полночь"
    "Nickname @monthly"]
   ["@weekly"
    "каждое воскресенье в полночь"
    "Nickname @weekly"]
   ["@daily"
    "каждый день в полночь"
    "Nickname @daily"]
   ["@hourly"
    "каждый час"
    "Nickname @hourly"]
   ["* * * * *"
    "каждую минуту"
    "Every minute"]
   ["*/2 * * * *"
    "каждые 2 минуты"
    "Every 2 minutes"]
   ["*/5 * * * *"
    "каждые 5 минут"
    "Every 5 minutes"]
   ["*/3 * * * *"
    "каждые 3 минуты"
    "Every 3 minutes (form2 plural)"]
   ["*/21 * * * *"
    "каждые 21 минуту"
    "Every 21 minutes (form1 plural)"]
   ["*/25 * * * *"
    "каждые 25 минут"
    "Double-digit minutes with step"]
   ["0 */2 * * *"
    "каждые 2 часа"
    "Every 2 hours"]
   ["0 */5 * * *"
    "каждые 5 часов"
    "Every 5 hours"]
   ["0 */12 * * *"
    "каждые 12 часов"
    "Double-digit hours with step"]
   ["0 9 * * *"
    "каждый день в 9:00"
    "Every day at 9:00"]
   ["30 8 * * *"
    "каждый день в 8:30"
    "Every day at 8:30"]
   ["0 0 * * *"
    "каждый день в полночь"
    "Every day at midnight"]
   ["0 9 * * 1-5"
    "каждый будний день в 9:00"
    "Every weekday at 9:00"]
   ["0 9 * * 6,7"
    "каждый выходной в 9:00"
    "Every weekend at 9:00"]
   ["0 9 * * 6,0"
    "каждый выходной в 9:00"
    "Every weekend at 9:00 (sun=0)"]
   ["30 8 15 * *"
    "каждое 15-е число месяца в 8:30"
    "Every 15th of month at 8:30"]
   ["0 0 1 * *"
    "каждое 1-е число месяца в полночь"
    "Every 1st of the month at midnight"]
   ["0 0 25 12 *"
    "каждое 25 декабря в полночь"
    "Every December 25th at midnight"]
   ["0 0 1 1 *"
    "каждое 1 января в полночь"
    "Every January 1st at midnight"]
   ["0 0 14 2 *"
    "каждое 14 февраля в полночь"
    "Every February 14th at midnight"]
   ["0 12 3 10 *"
    "каждое 3 октября в 12:00"
    "Every October 3rd at noon"]])

(deftest test-cron->text-ru-simple
  (testing "Test Russian locale simple patterns"
    (doseq [[cron expected description] params-cron->text-ru-simple]
      (testing cron
        (is (= expected (kairos/cron->text cron {:locale locale/ru}))
            description)))))

(def params-cron->text-ru-complex
  [["3-17 * * * *"
    "в каждую минуту с 3 по 17, каждый час, каждый день, каждый месяц"
    "Simple range value"]
   ["10-20/2 * * * *"
    "в каждую 2-ю минуту с 10 по 20, каждый час, каждый день, каждый месяц"
    "Explicit range with step"]
   ["1,2,17 * * * *"
    "в минуту 1, минуту 2, минуту 17, каждый час, каждый день, каждый месяц"
    "Simple list of values"]
   ["* * 1-15 * *"
    "каждую минуту, каждый час, каждый день месяца с 1 по 15, каждый месяц"
    "Day of month only"]
   ["* * * * 1-4"
    "каждую минуту, каждый час, каждый день недели с понедельника по четверг, каждый месяц"
    "Day of week only"]
   ["* * 1-15 * 1-4"
    "каждую минуту, каждый час, каждый день месяца с 1 по 15 или каждый день недели с понедельника по четверг, каждый месяц"
    "Day of month OR day of week"]
   ["* * * Jan-May *"
    "каждую минуту, каждый час, каждый день, каждый месяц с января по май"
    "Month range"]
   ["cannot be parsed"
    nil
    "Wrong value"]])

(deftest test-cron->text-ru-complex
  (testing "Test Russian locale verbose patterns"
    (doseq [[cron expected description] params-cron->text-ru-complex]
      (testing cron
        (is (= expected (kairos/cron->text cron {:locale locale/ru}))
            description)))))

(deftest test-cron->text-partial-locale
  (testing "Partial locale overrides merge with English defaults"
    (is (= "every day at minuit"
           (kairos/cron->text "0 0 * * *" {:locale {:midnight "minuit"}})))
    (is (= "every day at midnight"
           (kairos/cron->text "0 0 * * *"))
        "Default English unchanged"))
  (testing "Custom time-fn"
    (is (= "every day at 09h00"
           (kairos/cron->text "0 9 * * *"
                              {:locale {:time-fn (fn [h m] (format "%02dh%02d"
                                                                   (Integer/parseInt h)
                                                                   (Integer/parseInt m)))}})))))

(deftest test-public-wrappers
  (testing "Public multimethod wrappers still work"
    (is (= "January" (kairos/value-fragment->text "1" :month)))
    (is (= "Monday" (kairos/value-fragment->text "1" :day-of-week)))
    (is (= "5" (kairos/value-fragment->text "5" :minute)))
    (is (= "every minute" (kairos/value->text "*" :minute)))
    (is (= "every minute, minute 5" (kairos/field->text "*,5" :minute)))))
