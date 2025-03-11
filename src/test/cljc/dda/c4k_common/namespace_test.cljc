(ns dda.c4k-common.namespace-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   #?(:cljs [shadow.resource :as rc])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.namespace :as cut]))

(st/instrument `cut/generate)
(st/instrument `cut/load-and-adjust-namespace)

#?(:cljs
   (defmethod yaml/load-resource :namespace-test [resource-name]
     (case resource-name
       "namespace-test/dummy.yaml" (rc/inline "namespace-test/dummy.yaml")
       (throw (js/Error. (str "Undefined Resource: " resource-name))))))

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