(defproject lacinia-gen "0.1.0-SNAPSHOT"
  :description "Generators for GraphQL"
  :url "https://github.com/oliyh/lacinia-gen"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/test.check "0.10.0-alpha2"]
                 [com.walmartlabs/lacinia "0.27.0"]
                 [org.clojure/clojurescript "1.10.312"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-doo "0.1.8"]]

  :profiles {:dev {:dependencies [[lein-doo "0.1.10"]]}}

  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src" "test"]
                        :compiler {:output-to "target/unit-test.js"
                                   :main "lacinia-gen.cljs-runner"
                                   :optimizations :whitespace
                                   :parallel-build true}}]}
  :aliases {"test" ["do" ["clean"] ["test"] ["doo" "nashorn" "test" "once"]]})
