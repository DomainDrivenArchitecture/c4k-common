(ns dda.c4k-common.postgres
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.namespace :as ns]
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
                   ::pvc-storage-class-name ::pv-storage-size-gb ::ns/namespace]))
(def pg-auth?
  (s/keys :opt-un [::postgres-db-user ::postgres-db-password]))

(def ^{:deprecated "11.0.0"} postgres-function
  (s/keys :opt-un [::deserializer ::optional]))

(def default-config (merge ns/default-config
                           {:postgres-image "postgres:16"
                            :postgres-size :2gb
                            :db-name "postgres"
                            :postgres-data-volume-path "/var/postgres"
                            :pv-storage-size-gb 20
                            :pvc-storage-class-name "local-path"}))

(defn-spec generate-configmap map?
  [& config (s/? pg-config?)]
  (let [final-config (merge default-config
                            (first config))]
    (int/generate-configmap final-config)))


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
  ([auth pg-auth?]
   (let [final-config default-config]
     (int/generate-secret final-config auth)))
  ([config pg-config?
    auth pg-auth?]
   (let [final-config (merge default-config
                             config)]
     (int/generate-secret final-config auth))))


(defn-spec generate-service map?
  [config pg-config?]
  (let [final-config (merge default-config
                            config)]
    (int/generate-service final-config)))

(defn-spec config-objects seq?
  [config pg-config?]
  (let [resolved-config (merge default-config
                               config)]
  [(generate-configmap resolved-config)
   (when (contains? resolved-config :postgres-data-volume-path)
     (generate-persistent-volume
      (select-keys resolved-config [:postgres-data-volume-path :pv-storage-size-gb])))
   (generate-pvc resolved-config)
   (generate-deployment resolved-config)
   (generate-service resolved-config)]))

(defn-spec auth-objects seq?
  [config pg-config?
   auth pg-auth?]
  (let [resolved-config (merge default-config
                            config)]
    [(int/generate-secret resolved-config auth)]))

(defn-spec ^{:deprecated "11.0.0"} generate-config seq?
  "use config-objects instead"
  [config pg-config?]
  (config-objects config))

(defn-spec ^{:deprecated "11.0.0"} generate-auth seq?
  "use config-auth instead"
  [config pg-config?
   auth pg-auth?]
  (auth-objects config auth))