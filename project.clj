(defproject org.domaindrivenarchitecture/c4k-common-clj "0.3.4-SNAPSHOT"
  :description "Contains predicates and tools for c4k"
  :url "https://domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [org.clojure/tools.reader "1.3.4"]
                 [aero "1.1.6"]
                 [orchestra "2021.01.01-1"]
                 [expound "0.8.9"]
                 [clj-commons/clj-yaml "0.7.106"]]
  :source-paths ["src/main/cljc"
                 "src/main/clj"]
  :resource-paths ["src/main/resources"]
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :sign-releases false}]]
  :profiles {:test {:test-paths ["src/test/clj"
                                 "src/test/cljc"]
                    :resource-paths ["src/test/resources"]
                    :dependencies [[dda/data-test "0.1.1"]]}
             :dev {:plugins [[lein-shell "0.5.0"]]}}
  :release-tasks [["test"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["change" "version" "leiningen.release/bump-version"]])
