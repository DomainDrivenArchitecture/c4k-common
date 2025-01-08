(defproject org.domaindrivenarchitecture/c4k-common-cljs "8.1.1-SNAPSHOT"
  :description "Contains predicates and tools for c4k"
  :url "https://domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.12.0" :scope "provided"]
                 [aero "1.1.6"]
                 [orchestra "2021.01.01-1"]
                 [expound "0.9.0"]]
  :source-paths ["src/main/cljc"
                 "src/main/cljs"]
  :resource-paths ["src/main/resources"]
  :repositories [["snapshots" :clojars]
                 ["releases" :clojars]]
  :deploy-repositories [["snapshots" {:sign-releases false :url "https://clojars.org/repo"}]
                        ["releases" {:sign-releases false :url "https://clojars.org/repo"}]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "v" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]])
