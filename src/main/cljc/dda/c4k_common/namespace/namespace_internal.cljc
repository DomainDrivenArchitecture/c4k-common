(ns dda.c4k-common.namespace.namespace-internal
  (:require 
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]))


#?(:cljs
   (defmethod yaml/load-resource :namespace [resource-name]
     (case resource-name
       "namespace/namespace.yaml" (rc/inline "namespace/namespace.yaml")
       (throw (js/Error. (str "Undefined Resource: " resource-name))))))


(s/def ::namespace string?)

(def config? (s/keys :req-un [::namespace]
                     :opt-un []))

(defn-spec generate-namespace  map?
  [config config?]
  (let [{:keys [namespace]} config]
    (->
     (yaml/load-as-edn "namespace/namespace.yaml")
     (assoc-in [:metadata :name] namespace))))