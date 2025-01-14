(ns dda.c4k-common.postgres.postgres-internal-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.postgres.postgres-internal :as cut]))

(st/instrument `cut/generate-configmap)
(st/instrument `cut/generate-deployment)
(st/instrument `cut/generate-persistent-volume)
(st/instrument `cut/generate-pvc)
(st/instrument `cut/generate-secret)
(st/instrument `cut/generate-service)

(deftest should-generate-configmap
  (is (= {:name "postgres-config",
          :namespace "default"
          :labels {:app "postgres"}}
         (:metadata (cut/generate-configmap {:postgres-image "postgres:13"
                                             :postgres-size :2gb
                                             :db-name "postgres"
                                             :postgres-data-volume-path "/var/postgres"
                                             :pv-storage-size-gb 10
                                             :pvc-storage-class-name "manual"
                                             :namespace "default"}))))
  (is (= {:name "postgres-config",
          :namespace "myapp"
          :labels {:app "postgres"}}
         (:metadata (cut/generate-configmap {:postgres-image "postgres:13"
                                             :postgres-size :2gb
                                             :db-name "postgres"
                                             :postgres-data-volume-path "/var/postgres"
                                             :pv-storage-size-gb 10
                                             :pvc-storage-class-name "manual"
                                             :namespace "myapp"}))))
  (is (= {:postgres-db "postgres"
          :postgresql.conf
          "max_connections = 100\nwork_mem = 4MB\nshared_buffers = 512MB\n"}
         (:data (cut/generate-configmap {:postgres-image "postgres:13"
                                         :postgres-size :2gb
                                         :db-name "postgres"
                                         :postgres-data-volume-path "/var/postgres"
                                         :pv-storage-size-gb 10
                                         :pvc-storage-class-name "manual"
                                         :namespace "default"}))))
  (is (= {:postgres-db "postgres"
          :postgresql.conf
          "max_connections = 700\nwork_mem = 3MB\nshared_buffers = 2048MB\n"}
         (:data (cut/generate-configmap {:postgres-image "postgres:13"
                                         :postgres-size :8gb
                                         :db-name "postgres"
                                         :postgres-data-volume-path "/var/postgres"
                                         :pv-storage-size-gb 10
                                         :pvc-storage-class-name "manual"
                                         :namespace "default"}))))
  (is (= {:postgres-db "test"
          :postgresql.conf
          "max_connections = 100\nwork_mem = 4MB\nshared_buffers = 512MB\n"}
         (:data (cut/generate-configmap {:postgres-image "postgres:13"
                                         :postgres-size :2gb
                                         :db-name "test"
                                         :postgres-data-volume-path "/var/postgres"
                                         :pv-storage-size-gb 10
                                         :pvc-storage-class-name "manual"
                                         :namespace "default"})))))

(deftest should-generate-deployment
  (is (= [{:image "postgres:14"
           :name "postgresql"
           :env
           [{:name "POSTGRES_USER"
             :valueFrom
             {:secretKeyRef
              {:name "postgres-secret", :key "postgres-user"}}}
            {:name "POSTGRES_PASSWORD"
             :valueFrom
             {:secretKeyRef
              {:name "postgres-secret", :key "postgres-password"}}}
            {:name "POSTGRES_DB"
             :valueFrom
             {:configMapKeyRef
              {:name "postgres-config", :key "postgres-db"}}}
            {:name "PGDATA", :value "/var/lib/postgresql/data/db/"}]
           :ports [{:containerPort 5432, :name "postgresql"}]
           :volumeMounts
           [{:name "postgres-config-volume"
             :mountPath "/etc/postgresql/postgresql.conf"
             :subPath "postgresql.conf"
             :readOnly true}
            {:name "postgre-data-volume"
             :mountPath "/var/lib/postgresql/data"}]}]
         (get-in (cut/generate-deployment {:postgres-image "postgres:14"
                                           :postgres-size :2gb
                                           :db-name "test"
                                           :postgres-data-volume-path "/var/postgres"
                                           :pv-storage-size-gb 10
                                           :pvc-storage-class-name "manual"
                                           :namespace "default"})
                 [:spec :template :spec :containers])))
  (is (= {:name "postgresql",
          :namespace "myapp"}
         (:metadata (cut/generate-deployment {:postgres-image "postgres:14"
                                              :postgres-size :2gb
                                              :db-name "test"
                                              :postgres-data-volume-path "/var/postgres"
                                              :pv-storage-size-gb 10
                                              :pvc-storage-class-name "manual"
                                              :namespace "myapp"})))))



(deftest should-generate-persistent-volume
  (is (= {:kind "PersistentVolume"
          :apiVersion "v1"
          :metadata
          {:name "postgres-pv-volume",
           :namespace "default"
           :labels {:type "local"}}
          :spec
          {:storageClassName "manual"
           :accessModes ["ReadWriteOnce"]
           :capacity {:storage "20Gi"}
           :hostPath {:path "xx"}}}
         (cut/generate-persistent-volume {:postgres-image "postgres:14"
                                          :postgres-size :2gb
                                          :db-name "test"
                                          :pvc-storage-class-name "manual"
                                          :postgres-data-volume-path "xx"
                                          :pv-storage-size-gb 20
                                          :namespace "default"}))))


(deftest should-generate-persistent-volume-claim
  (is (= {:apiVersion "v1"
          :kind "PersistentVolumeClaim"
          :metadata
          {:name "postgres-claim",
           :namespace "default"
           :labels {:app "postgres"}}
          :spec
          {:storageClassName "local-path"
           :accessModes ["ReadWriteOnce"]
           :resources {:requests {:storage "20Gi"}}}}
         (cut/generate-pvc {:postgres-image "postgres:13"
                            :postgres-size :2gb
                            :db-name "postgres"
                            :postgres-data-volume-path "/var/postgres"
                            :pv-storage-size-gb 20
                            :pvc-storage-class-name "local-path"
                            :namespace "default"}))))


(deftest should-generate-secret
  (is (= {:apiVersion "v1"
          :kind "Secret"
          :metadata {:name "postgres-secret" :namespace "default"}
          :type "Opaque"
          :data
          {:postgres-user "eHgtdXM=", :postgres-password "eHgtcHc="}}
         (cut/generate-secret {:postgres-image "postgres:13"
                               :postgres-size :2gb
                               :db-name "postgres"
                               :postgres-data-volume-path "/var/postgres"
                               :pv-storage-size-gb 20
                               :pvc-storage-class-name "local-path"
                               :namespace "default"}
                              {:postgres-db-user "xx-us" :postgres-db-password "xx-pw"})))
  (is (= {:apiVersion "v1"
          :kind "Secret"
          :metadata {:name "postgres-secret" :namespace "app"}
          :type "Opaque"
          :data
          {:postgres-user "eHgtdXM=", :postgres-password "eHgtcHc="}}
         (cut/generate-secret {:postgres-image "postgres:13"
                               :postgres-size :2gb
                               :db-name "postgres"
                               :postgres-data-volume-path "/var/postgres"
                               :pv-storage-size-gb 20
                               :pvc-storage-class-name "local-path"
                               :namespace "app"}
                              {:postgres-db-user "xx-us" :postgres-db-password "xx-pw"}))))


(deftest should-generate-service
  (is (= {:name "postgresql-service" :namespace "default"}
         (:metadata (cut/generate-service
                     {:postgres-image "postgres:13"
                      :postgres-size :2gb
                      :db-name "postgres"
                      :postgres-data-volume-path "/var/postgres"
                      :pv-storage-size-gb 20
                      :pvc-storage-class-name "local-path"
                      :namespace "default"})))))

