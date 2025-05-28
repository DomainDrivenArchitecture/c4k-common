(ns dda.c4k-common.core
  (:require
   [clojure.spec.alpha :as s]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.monitoring :as monitoring]))

(def config-defaults {})

(def config? (s/keys :req-un [::monitoring/mon-cfg]
                     :opt-un []))

(def auth? (s/keys :req-un [::monitoring/mon-auth]
                   :opt-un []))

(defn config-objects [config]
  (map yaml/to-string
       (cm/concat-vec
        (monitoring/config-objects config))))

(defn auth-objects [config auth]
  (map yaml/to-string
       (cm/concat-vec
        (monitoring/auth-objects (:mon-cfg config) (:mon-auth auth)))))