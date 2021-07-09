(ns dda.c4k-common.postgres-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [dda.c4k-common.postgres :as cut]))

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
