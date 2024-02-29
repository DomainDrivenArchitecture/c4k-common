(defproject org.domaindrivenarchitecture/c4k-common-clj "6.2.3-SNAPSHOT"
  :description "Contains predicates and tools for c4k"
  :url "https://domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.11.1" :scope "provided"]
                 [org.clojure/tools.reader "1.4.0"]
                 [aero "1.1.6"]
                 [orchestra "2021.01.01-1"]
                 [expound "0.9.0"]
                 [clj-commons/clj-yaml "1.0.27"]]
  :target-path "target/%s/"
  :source-paths ["src/main/cljc"
                 "src/main/clj"]
  :resource-paths ["src/main/resources"]
  :repositories [["snapshots" :clojars]
                 ["releases" :clojars]]
  :deploy-repositories [["snapshots" {:sign-releases false :url "https://clojars.org/repo"}]
                        ["releases" {:sign-releases false :url "https://clojars.org/repo"}]]
  :profiles {:test {:test-paths ["src/test/clj"
                                 "src/test/cljc"]
                    :resource-paths ["src/test/resources"]
                    :dependencies [[dda/data-test "0.1.1"]]}
             :dev {:plugins [[lein-shell "0.5.0"]]}
             :uberjar {:aot :all
                       :main dda.c4k-common.uberjar
                       :uberjar-name "c4k-common-standalone.jar"
                       :dependencies [[org.clojure/tools.cli "1.1.230"]
                                      [ch.qos.logback/logback-classic "1.5.1"
                                       :exclusions [com.sun.mail/javax.mail]]
                                      [org.slf4j/jcl-over-slf4j "2.0.12"]]}}
  :release-tasks [["test"]
                  ["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v" "--no-sign"]
                  ["change" "version" "leiningen.release/bump-version"]])
