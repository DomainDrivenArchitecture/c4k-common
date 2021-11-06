(ns dda.c4k-common.postgres-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
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
         (cut/generate-persistent-volume {:postgres-data-volume-path "xx"}))))

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
