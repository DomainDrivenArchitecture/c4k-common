(ns dda.c4k-common.postgres-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.test-helper :as ct]
   [dda.c4k-common.postgres :as cut]))

(st/instrument `cut/generate-config)
(st/instrument `cut/generate-deployment)
(st/instrument `cut/generate-persistent-volume)
(st/instrument `cut/generate-pvc)
(st/instrument `cut/generate-secret)
(st/instrument `cut/generate-service)

(deftest should-generate-config
  (is (= {:postgres-db "postgres"
          :postgresql.conf
          "max_connections = 100\nwork_mem = 4MB\nshared_buffers = 512MB\n"}
         (:data (cut/generate-config))))
  (is (= {:postgres-db "postgres"
          :postgresql.conf
          "max_connections = 700\nwork_mem = 3MB\nshared_buffers = 2048MB\n"}
         (:data (cut/generate-config {:postgres-size :8gb}))))
  (is (= {:postgres-db "test"
          :postgresql.conf
          "max_connections = 100\nwork_mem = 4MB\nshared_buffers = 512MB\n"}
         (:data (cut/generate-config {:db-name "test"}))))
  )

(deftest should-generate-config-diff
  (is (= {:postgres-db-c1 "postgres",
          :postgres-db-c2 "test",
          :postgresql.conf-c1 "max_connections = 100\nwork_mem = 4MB\nshared_buffers = 512MB\n",
          :postgresql.conf-c2 "max_connections = 700\nwork_mem = 3MB\nshared_buffers = 2048MB\n"}
         (ct/map-diff (cut/generate-config) (cut/generate-config {:db-name "test" :postgres-size :8gb})))))

(deftest should-generate-persistent-volume
  (is (= {:kind "PersistentVolume"
          :apiVersion "v1"
          :metadata
          {:name "postgres-pv-volume", :labels {:type "local"}}
          :spec
          {:storageClassName "manual"
           :accessModes ["ReadWriteOnce"]
           :capacity {:storage "10Gi"}
           :hostPath {:path "xx"}}}
         (cut/generate-persistent-volume {:postgres-data-volume-path "xx"})))
  (is (= {:kind "PersistentVolume"
          :apiVersion "v1"
          :metadata
          {:name "postgres-pv-volume", :labels {:type "local"}}
          :spec
          {:storageClassName "manual"
           :accessModes ["ReadWriteOnce"]
           :capacity {:storage "20Gi"}
           :hostPath {:path "xx"}}}
         (cut/generate-persistent-volume {:postgres-data-volume-path "xx"
                                          :pv-storage-size-gb 20}))))

(deftest should-generate-persistent-volume-diff
  (is (= {:storage-c1 "10Gi", :storage-c2 "20Gi",
          :path-c1 "/var/postgres", :path-c2 "xx"}
         (ct/map-diff (cut/generate-persistent-volume {}) 
                        (cut/generate-persistent-volume {:postgres-data-volume-path "xx" 
                                                         :pv-storage-size-gb 20})))))

(deftest should-generate-persistent-volume-claim
  (is (= {:apiVersion "v1"
          :kind "PersistentVolumeClaim"
          :metadata
          {:name "postgres-claim", :labels {:app "postgres"}}
          :spec
          {:storageClassName "manual"
           :accessModes ["ReadWriteOnce"]
           :resources {:requests {:storage "10Gi"}}}}
         (cut/generate-pvc {})))
  (is (= {:apiVersion "v1"
          :kind "PersistentVolumeClaim"
          :metadata
          {:name "postgres-claim", :labels {:app "postgres"}}
          :spec
          {:storageClassName "local-path"
           :accessModes ["ReadWriteOnce"]
           :resources {:requests {:storage "20Gi"}}}}
         (cut/generate-pvc {:pv-storage-size-gb 20
                            :pvc-storage-class-name :local-path}))))

(deftest should-generate-persistent-volume-claim-diff
  (is (= {:storageClassName-c1 "manual", :storageClassName-c2 "local-path",
          :storage-c1 "10Gi", :storage-c2 "20Gi"}
         (ct/map-diff (cut/generate-pvc {}) 
                        (cut/generate-pvc {:pv-storage-size-gb 20
                                           :pvc-storage-class-name :local-path})))))

(deftest should-generate-secret
  (is (= {:apiVersion "v1"
          :kind "Secret"
          :metadata {:name "postgres-secret"}
          :type "Opaque"
          :data
          {:postgres-user "eHgtdXM=", :postgres-password "eHgtcHc="}}
         (cut/generate-secret {:postgres-db-user "xx-us" :postgres-db-password "xx-pw"}))))

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
              {:name "postgres-config", :key "postgres-db"}}}]
           :ports [{:containerPort 5432, :name "postgresql"}]
           :volumeMounts
           [{:name "postgres-config-volume"
             :mountPath "/etc/postgresql/postgresql.conf"
             :subPath "postgresql.conf"
             :readOnly true}
            {:name "postgre-data-volume"
             :mountPath "/var/lib/postgresql/data"}]}]
         (get-in (cut/generate-deployment {:postgres-image "postgres:14"})
                 [:spec :template :spec :containers]))))

(deftest should-generate-deployment-diff
  (is (= {:image-c1 "postgres:13", :image-c2 "postgres:14"}
         (ct/map-diff (cut/generate-deployment) (cut/generate-deployment {:postgres-image "postgres:14"})))))