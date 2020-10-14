(defproject clotter "0.1.0-SNAPSHOT"
  :description "CLOTTER: A thin twitter wrapper"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [metosin/compojure-api "2.0.0-alpha30"]
                 [org.clojure/core.async "0.4.490"]
                 [javax.servlet/javax.servlet-api "3.1.0"]
                 ; API
                 [clj-http "3.10.3"]
                 [clojure.java-time "0.3.2"]
                 [ring/ring-mock "0.3.2"]
                 [cheshire "5.10.0"]
                 [ring/ring-jetty-adapter "1.8.1"]
                 [org.clojure/data.json "1.0.0"]
                 [environ "1.2.0"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/data.csv "1.0.0"]

                 ; Database
                 [toucan "1.15.1"]
                 [org.postgresql/postgresql "42.2.16"]

                 ; Test
                 [midje "1.9.9"]]

  :resource-paths ["resources/REBL-0.9.242.jar"]

  :ring {:handler       clotter.core/app
         :port          4000
         :nrepl         {:start? true
                         :port   56782}
         :auto-reload?  true
         :auto-refresh? true
         :async?        true
         :init          clotter.core/init-db}

  :main ^:skip-aot clotter.core

  :uberjar-name "server.jar"
  :uberjar {:source-paths ["src/clotter"]
            :omit-source  true
            :main         clotter.core
            :aot          [clotter.core]
            :uberjar-name "server.jar"}

  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies []
                       :plugins      [[lein-ring "0.12.5"]]}})
