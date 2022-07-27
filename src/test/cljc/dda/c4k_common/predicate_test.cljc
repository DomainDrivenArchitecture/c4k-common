(ns dda.c4k-common.predicate-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]   
   [dda.c4k-common.predicate :as cut]))


(deftest test-bash-env-string?
  (is (true? (cut/bash-env-string? "abcd")))
  (is (false? (cut/bash-env-string? "$abdc")))
  (is (false? (cut/bash-env-string? "\"abdc"))))

