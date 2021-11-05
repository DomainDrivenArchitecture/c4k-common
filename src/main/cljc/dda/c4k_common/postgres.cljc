(ns dda.c4k-common.postgres
  (:require
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.common :as cm]))

(defn postgres-size?
  [input]
  (contains? #{:2gb :4gb :8gb :16gb} input))

(s/def ::postgres-db-user cp/bash-env-string?)
(s/def ::postgres-db-password cp/bash-env-string?)
(s/def ::postgres-data-volume-path string?)
(s/def ::postgres-size postgres-size?)
(s/def ::db-name cp/bash-env-string?)

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

(defn generate-config [& args]
  (let [{:keys [postgres-size db-name]
         :or {postgres-size :2gb
              db-name "postgres"}} args]
    (->
     (yaml/from-string (yaml/load-resource (str "postgres/config-" (name postgres-size) ".yaml")))
     (assoc-in [:data :postgres-db] db-name))))

(defn generate-deployment [& args]
  (let [{:keys [postgres-image]
         :or {postgres-image "postgres:13"}} args]
    (->
     (yaml/from-string (yaml/load-resource "postgres/deployment.yaml"))
     (assoc-in [:spec :template :spec :containers 0 :image] postgres-image))))

(defn generate-persistent-volume [config]
  (let [{:keys [postgres-data-volume-path]} config]
    (->
     (yaml/from-string (yaml/load-resource "postgres/persistent-volume.yaml"))
     (assoc-in [:spec :hostPath :path] postgres-data-volume-path))))

(defn generate-pvc []
  (yaml/from-string (yaml/load-resource "postgres/pvc.yaml")))

(defn generate-secret [my-auth]
  (let [{:keys [postgres-db-user postgres-db-password]} my-auth]
    (->
     (yaml/from-string (yaml/load-resource "postgres/secret.yaml"))
     (cm/replace-key-value :postgres-user (b64/encode postgres-db-user))
     (cm/replace-key-value :postgres-password (b64/encode postgres-db-password)))))

(defn generate-service []
  (yaml/from-string (yaml/load-resource "postgres/service.yaml")))
