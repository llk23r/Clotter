(defproject clotter "0.1.0-SNAPSHOT"
   :description "FIXME: write description"
   :dependencies [[org.clojure/clojure "1.10.0"]
                  [metosin/compojure-api "2.0.0-alpha30"]]
   :ring {:handler clotter.handler/app :nrepl {:start? true}}
   :uberjar-name "server.jar"
   :repl-options {:nrepl-middleware
                  [cider.nrepl.middleware.apropos/wrap-apropos
                   cider.nrepl.middleware.classpath/wrap-classpath
                   cider.nrepl.middleware.complete/wrap-complete
                   cider.nrepl.middleware.info/wrap-info
                   cider.nrepl.middleware.inspect/wrap-inspect
                   cider.nrepl.middleware.macroexpand/wrap-macroexpand
                   cider.nrepl.middleware.ns/wrap-ns
                   cider.nrepl.middleware.resource/wrap-resource
                   cider.nrepl.middleware.stacktrace/wrap-stacktrace
                   cider.nrepl.middleware.test/wrap-test
                   cider.nrepl.middleware.trace/wrap-trace
                   cider.nrepl.middleware.undef/wrap-undef]}
   :profiles {:dev {:dependencies [[javax.servlet/javax.servlet-api "3.1.0"]
                                   [clj-http "3.10.3"]
                                   [cider/cider-nrepl "0.25.3"]
                                   [ring/ring-mock "0.3.2"]
                                   [cheshire "5.10.0"]
                                   [ring/ring-jetty-adapter "1.8.1"]
                                   [org.clojure/data.json "1.0.0"]
                                   [environ "1.2.0"]]
                    :plugins [[lein-ring "0.12.5"]]}})