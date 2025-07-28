(ns dda.c4k-common.common-spec-test
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.common :as cut]))

(deftest should-refuse-illegal-inputs
  (is (thrown? Exception
               (cut/concat-vec ["a1" "a2"] "b1")))
  (is (thrown? Exception
               (cut/concat-vec ["a1" "a2"] 2)))
  (is (thrown? Exception
               (cut/concat-vec {"a1" "a2"} []))))