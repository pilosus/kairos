(ns org.pilosus.kairos-graalvm-test
  "Smoke test entry point for GraalVM native-image verification.
   Not part of the library — only used by `make graalvm-test`."
  (:require [org.pilosus.kairos :as k])
  (:gen-class))

(defn -main [& _]
  (let [text (k/cron->text "*/5 * * * *")
        validation (k/cron-validate "0 10 * * Mon-Fri")
        dts (take 3 (k/cron->dt "@hourly" {:start (k/get-dt 2025 1 1 0 0)}))]
    (println text)
    (println validation)
    (doseq [dt dts]
      (println dt))
    (when (and (string? text)
               (:ok? validation)
               (= 3 (count dts)))
      (println "OK"))))
