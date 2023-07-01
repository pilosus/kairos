(ns build
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]))

(def lib 'org.pilosus/kairos)

(defn- get-version
  "FIXME: change MAJOR.MINOR parts manually if needed"
  [patch]
  (format "0.1.%s" patch))

(def version (get-version (b/git-count-revs nil)))
(def snapshot (get-version "9999-SNAPSHOT"))
(def class-dir "target/classes")

(defn test
  "Run all the tests"
  [opts]
  (let [basis (b/create-basis {:aliases [:test]})
        cmds (b/java-command
              {:basis basis
               :main 'clojure.main
               :main-args ["-m" "cognitect.test-runner"]})
        {:keys [exit]} (b/process cmds)]
    (when-not (zero? exit) (throw (ex-info "Tests failed" {}))))
  opts)

(defn- jar-opts
  [opts]
  (let [version (if (:snapshot opts) snapshot version)]
    (println "\nVersion:" version)
    (assoc opts
           :lib lib :version version
           :jar-file (format "target/%s-%s.jar" lib version)
           :scm {:tag (str "v" version)}
           :basis (b/create-basis {})
           :class-dir class-dir
           :target "target"
           :src-dirs ["src"]
           :src-pom "template/pom.xml")))

(defn ci
  "Run the CI pipeline of tests (and build the JAR)"
  [opts]
  (let [test-disable (:test-disable opts)]
    (when (not test-disable) (test opts)))
  (b/delete {:path "target"})
  (let [opts (jar-opts opts)]
    (println "\nWriting pom.xml...")
    (b/write-pom opts)
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
    (println "\nBuilding JAR...")
    (b/jar opts))
  opts)

(defn install
  "Install the JAR locally"
  [opts]
  (let [opts (jar-opts opts)]
    (b/install opts))
  opts)

(defn deploy
  "Deploy the JAR to Clojars"
  [opts]
  (let [{:keys [jar-file] :as opts} (jar-opts opts)]
    (dd/deploy {:installer :remote :artifact (b/resolve-path jar-file)
                :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))}))
  opts)
