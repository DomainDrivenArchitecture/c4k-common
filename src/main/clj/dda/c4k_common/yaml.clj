(ns dda.c4k-common.yaml
  (:require
   [clojure.java.io :as io]
   [clj-yaml.core :as yaml]
   [clojure.walk]
   [orchestra.core :refer [defn-spec]]
   [orchestra.spec.test :as st]
   [clojure.spec.alpha :as s]))


(defn-spec cast-lazy-seq-to-vec map?
  [lazy-seq map?]
  (clojure.walk/postwalk #(if (instance? clojure.lang.LazySeq %)
                            (into [] %)
                            %) lazy-seq))

(defn-spec from-string map? [input string?]
  (cast-lazy-seq-to-vec (yaml/parse-string input)))

(defn-spec to-string string? [edn map?]
  (yaml/generate-string edn :dumper-options {:flow-style :block}))

(defn dispatch-by-resource-name 
  [resource]
  :clj)

(defmulti load-resource dispatch-by-resource-name)

(defmethod load-resource :clj [resource-name]
  (slurp (io/resource  resource-name)))

(st/instrument)