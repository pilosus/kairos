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

(ns org.pilosus.kairos.locale
  "Predefined locale maps for cron->text localization.
   See org.pilosus.kairos/locale-en for the full schema.")

(def de
  {:day-names          {1 "Montag" 2 "Dienstag" 3 "Mittwoch" 4 "Donnerstag"
                        5 "Freitag" 6 "Samstag" 7 "Sonntag"}
   :month-names        {1 "Januar" 2 "Februar" 3 "März" 4 "April"
                        5 "Mai" 6 "Juni" 7 "Juli" 8 "August"
                        9 "September" 10 "Oktober" 11 "November" 12 "Dezember"}
   :field-names        {:minute "Minute" :hour "Stunde"
                        :day-of-month "Tag des Monats" :month "Monat"
                        :day-of-week "Wochentag"}
   :field-articles     {:minute "jeder" :hour "jeder"
                        :day-of-month "jedem" :month "jedem"
                        :day-of-week "jedem"}
   :nicknames          {"@yearly"   "jeden 1. Januar um Mitternacht"
                        "@annually" "jeden 1. Januar um Mitternacht"
                        "@monthly"  "jeden 1. des Monats um Mitternacht"
                        "@weekly"   "jeden Sonntag um Mitternacht"
                        "@daily"    "jeden Tag um Mitternacht"
                        "@hourly"   "jede Stunde"}
   :ordinal-fn         (fn [n] (str n "."))
   :time-fn            nil
   :midnight           "Mitternacht"
   :every-day          "jedem Tag"
   :every-minute       "jede Minute"
   :weekday            "jeden Werktag"
   :weekend            "jedes Wochenende"
   :fmt/verbose        "in %s, in %s, an %s, in %s"
   :fmt/or             "%s oder an %s"
   :fmt/every-unit     "%s"
   :fmt/every-nth      "%s"
   :fmt/unit-value     "%s %s"
   :fmt/range          "%s von %s bis %s"
   :fmt/range-step     "%s von %s bis %s"
   :fmt/every-n-minutes    "alle %s Minuten"
   :fmt/every-n-hours      "alle %s Stunden"
   :fmt/every-dow-at       "%s um %s"
   :fmt/every-dom-at       "jeden %s des Monats um %s"
   :fmt/every-month-dom-at "jeden %2$s %1$s um %3$s"
   :fmt/every-day-at       "jeden Tag um %s"})

(defn- ru-plural
  "Russian plural form for a numeric string.
   Returns one of three forms based on standard Russian grammar rules:
     form1 - for 1, 21, 31, ... (but not 11)
     form2 - for 2-4, 22-24, 32-34, ... (but not 12-14)
     form5 - for 0, 5-20, 25-30, ...
   Example: (ru-plural \"2\" \"минуту\" \"минуты\" \"минут\") => \"минуты\""
  [s form1 form2 form5]
  (let [n (Integer/parseInt s)
        last-two (mod n 100)
        last-one (mod n 10)]
    (cond
      (<= 11 last-two 19) form5
      (= last-one 1) form1
      (<= 2 last-one 4) form2
      :else form5)))

(def ^:private ru-month-names-genitive
  {1 "января" 2 "февраля" 3 "марта" 4 "апреля"
   5 "мая" 6 "июня" 7 "июля" 8 "августа"
   9 "сентября" 10 "октября" 11 "ноября" 12 "декабря"})

(def ru
  {:day-names          {1 "понедельник" 2 "вторник" 3 "среда" 4 "четверг"
                        5 "пятница" 6 "суббота" 7 "воскресенье"}
   :day-names-genitive {1 "понедельника" 2 "вторника" 3 "среды" 4 "четверга"
                        5 "пятницы" 6 "субботы" 7 "воскресенья"}
   :month-names        {1 "январь" 2 "февраль" 3 "март" 4 "апрель"
                        5 "май" 6 "июнь" 7 "июль" 8 "август"
                        9 "сентябрь" 10 "октябрь" 11 "ноябрь" 12 "декабрь"}
   :month-names-genitive ru-month-names-genitive
   :field-names        {:minute "минута" :hour "час"
                        :day-of-month "день месяца" :month "месяц"
                        :day-of-week "день недели"}
   :field-fmts
   {:minute       {:fmt/every-unit  "каждую минуту"
                   :fmt/every-nth   "каждую %s минуту"
                   :fmt/unit-value  "минуту %s"
                   :fmt/range       "каждую минуту с %s по %s"
                   :fmt/range-step  "каждую %s минуту с %s по %s"}
    :hour         {:fmt/every-unit  "каждый час"
                   :fmt/every-nth   "каждый %s час"
                   :fmt/unit-value  "час %s"
                   :fmt/range       "каждый час с %s по %s"
                   :fmt/range-step  "каждый %s час с %s по %s"}
    :day-of-month {:fmt/every-unit  "каждый день месяца"
                   :fmt/every-nth   "каждый %s день месяца"
                   :fmt/unit-value  "день месяца %s"
                   :fmt/range       "каждый день месяца с %s по %s"
                   :fmt/range-step  "каждый %s день месяца с %s по %s"}
    :month        {:fmt/every-unit  "каждый месяц"
                   :fmt/every-nth   "каждый %s месяц"
                   :fmt/unit-value  "месяц %s"
                   :fmt/range       "каждый месяц с %s по %s"
                   :fmt/range-step  "каждый %s месяц с %s по %s"}
    :day-of-week  {:fmt/every-unit  "каждый день недели"
                   :fmt/every-nth   "каждый %s день недели"
                   :fmt/unit-value  "день недели %s"
                   :fmt/range       "каждый день недели с %s по %s"
                   :fmt/range-step  "каждый %s день недели с %s по %s"}}
   :field-ordinals {:minute       (fn [n] (str n "-ю"))
                    :hour         (fn [n] (str n "-й"))
                    :day-of-month (fn [n] (str n "-й"))
                    :month        (fn [n] (str n "-й"))
                    :day-of-week  (fn [n] (str n "-й"))}
   :nicknames          {"@yearly"   "каждое 1 января в полночь"
                        "@annually" "каждое 1 января в полночь"
                        "@monthly"  "1-го числа каждого месяца в полночь"
                        "@weekly"   "каждое воскресенье в полночь"
                        "@daily"    "каждый день в полночь"
                        "@hourly"   "каждый час"}
   :ordinal-fn         (fn [n] (str n "-е"))
   :time-fn            nil
   :midnight           "полночь"
   :every-day          "каждый день"
   :every-minute       "каждую минуту"
   :weekday            "будний день"
   :weekend            "выходной"
   :fmt/verbose        "%s, %s, %s, %s"
   :fmt/verbose-fn     (fn [{:keys [minute hour day month minute-raw]}]
                         (let [prefix (if (= minute-raw "*") "" "в ")]
                           (str prefix minute ", " hour ", " day ", " month)))
   :fmt/or             "%s или %s"
   :fmt/every-unit     "каждый %s"
   :fmt/every-nth      "каждый %s %s"
   :fmt/unit-value     "%s %s"
   :fmt/range          "каждый %s с %s по %s"
   :fmt/range-step     "каждый %s %s с %s по %s"
   :fmt/every-n-minutes    (fn [n] (format "каждые %s %s" n (ru-plural n "минуту" "минуты" "минут")))
   :fmt/every-n-hours      (fn [n] (format "каждые %s %s" n (ru-plural n "час" "часа" "часов")))
   :fmt/every-dow-at       "каждый %s в %s"
   :fmt/every-dom-at       "каждое %s число месяца в %s"
   :fmt/every-month-dom-at (fn [{:keys [day-of-month month time]}]
                             (let [month-gen (get ru-month-names-genitive
                                                  (Integer/parseInt month))]
                               (format "каждое %s %s в %s" day-of-month month-gen time)))
   :fmt/every-day-at       "каждый день в %s"})
