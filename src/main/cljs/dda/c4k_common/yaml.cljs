(ns dda.c4k-common.yaml
  (:require
   ["js-yaml" :as yaml]
   [shadow.resource :as rc]
   [clojure.string :as st]))

(defn from-string [input]
  (js->clj (yaml/load input)
           :keywordize-keys true))

(defn to-string [edn]
  (yaml/dump (clj->js  edn)))

(defn dispatch-by-resource-name
  [resource]
  (keyword (first (st/split resource #"/"))))

(defmulti load-resource dispatch-by-resource-name)

(defmethod load-resource :test [resource-name]
  (case resource-name
    "test/ingress_test.yaml" (rc/inline "test/ingress_test.yaml")
    (throw (js/Error. "Undefined Resource!"))))