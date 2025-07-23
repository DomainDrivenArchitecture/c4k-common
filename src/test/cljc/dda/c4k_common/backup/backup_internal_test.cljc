(ns dda.c4k-common.backup.backup-internal-test
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.backup.backup-internal :as cut]))

(st/instrument `cut/secret)
(st/instrument `cut/config)
(st/instrument `cut/backup-restore-deployment)
(st/instrument `cut/backup-cron)

(def config {:namespace "ns"
             :app-name "app-name"
             :backup-image "image"
             :backup-postgres false
             :restic-repository "repo"})

(deftest should-generate-backup-cron
  (is (= {:name "backup-cron",
          :namespace "ns",
          :labels #:app.kubernetes.io{:name "backup-cron"
                                      :part-of "app-name"}}
         (:metadata (cut/backup-cron config))))
  (is (= "image"
         (get-in (cut/backup-cron config)
                 [:spec :jobTemplate :spec :template :spec :containers 0 :image]))))

(deftest should-generate-backup-cron-env
  (is (= [{:name "AWS_DEFAULT_REGION", :value "eu-central-1"}
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
           :value "/var/run/secrets/backup-secrets/restic-new-password"}]
         (get-in (cut/backup-cron config)
                 [:spec :jobTemplate :spec :template :spec :containers 0 :env])))
  (is (= [{:name "POSTGRES_USER",
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
          {:name "POSTGRES_PORT", :value "5432"}
          {:name "AWS_DEFAULT_REGION", :value "eu-central-1"}
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
           :value "/var/run/secrets/backup-secrets/restic-new-password"}]
         (get-in (cut/backup-cron (merge config
                                         {:backup-postgres true}))
                 [:spec :jobTemplate :spec :template :spec :containers 0 :env]))))

(deftest should-generate-backup-volume-mounts
  (is (= [{:name "backup-secret-volume",
           :mountPath "/var/run/secrets/backup-secrets",
           :readOnly true}]
         (get-in (cut/backup-cron config)
                 [:spec :jobTemplate :spec :template :spec :containers 0 :volumeMounts])))
  (is (= [{:name "backup-secret-volume",
           :mountPath "/var/run/secrets/backup-secrets",
           :readOnly true}
          {:name "forgejo-data-volume",
           :mountPath "/var/backups"}]
         (get-in (cut/backup-cron (merge config
                                                       {:backup-volume-mount
                                                        {:mount-name "forgejo-data-volume"
                                                         :pvc-name "forgejo-data-pvc"
                                                         :mount-path "/var/backups"}}))
                 [:spec :jobTemplate :spec :template :spec :containers 0 :volumeMounts])))
  (is (= [{:mountPath "/var/run/secrets/backup-secrets",
           :name "backup-secret-volume",
           :readOnly true}
          {:mountPath "/var/backups/prom", :name "prom-data-volume"}
          {:mountPath "/var/backups/grafana", :name "grafana-data-volume"}]
         (get-in (cut/backup-cron (merge config
                                         {:backup-volume-mounts
                                          [{:mount-name "prom-data-volume"
                                            :pvc-name "prom-data-pvc"
                                            :mount-path "/var/backups/prom"}
                                           {:mount-name "grafana-data-volume"
                                            :pvc-name "grafana-data-pvc"
                                            :mount-path "/var/backups/grafana"}]}))
                 [:spec :jobTemplate :spec :template :spec :containers 0 :volumeMounts]))))

(deftest should-generate-backup-volumes
  (is (= [{:name "backup-secret-volume",
           :secret {:secretName "backup-secret"}}]
         (get-in (cut/backup-cron config)
                 [:spec :jobTemplate :spec :template :spec :volumes])))
  (is (= [{:name "backup-secret-volume",
           :secret {:secretName "backup-secret"}}
          {:name "forgejo-data-volume",
           :persistentVolumeClaim {:claimName "forgejo-data-pvc"}}]
         (get-in (cut/backup-cron (merge config
                                         {:backup-volume-mount
                                          {:mount-name "forgejo-data-volume"
                                           :pvc-name "forgejo-data-pvc"
                                           :mount-path "/var/backups"}}))
                 [:spec :jobTemplate :spec :template :spec :volumes])))
  (is (= [{:name "backup-secret-volume", :secret {:secretName "backup-secret"}}
          {:name "prom-data-volume",
           :persistentVolumeClaim {:claimName "prom-data-pvc"}}
          {:name "grafana-data-volume",
           :persistentVolumeClaim {:claimName "grafana-data-pvc"}}]
          (get-in (cut/backup-cron (merge config
                                          {:backup-volume-mounts
                                           [{:mount-name "prom-data-volume"
                                             :pvc-name "prom-data-pvc"
                                             :mount-path "/var/backups/prom"}
                                            {:mount-name "grafana-data-volume"
                                             :pvc-name "grafana-data-pvc"
                                             :mount-path "/var/backups/grafana"}]}))
                  [:spec :jobTemplate :spec :template :spec :volumes]))))

(deftest should-generate-backup-restore-deployment
  (is (= {:name "backup-restore",
          :namespace "ns",
          :labels #:app.kubernetes.io{:name "backup-restore"
                                      :part-of "app-name"}}
         (:metadata (cut/backup-restore-deployment config))))
  (is (= {:labels #:app.kubernetes.io{:name "backup-restore"
                                      :part-of "app-name"}}
         (get-in (cut/backup-restore-deployment config)
                 [:spec :template :metadata])))
  (is (= "image"
         (get-in (cut/backup-restore-deployment config)
                 [:spec :template :spec :containers 0 :image]))))

(deftest should-generate-backup-restore-deployment-env
  (is (= [{:name "AWS_DEFAULT_REGION", :value "eu-central-1"}
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
           :value "/var/run/secrets/backup-secrets/restic-new-password"}]
         (get-in (cut/backup-restore-deployment config)
                 [:spec :template :spec :containers 0 :env])))
  (is (= [{:name "POSTGRES_USER",
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
          {:name "POSTGRES_PORT", :value "5432"}
          {:name "AWS_DEFAULT_REGION", :value "eu-central-1"}
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
           :value "/var/run/secrets/backup-secrets/restic-new-password"}]
         (get-in (cut/backup-restore-deployment (merge config
                                                       {:backup-postgres true}))
                 [:spec :template :spec :containers 0 :env]))))

(deftest should-generate-backup-restore-deployment-volume-mounts
  (is (= [{:name "backup-secret-volume",
           :mountPath "/var/run/secrets/backup-secrets",
           :readOnly true}]
         (get-in (cut/backup-restore-deployment config)
                 [:spec :template :spec :containers 0 :volumeMounts])))
  (is (= [{:name "backup-secret-volume",
           :mountPath "/var/run/secrets/backup-secrets",
           :readOnly true}
          {:name "forgejo-data-volume",
           :mountPath "/var/backups"}]
         (get-in (cut/backup-restore-deployment (merge config
                                                       {:backup-volume-mount 
                                                        {:mount-name "forgejo-data-volume"
                                                         :pvc-name "forgejo-data-pvc"
                                                         :mount-path "/var/backups"}}))
                 [:spec :template :spec :containers 0 :volumeMounts]))))

(deftest should-generate-backup-restore-deployment-volumes
  (is (= [{:name "backup-secret-volume", 
           :secret {:secretName "backup-secret"}}]
         (get-in (cut/backup-restore-deployment config)
                 [:spec :template :spec :volumes])))
  (is (= [{:name "backup-secret-volume",
           :secret {:secretName "backup-secret"}}
          {:name "forgejo-data-volume",
           :persistentVolumeClaim {:claimName "forgejo-data-pvc"}}]
         (get-in (cut/backup-restore-deployment (merge config
                                                       {:backup-volume-mount
                                                        {:mount-name "forgejo-data-volume"
                                                         :pvc-name "forgejo-data-pvc"
                                                         :mount-path "/var/backups"}}))
                 [:spec :template :spec :volumes]))))



(deftest should-generate-config
  (is (= {:apiVersion "v1",
          :kind "ConfigMap",
          :metadata
          {:name "backup-config",
           :namespace "ns",
           :labels #:app.kubernetes.io{:name "backup-config", 
                                       :part-of "app-name"}},
          :data {:restic-repository "repo"}}
         (cut/config config))))

(deftest should-generate-secret
  (is (= {:apiVersion "v1",
           :kind "Secret",
           :metadata {:name "backup-secret", :namespace "ns"
                      :labels #:app.kubernetes.io{:name "backup-secret", 
                                                  :part-of "app-name"}},
           :type "Opaque",
           :data
           {:aws-access-key-id "YWtp",
            :aws-secret-access-key "YXNhaw==",
            :restic-password "cnB3"}}
         (cut/secret config
                     {:restic-password "rpw"
                      :aws-access-key-id "aki"
                      :aws-secret-access-key "asak"})))
  (is (= {:apiVersion "v1",
          :kind "Secret",
          :metadata {:name "backup-secret", 
                     :namespace "ns"
                     :labels #:app.kubernetes.io{:name "backup-secret", 
                                                 :part-of "app-name"}},
          :type "Opaque",
          :data
          {:aws-access-key-id "YWtp",
           :aws-secret-access-key "YXNhaw==",
           :restic-new-password "bnJwdw=="
           :restic-password "cnB3"}}
         (cut/secret config
                     {:restic-password "rpw"
                      :restic-new-password "nrpw"
                      :aws-access-key-id "aki"
                      :aws-secret-access-key "asak"}))))
