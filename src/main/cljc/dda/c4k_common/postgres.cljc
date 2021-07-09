(ns dda.c4k-common.postgres
  (:require
   [clojure.spec.alpha :as s]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.common :as cm]))

(s/def ::postgres-db-user cm/bash-env-string?)
(s/def ::postgres-db-password cm/bash-env-string?)
(s/def ::postgres-data-volume-path string?)

(defn generate-config []
   (yaml/from-string (yaml/load-resource "postgres/config.yaml")))

(defn generate-deployment []
  (yaml/from-string (yaml/load-resource "postgres/deployment.yaml")))

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
