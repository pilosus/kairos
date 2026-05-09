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
   See org.pilosus.kairos/locale-en for the full schema."
  (:require [clojure.string :as string]))

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

(def ^:private es-month-names-genitive
  {1 "enero" 2 "febrero" 3 "marzo" 4 "abril"
   5 "mayo" 6 "junio" 7 "julio" 8 "agosto"
   9 "septiembre" 10 "octubre" 11 "noviembre" 12 "diciembre"})

(defn- es-list-join
  "Join list values with commas and 'y' before the last item, prefixing with plural field name."
  [field-name-plural parsed-values]
  (let [n (count parsed-values)]
    (if (= n 1)
      (first parsed-values)
      (str field-name-plural " "
           (string/join ", " (butlast parsed-values))
           " y " (last parsed-values)))))

(def es
  {:day-names          {1 "lunes" 2 "martes" 3 "miércoles" 4 "jueves"
                        5 "viernes" 6 "sábado" 7 "domingo"}
   :month-names        {1 "enero" 2 "febrero" 3 "marzo" 4 "abril"
                        5 "mayo" 6 "junio" 7 "julio" 8 "agosto"
                        9 "septiembre" 10 "octubre" 11 "noviembre" 12 "diciembre"}
   :field-names        {:minute "minuto" :hour "hora"
                        :day-of-month "día del mes" :month "mes"
                        :day-of-week "día de la semana"}
   :field-fmts
   {:minute       {:fmt/every-unit  "cada minuto"
                   :fmt/every-nth   "cada %s minutos"
                   :fmt/unit-value  "%s"
                   :fmt/range       "cada minuto entre el %s y el %s"
                   :fmt/range-step  "cada %s minutos entre el %s y el %s"
                   :fmt/list-fn     (fn [vals] (es-list-join "minutos" vals))}
    :hour         {:fmt/every-unit  "cada hora"
                   :fmt/every-nth   "cada %s horas"
                   :fmt/unit-value  "%s"
                   :fmt/range       "cada hora entre las %s y las %s"
                   :fmt/range-step  "cada %s horas entre las %s y las %s"
                   :fmt/list-fn     (fn [vals] (es-list-join "horas" vals))}
    :day-of-month {:fmt/every-unit  "cada día"
                   :fmt/every-nth   "cada %s días"
                   :fmt/unit-value  "%s"
                   :fmt/range       "días del mes del %s al %s"
                   :fmt/range-step  "cada %s días del mes del %s al %s"
                   :fmt/list-fn     (fn [vals] (es-list-join "días" vals))}
    :month        {:fmt/every-unit  "cada mes"
                   :fmt/every-nth   "cada %s meses"
                   :fmt/unit-value  "%s"
                   :fmt/range       "de %s a %s"
                   :fmt/range-step  "cada %s meses de %s a %s"
                   :fmt/list-fn     (fn [vals] (es-list-join "meses" vals))}
    :day-of-week  {:fmt/every-unit  "cada día"
                   :fmt/every-nth   "cada %s días"
                   :fmt/unit-value  "%s"
                   :fmt/range       "cada día de %s a %s"
                   :fmt/range-step  "cada %s días de %s a %s"
                   :fmt/list-fn     (fn [vals] (es-list-join "días" vals))}}
   :nicknames          {"@yearly"   "cada 1 de enero a medianoche"
                        "@annually" "cada 1 de enero a medianoche"
                        "@monthly"  "el 1 de cada mes a medianoche"
                        "@weekly"   "cada domingo a medianoche"
                        "@daily"    "cada día a medianoche"
                        "@hourly"   "cada hora"}
   :ordinal-fn         (fn [n] (str n))
   :time-fn            (fn [h m]
                         (cond
                           (and (= h "0") (= m "0")) "medianoche"
                           (and (re-matches #"\d+" m) (re-matches #"\d+" h))
                           (let [mi (Integer/parseInt m)]
                             (if (zero? mi)
                               (format "las %s:00" h)
                               (format "las %s:%02d" h mi)))
                           :else nil))
   :midnight           "medianoche"
   :every-day          "cada día"
   :every-minute       "cada minuto"
   :weekday            "día laborable"
   :weekend            "fin de semana"
   :fmt/verbose        "%s de %s, %s, %s"
   :fmt/verbose-fn     (fn [{:keys [minute hour day month
                                    day-of-month-raw day-of-week-raw month-raw]}]
                         (let [has-dow (not= day-of-week-raw "*")
                               has-dom (not= day-of-month-raw "*")
                               has-month (not= month-raw "*")
                               ;; When only day-of-week or both dom+dow are specified, omit month
                               omit-month (or (and has-dow (not has-dom))
                                              (and has-dow has-dom))
                               day-text (cond
                                          ;; Both day fields are *, use "todos los días" unless month is specified
                                          (and (= day-of-month-raw "*")
                                               (= day-of-week-raw "*")
                                               (not has-month)) "todos los días"
                                          ;; Both day fields are * but month is specified, use "cada día"
                                          (and (= day-of-month-raw "*")
                                               (= day-of-week-raw "*")
                                               has-month) "cada día"
                                          :else day)]
                           (if omit-month
                             (str minute " de " hour ", " day-text)
                             (str minute " de " hour ", " day-text ", "
                                  (if has-month month "todos los meses")))))
   :fmt/or             "%s o %s"
   :fmt/or-fn          (fn [dom-raw dow-raw locale]
                         (let [day-names (:day-names locale)
                               dom-m (re-matcher #"(\d+)-(\d+)" dom-raw)
                               dow-m (re-matcher #"(\d+)-(\d+)" dow-raw)]
                           (when (and (.matches dom-m) (.matches dow-m))
                             (let [dom-start (.group dom-m 1)
                                   dom-end (.group dom-m 2)
                                   dow-start (get day-names (Integer/parseInt (.group dow-m 1)))
                                   dow-end (get day-names (Integer/parseInt (.group dow-m 2)))]
                               (format "días del %s al %s del mes o de %s a %s"
                                       dom-start dom-end dow-start dow-end)))))
   :fmt/every-unit     "cada %s"
   :fmt/every-nth      "cada %s %s"
   :fmt/unit-value     "%s %s"
   :fmt/range          "cada %s de %s a %s"
   :fmt/range-step     "cada %s %s de %s a %s"
   :fmt/every-n-minutes    "cada %s minutos"
   :fmt/every-n-hours      "cada %s horas"
   :fmt/every-dow-at       "cada %s a %s"
   :fmt/every-dom-at       "el %s de cada mes a %s"
   :fmt/every-month-dom-at (fn [{:keys [day-of-month month time]}]
                             (let [month-name (get es-month-names-genitive
                                                   (Integer/parseInt month))]
                               (format "cada %s de %s a %s"
                                       day-of-month month-name time)))
   :fmt/every-day-at       "cada día a %s"})

(def fr
  {:day-names          {1 "lundi" 2 "mardi" 3 "mercredi" 4 "jeudi"
                        5 "vendredi" 6 "samedi" 7 "dimanche"}
   :month-names        {1 "janvier" 2 "février" 3 "mars" 4 "avril"
                        5 "mai" 6 "juin" 7 "juillet" 8 "août"
                        9 "septembre" 10 "octobre" 11 "novembre" 12 "décembre"}
   :field-names        {:minute "minute" :hour "heure"
                        :day-of-month "jour du mois" :month "mois"
                        :day-of-week "jour de la semaine"}
   :field-fmts
   {:minute       {:fmt/every-unit  "chaque minute"
                   :fmt/every-nth   "toutes les %s minutes"
                   :fmt/unit-value  "minute %s"
                   :fmt/range       "chaque minute de %s à %s"
                   :fmt/range-step  "toutes les %s minutes de %s à %s"}
    :hour         {:fmt/every-unit  "chaque heure"
                   :fmt/every-nth   "toutes les %s heures"
                   :fmt/unit-value  "heure %s"
                   :fmt/range       "chaque heure de %s à %s"
                   :fmt/range-step  "toutes les %s heures de %s à %s"}
    :day-of-month {:fmt/every-unit  "chaque jour du mois"
                   :fmt/every-nth   "tous les %s jours du mois"
                   :fmt/unit-value  "jour du mois %s"
                   :fmt/range       "chaque jour du mois de %s à %s"
                   :fmt/range-step  "tous les %s jours du mois de %s à %s"}
    :month        {:fmt/every-unit  "chaque mois"
                   :fmt/every-nth   "tous les %s mois"
                   :fmt/unit-value  "mois %s"
                   :fmt/range       "chaque mois de %s à %s"
                   :fmt/range-step  "tous les %s mois de %s à %s"}
    :day-of-week  {:fmt/every-unit  "chaque jour de la semaine"
                   :fmt/every-nth   "tous les %s jours de la semaine"
                   :fmt/unit-value  "jour de la semaine %s"
                   :fmt/range       "chaque jour de la semaine de %s à %s"
                   :fmt/range-step  "tous les %s jours de la semaine de %s à %s"}}
   :nicknames          {"@yearly"   "chaque 1er janvier à minuit"
                        "@annually" "chaque 1er janvier à minuit"
                        "@monthly"  "le 1er de chaque mois à minuit"
                        "@weekly"   "chaque dimanche à minuit"
                        "@daily"    "chaque jour à minuit"
                        "@hourly"   "chaque heure"}
   :ordinal-fn         (fn [n] (if (= n 1) (str n "er") (str n)))
   :time-fn            (fn [h m]
                         (cond
                           (and (= h "0") (= m "0")) "minuit"
                           (and (re-matches #"\d+" m) (re-matches #"\d+" h))
                           (format "%sh%02d" h (Integer/parseInt m))
                           :else nil))
   :midnight           "minuit"
   :every-day          "chaque jour"
   :every-minute       "chaque minute"
   :weekday            "jour ouvrable"
   :weekend            "week-end"
   :fmt/verbose        "%s, %s, %s, %s"
   :fmt/or             "%s ou %s"
   :fmt/every-unit     "chaque %s"
   :fmt/every-nth      "chaque %s %s"
   :fmt/unit-value     "%s %s"
   :fmt/range          "chaque %s de %s à %s"
   :fmt/range-step     "chaque %s %s de %s à %s"
   :fmt/every-n-minutes    "toutes les %s minutes"
   :fmt/every-n-hours      "toutes les %s heures"
   :fmt/every-dow-at       "chaque %s à %s"
   :fmt/every-dom-at       "le %s de chaque mois à %s"
   :fmt/every-month-dom-at (fn [{:keys [day-of-month month time]}]
                             (let [month-names {1 "janvier" 2 "février" 3 "mars" 4 "avril"
                                                5 "mai" 6 "juin" 7 "juillet" 8 "août"
                                                9 "septembre" 10 "octobre" 11 "novembre" 12 "décembre"}
                                   month-name (get month-names (Integer/parseInt month))
                                   dom (let [n (Integer/parseInt day-of-month)]
                                         (if (= n 1) "1er" (str n)))]
                               (format "chaque %s %s à %s" dom month-name time)))
   :fmt/every-day-at       "chaque jour à %s"})

(def pt
  {:day-names          {1 "segunda-feira" 2 "terça-feira" 3 "quarta-feira" 4 "quinta-feira"
                        5 "sexta-feira" 6 "sábado" 7 "domingo"}
   :month-names        {1 "janeiro" 2 "fevereiro" 3 "março" 4 "abril"
                        5 "maio" 6 "junho" 7 "julho" 8 "agosto"
                        9 "setembro" 10 "outubro" 11 "novembro" 12 "dezembro"}
   :field-names        {:minute "minuto" :hour "hora"
                        :day-of-month "dia do mês" :month "mês"
                        :day-of-week "dia da semana"}
   :field-fmts
   {:minute       {:fmt/every-unit  "a cada minuto"
                   :fmt/every-nth   "a cada %s minutos"
                   :fmt/unit-value  "minuto %s"
                   :fmt/range       "a cada minuto de %s a %s"
                   :fmt/range-step  "a cada %s minutos de %s a %s"}
    :hour         {:fmt/every-unit  "a cada hora"
                   :fmt/every-nth   "a cada %s horas"
                   :fmt/unit-value  "hora %s"
                   :fmt/range       "a cada hora de %s a %s"
                   :fmt/range-step  "a cada %s horas de %s a %s"}
    :day-of-month {:fmt/every-unit  "cada dia do mês"
                   :fmt/every-nth   "cada %s dias do mês"
                   :fmt/unit-value  "dia do mês %s"
                   :fmt/range       "cada dia do mês de %s a %s"
                   :fmt/range-step  "cada %s dias do mês de %s a %s"}
    :month        {:fmt/every-unit  "cada mês"
                   :fmt/every-nth   "cada %s meses"
                   :fmt/unit-value  "mês %s"
                   :fmt/range       "cada mês de %s a %s"
                   :fmt/range-step  "cada %s meses de %s a %s"}
    :day-of-week  {:fmt/every-unit  "cada dia da semana"
                   :fmt/every-nth   "cada %s dias da semana"
                   :fmt/unit-value  "dia da semana %s"
                   :fmt/range       "cada dia da semana de %s a %s"
                   :fmt/range-step  "cada %s dias da semana de %s a %s"}}
   :nicknames          {"@yearly"   "todo dia 1 de janeiro à meia-noite"
                        "@annually" "todo dia 1 de janeiro à meia-noite"
                        "@monthly"  "todo dia 1 de cada mês à meia-noite"
                        "@weekly"   "todo domingo à meia-noite"
                        "@daily"    "todo dia à meia-noite"
                        "@hourly"   "a cada hora"}
   :ordinal-fn         (fn [n] (str n))
   :time-fn            (fn [h m]
                         (cond
                           (and (= h "0") (= m "0")) "à meia-noite"
                           (and (re-matches #"\d+" m) (re-matches #"\d+" h))
                           (let [mi (Integer/parseInt m)]
                             (if (zero? mi)
                               (format "às %s:00" h)
                               (format "às %s:%02d" h mi)))
                           :else nil))
   :midnight           "à meia-noite"
   :every-day          "cada dia"
   :every-minute       "a cada minuto"
   :weekday            "dia útil"
   :weekend            "fim de semana"
   :fmt/verbose        "%s, %s, %s, %s"
   :fmt/or             "%s ou %s"
   :fmt/every-unit     "cada %s"
   :fmt/every-nth      "cada %s %s"
   :fmt/unit-value     "%s %s"
   :fmt/range          "cada %s de %s a %s"
   :fmt/range-step     "cada %s %s de %s a %s"
   :fmt/every-n-minutes    "a cada %s minutos"
   :fmt/every-n-hours      "a cada %s horas"
   :fmt/every-dow-at       "todo %s %s"
   :fmt/every-dom-at       "todo dia %s de cada mês %s"
   :fmt/every-month-dom-at (fn [{:keys [day-of-month month time]}]
                             (let [month-names {1 "janeiro" 2 "fevereiro" 3 "março" 4 "abril"
                                                5 "maio" 6 "junho" 7 "julho" 8 "agosto"
                                                9 "setembro" 10 "outubro" 11 "novembro" 12 "dezembro"}
                                   month-name (get month-names (Integer/parseInt month))]
                               (format "todo dia %s de %s %s"
                                       day-of-month month-name time)))
   :fmt/every-day-at       "todo dia %s"})

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
