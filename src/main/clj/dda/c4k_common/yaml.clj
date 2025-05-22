(ns dda.c4k-common.yaml
  (:require
   [clojure.java.io :as io]
   [clj-yaml.core :as yaml]
   [clojure.string :as cs]
   [clojure.walk]
   [orchestra.core :refer [defn-spec]]
   [dda.c4k-common.predicate :as cp]))



(defn-spec resolve-lazy-sequences cp/map-or-seq?
  [lazy-seq cp/map-or-seq?]
  (clojure.walk/postwalk #(if (instance? clojure.lang.LazySeq %)
                            (into [] %)
                            %) lazy-seq))

(defn- from-string-internal [input & {:as opts}]
  (resolve-lazy-sequences (yaml/parse-string input opts)))

(defn-spec from-string cp/map-or-seq? 
  [input string?]
  (from-string-internal input))

(defn-spec to-string string?
  [edn cp/map-or-seq?]
  (yaml/generate-string edn :dumper-options {:flow-style :block}))

(defn-spec is-yaml? boolean?
  [filename string?]
  (or
   (cs/ends-with? filename ".yaml")
   (cs/ends-with? filename ".yml")))

(defn dispatch-by-resource-name 
  [resource]
  :clj)

(defmulti load-resource dispatch-by-resource-name)

(defmethod load-resource :clj [resource-name]
  (slurp (io/resource resource-name)))

(defn load-as-edn [resource-name & {:as opts}]
  (from-string-internal (load-resource resource-name) opts))