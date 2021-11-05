(ns dda.c4k-common.yaml
  (:require
   ["js-yaml" :as yaml]
   [clojure.string :as st]
   [orchestra.core :refer-macros [defn-spec]]
   [dda.c4k-common.predicate :as cp]))

(defn-spec from-string cp/map-or-seq?
  [input string?]
  (js->clj (yaml/load input)
           :keywordize-keys true))

(defn-spec to-string string? 
  [edn cp/map-or-seq?]
  (yaml/dump (clj->js  edn)))

(defn-spec dispatch-by-resource-name keyword?
  [resource string?]
  (keyword (first (st/split resource #"/"))))

(defmulti load-resource dispatch-by-resource-name)
