(ns dda.c4k-common.postgres.postgres-internal
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:cljs [dda.c4k-common.macros :refer-macros [inline-resources]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.postgres.postgres-internal :as int]))

(defn postgres-size?
  [input]
  (contains? #{:2gb :4gb :8gb :16gb} input))

(defn postgres-image?
  [input]
  (contains? #{"postgres:13" "postgres:14" "postgres:15" "postgres:16"} input))

(s/def ::postgres-db-user cp/bash-env-string?)
(s/def ::postgres-db-password cp/bash-env-string?)
(s/def ::postgres-data-volume-path string?)
(s/def ::postgres-size postgres-size?)
(s/def ::db-name cp/bash-env-string?)
(s/def ::pvc-storage-class-name cp/pvc-storage-class-name?)
(s/def ::pv-storage-size-gb pos?)

(def pg-config?
  (s/keys :req-un [::postgres-size ::db-name ::postgres-data-volume-path
                   ::pvc-storage-class-name ::pv-storage-size-gb]))
(def pg-auth?
  (s/keys :opt-un [::postgres-db-user ::postgres-db-password]))

(def postgres-function (s/keys :opt-un [::deserializer ::optional]))


(defn-spec generate-config map?
  [config pg-config?]
  (let [{:keys [postgres-size db-name]} config]
    (->
     (yaml/from-string (yaml/load-resource
                        (str "postgres/config-" (name postgres-size) ".yaml")))
     (assoc-in [:data :postgres-db] db-name))))


(defn-spec generate-deployment map?
  [config pg-config?]
  (let [{:keys [postgres-image]} config]
    (->
     (yaml/from-string (yaml/load-resource "postgres/deployment.yaml"))
     (assoc-in [:spec :template :spec :containers 0 :image] postgres-image))))


(defn-spec generate-persistent-volume map?
  [config pg-config?]
  (let [{:keys [postgres-data-volume-path pv-storage-size-gb]} config]
    (->
     (yaml/from-string (yaml/load-resource "postgres/persistent-volume.yaml"))
     (assoc-in [:spec :hostPath :path] postgres-data-volume-path)
     (assoc-in [:spec :capacity :storage] (str pv-storage-size-gb "Gi")))))


(defn-spec generate-pvc map?
  [config pg-config?]
  (let [{:keys [pv-storage-size-gb pvc-storage-class-name]} config]
  (-> 
   (yaml/from-string (yaml/load-resource "postgres/pvc.yaml"))
   (assoc-in [:spec :resources :requests :storage] (str pv-storage-size-gb "Gi"))
   (assoc-in [:spec :storageClassName] (name pvc-storage-class-name)))))


(defn-spec generate-secret map? 
  [my-auth pg-auth?]
  (let [{:keys [postgres-db-user postgres-db-password]} my-auth]
    (->
     (yaml/from-string (yaml/load-resource "postgres/secret.yaml"))
     (cm/replace-key-value :postgres-user (b64/encode postgres-db-user))
     (cm/replace-key-value :postgres-password (b64/encode postgres-db-password)))))


(defn-spec generate-service map? 
  []
  (yaml/from-string (yaml/load-resource "postgres/service.yaml")))


#?(:cljs
   (defmethod yaml/load-resource :postgres [resource-name]
      (get (inline-resources "postgres") resource-name)))
