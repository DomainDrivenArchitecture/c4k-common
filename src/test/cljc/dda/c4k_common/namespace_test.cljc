(ns dda.c4k-common.namespace-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.namespace :as cut]))

(st/instrument `cut/generate)

(deftest should-generate-simple-ingress
  (is (= [{:apiVersion "v1" 
           :kind "Namespace"
           :metadata {:name "default"}}]
         (cut/generate {})))
  (is (= [{:apiVersion "v1"
           :kind "Namespace"
           :metadata {:name "myapp"}}]
         (cut/generate {:namespace "myapp"}))))