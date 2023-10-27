(ns dda.c4k-common.macros-test
  (:require 
   [clojure.test :refer [deftest is are testing run-tests]]
   [dda.c4k-common.macros :refer [inline-resources]]))

(deftest should-inline-resources
  (is (= 3 (count (inline-resources "ingress")))))