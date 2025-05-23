(ns dda.c4k-common.common-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.common :as cut]))

(st/instrument `cut/concat-vec)

(deftest should-concat-vec
  (is (= ["a1" "a2" "b1"]
         (cut/concat-vec ["a1" "a2"] ["b1"])))
  (is (= ["a1" "a2" "b1"]
         (cut/concat-vec ["a1"] ["a2"] ["b1"])))
  (is (= ["a1" "a2" "b1"]
         (cut/concat-vec '("a1" "a2") ["b1"])))
  (is (= ["a1" "a2" "b1"]
         (cut/concat-vec '("a1" "a2") '("b1"))))
  (is (= ["a1" "a2" "b1"]
         (cut/concat-vec '("a1" "a2") '() '("b1")))))

(deftest should-replace-map-value
  (testing "replce string"
    (is (= {:data ["realy long String contain replaced inside."]}
           (cut/replace-map-value 
            {:data ["realy long String contain TOREPLACE inside."]}
            [:data 0]
            "TOREPLACE"
            "replaced"))))
  (testing "replace vect"
    (is (= {:data ["realy" "long String" "contain replaced inside."]}
            (cut/replace-map-value
             {:data ["realy" 
                     "long String" 
                     "contain TOREPLACE inside."]}
             [:data]
             "TOREPLACE"
             "replaced")))))
