(ns dda.c4k_common.yaml
  (:require
   ["js-yaml" :as yaml]
   [shadow.resource :as rc]))

(defn load-resource [resource-name]
  (case resource-name
    "ingress_test.yaml" (rc/inline "ingress_test.yaml")
    (throw (js/Error. "Undefined Resource!"))))

(defn from-string [input]
  (js->clj (yaml/load input)
           :keywordize-keys true))

(defn to-string [edn]
  (yaml/dump (clj->js  edn)))