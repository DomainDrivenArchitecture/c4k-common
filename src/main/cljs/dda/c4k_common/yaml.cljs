(ns dda.c4k-common.yaml
  (:require
   ["js-yaml" :as yaml]
   [clojure.string :as st]
   [orchestra.core :refer-macros [defn-spec]]
   [shadow.resource :as rc]
   [dda.c4k-common.predicate :as cp]))

(defn string-or-keyword? [input]
  (or (string? input) (keyword? input)))

(defn-spec from-string cp/map-or-seq?
  [input string?]
  (js->clj (yaml/load input)
           :keywordize-keys true))

(defn-spec to-string string?
  [edn cp/map-or-seq?]
  (yaml/dump (clj->js  edn)))

(defn-spec dispatch-by-resource-name keyword?
  [resource string-or-keyword?]
  (keyword (first (st/split resource #"/"))))

(defmulti load-resource dispatch-by-resource-name)

(defmethod load-resource :default [allowed-resources resource-name]
  (if (some #(= % resource-name) allowed-resources)
    (rc/inline resource-name)
    (throw (js/Error. "Undefined Resource!"))))

(defmulti load-as-edn dispatch-by-resource-name)

(defmethod load-as-edn :default [resource-name]
  (let [allowed-resources (allowed-resources)]
   (from-string (load-resource allowed-resources resource-name))))

(defmulti allowed-resources dispatch-by-resource-name)

(defmethod allowed-resources :default []
  [])
