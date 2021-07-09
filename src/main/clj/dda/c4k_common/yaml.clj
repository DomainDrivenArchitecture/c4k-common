(ns dda.c4k-common.yaml
  (:require
   [clojure.java.io :as io]
   [clj-yaml.core :as yaml]
   [clojure.walk]))

(defn cast-lazy-seq-to-vec
  [lazy-seq]
  (clojure.walk/postwalk #(if (instance? clojure.lang.LazySeq %)
                            (into [] %)
                            %) lazy-seq))

(defmethod load-resource :clj [resource-name]
  (slurp (io/resource  resource-name)))

(defn from-string [input]
  (cast-lazy-seq-to-vec (yaml/parse-string input)))

(defn to-string [edn]
  (yaml/generate-string edn :dumper-options {:flow-style :block}))

(defn dispatch-by-resource-name 
  [resource])

(defmulti load-resource dispatch-by-resource-name)