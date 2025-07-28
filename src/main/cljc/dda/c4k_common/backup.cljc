(ns dda.c4k-common.backup
  (:require
   [clojure.spec.alpha :as s]
   [orchestra.core :refer [defn-spec]]
   [dda.c4k-common.namespace :as ns]
   [dda.c4k-common.backup.backup-internal :as int]))

(s/def ::mount-name ::int/mount-name)
(s/def ::pvc-name ::int/pvc-name)
(s/def ::mount-path ::int/mount-path)

(s/def ::app-name ::int/app-name)
(s/def ::backup-image ::int/backup-image)
(s/def ::backup-postgres ::int/backup-postgres)
(s/def ::backup-volume-mount ::int/backup-volume-mount)
(s/def ::aws-access-key-id ::int/aws-access-key-id)
(s/def ::aws-secret-access-key ::int/aws-secret-access-key)
(s/def ::restic-password ::int/restic-password)
(s/def ::restic-new-password ::int/restic-new-password)
(s/def ::restic-repository ::int/restic-repository)

(s/def ::config int/config?)
(s/def ::auth int/auth?)

(def default-config
  (merge ns/default-config
         {:backup-postgres false}))

(defn-spec config-objects seq?
  [config ::config]
  (let [resolved-config (merge default-config
                               config)]
    [(int/config resolved-config)
     (int/backup-restore-deployment resolved-config)
     (int/backup-cron resolved-config)]))

(defn-spec auth-objects seq?
  [config ::config
   auth ::auth]
  (let [resolved-config (merge default-config
                               config)]
    [(int/secret resolved-config auth)]))