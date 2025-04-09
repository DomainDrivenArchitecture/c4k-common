(ns dda.c4k-common.postgres-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.postgres :as cut]))

(st/instrument `cut/generate-configmap)
(st/instrument `cut/generate-persistent-volume)
(st/instrument `cut/generate-secret)
(st/instrument `cut/generate)
(st/instrument `cut/generate-config)
(st/instrument `cut/generate-auth)

(deftest should-generate-configmap
  (is (= {:postgres-db "postgres"
          :postgresql.conf
          "max_connections = 100\nwork_mem = 4MB\nshared_buffers = 512MB\n"}
         (:data (cut/generate-configmap)))))

(deftest should-generate-persistent-volume
  (is (= {:kind "PersistentVolume"
          :apiVersion "v1"
          :metadata
          {:name "postgres-pv-volume", :namespace "default" :labels {:type "local"}}
          :spec
          {:storageClassName "manual"
           :accessModes ["ReadWriteOnce"]
           :capacity {:storage "20Gi"}
           :hostPath {:path "xx"}}}
         (cut/generate-persistent-volume {:postgres-data-volume-path "xx"}))))


(deftest should-generate-persistent-volume-claim
  (is (= {:apiVersion "v1"
          :kind "PersistentVolumeClaim"
          :metadata
          {:name "postgres-claim", :namespace "default" :labels {:app "postgres"}}
          :spec
          {:storageClassName "local-path"
           :accessModes ["ReadWriteOnce"]
           :resources {:requests {:storage "20Gi"}}}}
         (cut/generate-pvc {}))))


(deftest should-generate-secret
  (is (= {:apiVersion "v1",
          :kind "Secret",
          :metadata {:name "postgres-secret", :namespace "default"},
          :type "Opaque",
          :data {:postgres-user "eHgtdXM=", :postgres-password "eHgtcHc="}}
         (cut/generate-secret {:postgres-db-user "xx-us" :postgres-db-password "xx-pw"})))
  (is (= {:apiVersion "v1",
          :kind "Secret",
          :metadata {:name "postgres-secret", :namespace "app"},
          :type "Opaque",
          :data {:postgres-user "eHgtdXM=", :postgres-password "eHgtcHc="}}
         (cut/generate-secret {:namespace "app"}
                              {:postgres-db-user "xx-us"
                               :postgres-db-password "xx-pw"}))))

(deftest should-generate-auth

  (is (= [{:apiVersion "v1",
           :kind "Secret",
           :metadata {:name "postgres-secret", :namespace "app"},
           :type "Opaque",
           :data {:postgres-user "eHgtdXM=", :postgres-password "eHgtcHc="}}]
         (cut/auth-objects {:namespace "app"}
                           {:postgres-db-user "xx-us"
                            :postgres-db-password "xx-pw"}))))



(deftest should-generate
  (is (= 5
         (count (cut/config-objects {}))))
  (is (= 1
         (count (cut/auth-objects {}
                                  {:postgres-db-user "user"
                                   :postgres-db-password "password"})))))