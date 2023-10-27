(ns dda.c4k-common.macros-test
  (:require 
   [cljs.test :refer-macros [deftest is]]
   [dda.c4k-common.macros :refer-macros [inline-resources]]))

(deftest should-inline-resources
  (is (= 3 (count (inline-resources "ingress")))))
