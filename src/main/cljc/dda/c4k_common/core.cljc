(ns dda.c4k-common.core
  (:require
   [clojure.spec.alpha :as s]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.monitoring :as monitoring]))

(def config-defaults {})

(def config? (s/keys :req-un []
                     :opt-un []))

(def auth? (s/keys :req-un []
                   :opt-un []))

(defn k8s-objects [config auth]
  (let []
    (map yaml/to-string
         (filter #(not (nil? %))
                 (cm/concat-vec
                  (monitoring/generate config auth))))))