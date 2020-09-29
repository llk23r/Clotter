(defproject clotter "0.1.0-SNAPSHOT"
  :description "CLOTTER: A thin twitter wrapper"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [metosin/compojure-api "2.0.0-alpha30"]]

  :ring {:handler      clotter.core/-main
         :port         3000
         :nrepl        {:start? true
                        :port   56782}
         :auto-reload? true
         :auto-refresh? true}

  :main ^:skip-aot clotter.core

  :uberjar-name "server.jar"
  :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                  ; API
                                  [clj-http "3.10.3"]
                                  [clojure.java-time "0.3.2"]
                                  [ring/ring-mock "0.3.2"]
                                  [cheshire "5.10.0"]
                                  [ring/ring-jetty-adapter "1.8.1"]
                                  [org.clojure/data.json "1.0.0"]
                                  [environ "1.2.0"]

                                  ; Database
                                  [toucan "1.15.1"]
                                  [org.postgresql/postgresql "42.2.16"]]

                   :plugins      [[lein-ring "0.12.5"]]}})
