(ns dda.c4k-common.uberjar-test
  (:require
   [clojure.test :refer [deftest is testing run-tests]]
   [clojure.spec.alpha :as s]
   [dda.c4k-common.uberjar :as cut]))

(deftest should-validate-cli-args
  (testing "options"
    (is (s/valid? 
         ::cut/opts-and-params
         []))
    (is (s/valid?
         ::cut/opts-and-params
         ["-h"]))
    (is (not (s/valid?
              ::cut/opts-and-params
              ["-c" "-a"])))
    (is (not (s/valid?
              ::cut/opts-and-params
              ["-c" "--auth"])))
    (is (not (s/valid?
              ::cut/opts-and-params
              ["-c" "-cp"])))
    (is (not (s/valid?
              ::cut/opts-and-params
              ["-h" "-c" "-a"])))
    (is (not (s/valid?
              ::cut/opts-and-params
              ["-h" "-v"])))))

(deftest should-parse-cli-args
  (is (= {}
         (cut/parse-args [])))
  (is (= {:help true}
         (cut/parse-args ["-h"])))
  (is (= {:help true}
         (cut/parse-args ["--help"])))
  (is (= {:invalid true}
         (cut/parse-args ["-v" "-h"])))
  (is (= {:version true}
         (cut/parse-args ["-v"])))
  (is (= {:config-select ["auth"]}
         (cut/parse-args ["--auth"])))
  (is (= {:config-select []}
         (cut/parse-args ["-c"])))
  (is (= {:config-select ["part1" "part2"]}
         (cut/parse-args ["-cp" "part1,part2"])))
  (is (= {:config-select ["part1"]}
         (cut/parse-args ["-cp" "part1"])))
  (is (= {:config-select ["part1"],
          :config-file "conf.yaml",
          :auth-file "auth.yaml"}
         (cut/parse-args ["-cp" "part1" "conf.yaml" "auth.yaml"]))))