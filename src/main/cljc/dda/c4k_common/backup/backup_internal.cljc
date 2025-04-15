(ns dda.c4k-common.backup.backup-internal
  (:require
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.base64 :as b64]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.predicate :as p]
   [dda.c4k-common.namespace :as ns]))

(s/def ::mount-name string?)
(s/def ::pvc-name string?)
(s/def ::mount-path string?)

(s/def ::app-name string?)
(s/def ::image string?)
(s/def ::backup-postgres boolean?)
(s/def ::backup-volume-mount (s/keys :req-un [::mount-name ::pvc-name ::mount-path]))
(s/def ::aws-access-key-id p/bash-env-string?)
(s/def ::aws-secret-access-key p/bash-env-string?)
(s/def ::restic-password p/bash-env-string?)
(s/def ::restic-new-password p/bash-env-string?)
(s/def ::restic-repository p/bash-env-string?)

(def config? (s/keys :req-un [::ns/namespace
                              ::app-name
                              ::image
                              ::backup-postgres
                              ::restic-repository]
                     :opt-un [::backup-volume-mount]))

(def auth? (s/keys :req-un [::restic-password ::aws-access-key-id ::aws-secret-access-key]
                     :opt-un [::restic-new-password]))

#?(:cljs
   (defmethod yaml/load-resource :backup [resource-name]
     (case resource-name
       "backup/backup-restore-deployment.yaml" (rc/inline "backup/backup-restore-deployment.yaml")
       "backup/backup-cron.yaml"               (rc/inline "backup/backup-cron.yaml")
       "backup/secret.yaml"                    (rc/inline "backup/secret.yaml")
       "backup/config.yaml"                    (rc/inline "backup/config.yaml")
       (throw (js/Error. (str "Undefined Resource: " resource-name))))))

(defn-spec backup-volumes seq?
  [config config?]
  (let [{:keys [backup-volume-mount]} config
        backup-secret [{:name "backup-secret-volume",
                        :secret {:secretName "backup-secret"}}]]
    (if (some? backup-volume-mount) 
      (into backup-secret [{:name (:mount-name backup-volume-mount),
                            :persistentVolumeClaim {:claimName (:pvc-name backup-volume-mount)}}]) 
      backup-secret)))

(defn-spec backup-volume-mounts seq?
  [config config?]
  (let [{:keys [backup-volume-mount]} config
        backup-secret [{:name "backup-secret-volume",
                        :mountPath "/var/run/secrets/backup-secrets",
                        :readOnly true}]]
    (if (some? backup-volume-mount)
      (into backup-secret [{:name (:mount-name backup-volume-mount),
                            :mountPath (:mount-path backup-volume-mount)}])
      backup-secret)))

(defn-spec backup-env seq?
  [config config?]
  (let [{:keys [backup-postgres]} config
        postgres-env [{:name "POSTGRES_USER",
                       :valueFrom
                       {:secretKeyRef {:name "postgres-secret", :key "postgres-user"}}}
                      {:name "POSTGRES_PASSWORD",
                       :valueFrom
                       {:secretKeyRef {:name "postgres-secret", :key "postgres-password"}}}
                      {:name "POSTGRES_DB",
                       :valueFrom
                       {:configMapKeyRef {:name "postgres-config", :key "postgres-db"}}}
                      {:name "POSTGRES_HOST", :value "postgresql-service:5432"}
                      {:name "POSTGRES_SERVICE", :value "postgresql-service"}
                      {:name "POSTGRES_PORT", :value "5432"}]
        restic-env [{:name "AWS_DEFAULT_REGION", :value "eu-central-1"}
                    {:name "AWS_ACCESS_KEY_ID_FILE",
                     :value "/var/run/secrets/backup-secrets/aws-access-key-id"}
                    {:name "AWS_SECRET_ACCESS_KEY_FILE",
                     :value "/var/run/secrets/backup-secrets/aws-secret-access-key"}
                    {:name "RESTIC_REPOSITORY",
                     :valueFrom
                     {:configMapKeyRef {:name "backup-config", :key "restic-repository"}}}
                    {:name "RESTIC_PASSWORD_FILE",
                     :value "/var/run/secrets/backup-secrets/restic-password"}
                    {:name "RESTIC_NEW_PASSWORD_FILE",
                     :value "/var/run/secrets/backup-secrets/restic-new-password"}]]
    (if backup-postgres (into postgres-env restic-env) restic-env)))

(defn-spec backup-restore-deployment map?
  [config config?]
  (let [{:keys [namespace app-name image]} config]
    (->
     (ns/load-and-adjust-namespace "backup/backup-restore-deployment.yaml" namespace)
     (assoc-in [:metadata :labels :app.kubernetes.io/part-of] app-name)
     (assoc-in [:spec :template :metadata :labels :app.kubernetes.io/part-of] app-name)
     (assoc-in [:spec :template :spec :containers 0 :image] image)     
     (assoc-in [:spec :template :spec :containers 0 :env] (backup-env config))
     (assoc-in [:spec :template :spec :containers 0 :volumeMounts] (backup-volume-mounts config))
     (assoc-in [:spec :template :spec :volumes] (backup-volumes config)))))

(defn-spec backup-cron map?
  [config config?]
  (let [{:keys [namespace app-name image]} config]
    (->
     (ns/load-and-adjust-namespace "backup/backup-cron.yaml" namespace)
     (assoc-in [:metadata :labels :app.kubernetes.io/part-of] app-name)
     (assoc-in [:spec :jobTemplate :spec :template :spec :containers 0 :image] image)
     (assoc-in [:spec :jobTemplate :spec :template :spec :containers 0 :env] (backup-env config))
     (assoc-in [:spec :jobTemplate :spec :template :spec :containers 0 :volumeMounts] (backup-volume-mounts config))
     (assoc-in [:spec :jobTemplate :spec :template :spec :volumes] (backup-volumes config)))))

(defn-spec config map?
  [config config?]
  (let [{:keys [restic-repository namespace app-name]} config]
    (->
     (ns/load-and-adjust-namespace "backup/config.yaml" namespace)
     (assoc-in [:metadata :labels :app.kubernetes.io/part-of] app-name)
     (cm/replace-key-value :restic-repository restic-repository))))

(defn-spec secret map?
  [config config?
   auth auth?]
  (let [{:keys [namespace app-name]} config
        {:keys [aws-access-key-id aws-secret-access-key
                restic-password restic-new-password]} auth]
    (as-> 
     (ns/load-and-adjust-namespace "backup/secret.yaml" namespace) res
      (assoc-in res [:metadata :labels :app.kubernetes.io/part-of] app-name)
      (cm/replace-key-value res :#:app.kubernetes.io:part-of app-name)
      (cm/replace-key-value res :aws-access-key-id (b64/encode aws-access-key-id))
      (cm/replace-key-value res :aws-secret-access-key (b64/encode aws-secret-access-key))
      (cm/replace-key-value res :restic-password (b64/encode restic-password))
      (if (contains? auth :restic-new-password)
        (assoc-in res [:data :restic-new-password] (b64/encode restic-new-password))
        res))))