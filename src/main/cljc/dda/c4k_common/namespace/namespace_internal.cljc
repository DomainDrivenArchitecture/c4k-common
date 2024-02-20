(ns dda.c4k-common.namespace.namespace-internal
  (:require 
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:cljs [dda.c4k-common.macros :refer-macros [inline-resources]])
   [dda.c4k-common.yaml :as yaml]))

(s/def ::namespace string?)

(def config? (s/keys :req-un [::namespace]
                     :opt-un []))

(defn-spec generate-namespace  map?
  [config config?]
  (let [{:keys [namespace]} config]
    (->
     (yaml/load-as-edn "namespace/namespace.yaml")
     (assoc-in [:metadata :name] namespace))))

#?(:cljs
   (defmethod yaml/load-resource :namespace [resource-name]
     (get (inline-resources "namespace") resource-name)))