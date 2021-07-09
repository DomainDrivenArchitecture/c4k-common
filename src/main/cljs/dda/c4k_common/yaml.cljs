(ns dda.c4k-common.yaml
  (:require
   ["js-yaml" :as yaml]
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
