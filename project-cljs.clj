(defproject org.domaindrivenarchitecture/c4k-common-cljs "0.4.0"
  :description "Contains predicates and tools for c4k"
  :url "https://domaindrivenarchitecture.org"
  :license {:name "Apache License, Version 2.0"
            :url "https://www.apache.org/licenses/LICENSE-2.0.html"}
  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [aero "1.1.6"]
                 [orchestra "2021.01.01-1"]
                 [expound "0.8.9"]]
  :source-paths ["src/main/cljc"
                 "src/main/cljs"]
  :resource-paths ["src/main/resources"]
  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :sign-releases false}]]
  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]])
