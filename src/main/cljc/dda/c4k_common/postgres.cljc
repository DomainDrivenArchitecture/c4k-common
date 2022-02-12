(ns dda.c4k-common.postgres
  (:require
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.common :as cm]))

(defn postgres-size?
  [input]
  (contains? #{:2gb :4gb :8gb :16gb} input))

(defn postgres-image?
  [input]
  (contains? #{"postgres:13" "postgres:14"} input))

(s/def ::postgres-db-user cp/bash-env-string?)
(s/def ::postgres-db-password cp/bash-env-string?)
(s/def ::postgres-data-volume-path string?)
(s/def ::postgres-size postgres-size?)
(s/def ::db-name cp/bash-env-string?)
(s/def ::pvc-storage-class-name cp/pvc-storage-class-name?)
(s/def ::pv-storage-size-gb pos?)
(defn pg-config? [input]
  (s/keys :opt-un [::postgres-size ::db-name ::postgres-data-volume-path 
                   ::pvc-storage-class-name ::pv-storage-size-gb]))
(defn pg-auth? [input]
  (s/keys :opt-un [::postgres-db-user ::postgres-db-password]))

(def postgres-function (s/keys :opt-un [::deserializer ::optional]))

#?(:cljs
   (defmethod yaml/load-resource :postgres [resource-name]
     (case resource-name
       "postgres/config-2gb.yaml" (rc/inline "postgres/config-2gb.yaml")
       "postgres/config-4gb.yaml" (rc/inline "postgres/config-4gb.yaml")
       "postgres/config-8gb.yaml" (rc/inline "postgres/config-8gb.yaml")
       "postgres/config-16gb.yaml" (rc/inline "postgres/config-16gb.yaml")
       "postgres/deployment.yaml" (rc/inline "postgres/deployment.yaml")
       "postgres/persistent-volume.yaml" (rc/inline "postgres/persistent-volume.yaml")
       "postgres/pvc.yaml" (rc/inline "postgres/pvc.yaml")
       "postgres/secret.yaml" (rc/inline "postgres/secret.yaml")
       "postgres/service.yaml" (rc/inline "postgres/service.yaml")
       (throw (js/Error. "Undefined Resource!")))))

(defn-spec generate-config cp/map-or-seq?
  [& config (s/? pg-config?)]
  (let [{:keys [postgres-size db-name]
         :or {postgres-size :2gb
              db-name "postgres"}} (first config)]
    (->
     (yaml/from-string (yaml/load-resource
                        (str "postgres/config-" (name postgres-size) ".yaml")))
     (assoc-in [:data :postgres-db] db-name))))

; TODO: why do we need a sequence of configs?
(defn-spec generate-deployment cp/map-or-seq?
  [& config (s/? pg-config?)]
  (let [{:keys [postgres-image]
         :or {postgres-image "postgres:13"}} (first config)]
    (->
     (yaml/from-string (yaml/load-resource "postgres/deployment.yaml"))
     (assoc-in [:spec :template :spec :containers 0 :image] postgres-image))))

(defn-spec generate-persistent-volume cp/map-or-seq?
  [config pg-config?]
  (let [{:keys [postgres-data-volume-path pv-storage-size-gb] 
         :or {postgres-data-volume-path "/var/postgres"
              pv-storage-size-gb 10}} config]
    (->
     (yaml/from-string (yaml/load-resource "postgres/persistent-volume.yaml"))
     (assoc-in [:spec :hostPath :path] postgres-data-volume-path)
     (assoc-in [:spec :capacity :storage] (str pv-storage-size-gb "Gi")))))

(defn-spec generate-pvc cp/map-or-seq? 
  [config pg-config?]
  (let [{:keys [pv-storage-size-gb pvc-storage-class-name] 
         :or {pv-storage-size-gb 10
              pvc-storage-class-name :manual}} config]
  (-> 
   (yaml/from-string (yaml/load-resource "postgres/pvc.yaml"))
   (assoc-in [:spec :resources :requests :storage] (str pv-storage-size-gb "Gi"))
   (assoc-in [:spec :storageClassName] (name pvc-storage-class-name)))))

(defn-spec generate-secret cp/map-or-seq? 
  [my-auth any?]
  (let [{:keys [postgres-db-user postgres-db-password]} my-auth]
    (->
     (yaml/from-string (yaml/load-resource "postgres/secret.yaml"))
     (cm/replace-key-value :postgres-user (b64/encode postgres-db-user))
     (cm/replace-key-value :postgres-password (b64/encode postgres-db-password)))))

(defn-spec generate-service cp/map-or-seq? 
  []
  (yaml/from-string (yaml/load-resource "postgres/service.yaml")))
