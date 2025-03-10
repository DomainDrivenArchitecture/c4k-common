(ns dda.c4k-common.namespace
  (:require 
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [clojure.spec.alpha :as s]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.namespace.namespace-internal :as int]))

(s/def ::namespace ::int/namespace)

(def config? (s/keys :req-un []
                     :opt-un [::namespace]))

(def default-config {:namespace "default"})

(defn-spec generate seq?
  [config config?]
  (let [final-config (merge default-config
                            config)]
    [(int/generate-namespace final-config)]))

(defn-spec load-and-adjust-namespace cp/map-or-seq?
  [file string?
   namespace ::namespace]
  (->
   (yaml/load-as-edn file)
   (assoc-in [:metadata :namespace] namespace)))
