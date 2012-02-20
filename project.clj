(defproject lein-api-doc "0.1.0"
  :description "A simple leinigen plugin to JSON api documentation."
  :eval-in-leiningen true
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/tools.namespace "0.1.0"]]
  :dev-dependencies [[backtype/leiningen "1.6.2-SNAPSHOT"]]
  :api-doc-test true
  :api-doc [leiningen.views]
  )