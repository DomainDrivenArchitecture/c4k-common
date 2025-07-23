(ns dda.c4k-common.namespace-test
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.namespace :as cut]))

(st/instrument `cut/generate)
(st/instrument `cut/load-and-adjust-namespace)

(deftest should-generate-simple-ingress
  (is (= [{:apiVersion "v1" 
           :kind "Namespace"
           :metadata {:name "default"}}]
         (cut/generate {})))
  (is (= [{:apiVersion "v1"
           :kind "Namespace"
           :metadata {:name "myapp"}}]
         (cut/generate {:namespace "myapp"}))))

(deftest should-load-and-replace-ns
  (is (= {:apiVersion "v1"
          :kind "Dummy"
          :metadata {:name "dummy" 
                     :namespace "xy"}}
         (cut/load-and-adjust-namespace "namespace-test/dummy.yaml" "xy"))))