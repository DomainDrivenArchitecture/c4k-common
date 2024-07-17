(ns dda.c4k-common.postgres.postgres-internal
  (:require
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.namespace :as ns]))


#?(:cljs
   (defmethod yaml/load-resource :postgres [resource-name]
     (case resource-name
       "postgres/config-2gb.yaml"        (rc/inline "postgres/config-2gb.yaml")
       "postgres/config-4gb.yaml"        (rc/inline "postgres/config-4gb.yaml")
       "postgres/config-8gb.yaml"        (rc/inline "postgres/config-8gb.yaml")
       "postgres/config-16gb.yaml"       (rc/inline "postgres/config-16gb.yaml")
       "postgres/deployment.yaml"        (rc/inline "postgres/deployment.yaml")
       "postgres/persistent-volume.yaml" (rc/inline "postgres/persistent-volume.yaml")
       "postgres/pvc.yaml"               (rc/inline "postgres/pvc.yaml")
       "postgres/secret.yaml"            (rc/inline "postgres/secret.yaml")
       "postgres/service.yaml"           (rc/inline "postgres/service.yaml")
       (throw (js/Error. (str "Undefined Resource: " resource-name))))))


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
                   ::pvc-storage-class-name ::pv-storage-size-gb ::ns/namespace]))

(def pg-auth?
  (s/keys :req-un [::postgres-db-user ::postgres-db-password]))


(def postgres-function (s/keys :opt-un [::deserializer ::optional]))


(defn-spec generate-config-configmap map?
  [config pg-config?]
  (let [{:keys [postgres-size db-name namespace]} config]
    (->
     (yaml/from-string (yaml/load-resource
                        (str "postgres/config-" (name postgres-size) ".yaml")))
     (assoc-in [:metadata :namespace] namespace)
     (assoc-in [:data :postgres-db] db-name))))

(defn-spec ^{:deprecated "6.4.1"} generate-config map?
  "use generate-config-configmap instead"
  [config pg-config?]
  (let [{:keys [postgres-size db-name namespace]} config]
    (->
     (yaml/from-string (yaml/load-resource
                        (str "postgres/config-" (name postgres-size) ".yaml")))
     (assoc-in [:metadata :namespace] namespace)
     (assoc-in [:data :postgres-db] db-name))))


(defn-spec generate-deployment map?
  [config pg-config?]
  (let [{:keys [postgres-image namespace]} config]
    (->
     (yaml/from-string (yaml/load-resource "postgres/deployment.yaml"))
     (assoc-in [:metadata :namespace] namespace)
     (assoc-in [:spec :template :spec :containers 0 :image] postgres-image))))


(defn-spec generate-persistent-volume map?
  [config pg-config?]
  (let [{:keys [postgres-data-volume-path pv-storage-size-gb namespace]} config]
    (->
     (yaml/from-string (yaml/load-resource "postgres/persistent-volume.yaml"))
     (assoc-in [:metadata :namespace] namespace)
     (assoc-in [:spec :hostPath :path] postgres-data-volume-path)
     (assoc-in [:spec :capacity :storage] (str pv-storage-size-gb "Gi")))))


(defn-spec generate-pvc map?
  [config pg-config?]
  (let [{:keys [pv-storage-size-gb pvc-storage-class-name namespace]} config]
  (-> 
   (yaml/from-string (yaml/load-resource "postgres/pvc.yaml"))
   (assoc-in [:metadata :namespace] namespace)
   (assoc-in [:spec :resources :requests :storage] (str pv-storage-size-gb "Gi"))
   (assoc-in [:spec :storageClassName] (name pvc-storage-class-name)))))


(defn-spec generate-secret map? 
  [config pg-config?
   auth pg-auth?]
  (let [{:keys [namespace]} config
        {:keys [postgres-db-user postgres-db-password]} auth]
    (->
     (yaml/from-string (yaml/load-resource "postgres/secret.yaml"))
     (assoc-in [:metadata :namespace] namespace)
     (cm/replace-key-value :postgres-user (b64/encode postgres-db-user))
     (cm/replace-key-value :postgres-password (b64/encode postgres-db-password)))))


(defn-spec generate-service map? 
  [config pg-config?]
  (let [{:keys [namespace]} config]
  (->
   (yaml/from-string (yaml/load-resource "postgres/service.yaml"))
   (assoc-in [:metadata :namespace] namespace))))
