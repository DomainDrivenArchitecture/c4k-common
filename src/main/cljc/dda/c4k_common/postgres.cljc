(ns dda.c4k-common.postgres
  (:require
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:cljs [dda.c4k-common.macros :refer-macros [inline-resources]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.postgres.postgres-internal :as int]))

(def postgres-size? int/postgres-size?)

(def postgres-image? int/postgres-image?)

(s/def ::postgres-db-user ::int/postgres-db-user)
(s/def ::postgres-db-password ::int/postgres-db-password)
(s/def ::postgres-data-volume-path ::int/postgres-data-volume-path)
(s/def ::postgres-size ::int/postgres-size)
(s/def ::db-name ::int/db-name)
(s/def ::pvc-storage-class-name ::int/pvc-storage-class-name)
(s/def ::pv-storage-size-gb ::int/pv-storage-size-gb)

(def pg-config?
  (s/keys :opt-un [::postgres-size ::db-name ::postgres-data-volume-path
                   ::pvc-storage-class-name ::pv-storage-size-gb]))
(def pg-auth?
  (s/keys :opt-un [::postgres-db-user ::postgres-db-password]))

(def postgres-function (s/keys :opt-un [::deserializer ::optional]))

(def default-config {:postgres-image "postgres:13"
                     :postgres-size :2gb
                     :db-name "postgres"
                     :postgres-data-volume-path "/var/postgres"
                     :pv-storage-size-gb 10
                     :pvc-storage-class-name "manual"})


(defn-spec generate-config map?
  [& config (s/? pg-config?)]
  (let [final-config (merge default-config
                            (first config))]
    (int/generate-config final-config)))


(defn-spec generate-deployment map?
  [& config (s/? pg-config?)]
  (let [final-config (merge default-config
                            (first config))]
    (int/generate-deployment final-config)))


(defn-spec generate-persistent-volume map?
  [config pg-config?]
  (let [final-config (merge default-config
                            config)]
    (int/generate-persistent-volume final-config)))


(defn-spec generate-pvc map? 
  [config pg-config?]
  (let [final-config (merge default-config
                             config)]
     (int/generate-pvc final-config)))


(defn-spec generate-secret map?
  [my-auth pg-auth?]
  (int/generate-secret my-auth))


(defn-spec generate-service map?
  []
  (int/generate-service))
