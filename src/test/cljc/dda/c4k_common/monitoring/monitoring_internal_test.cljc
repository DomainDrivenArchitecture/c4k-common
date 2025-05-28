(ns dda.c4k-common.monitoring.monitoring-internal-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.string :as str]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.monitoring.monitoring-internal :as cut]))

(st/instrument `cut/deployment)
(st/instrument `cut/pvc)
(st/instrument `cut/remote-write)
(st/instrument `cut/prometheus-config)
(st/instrument `cut/config-secret)

(def conf {:cluster-name "clustername"
           :cluster-stage "test"
           :mode {:remote-write-url "https://some.url/with/path"}})

(def auth {:remote-write-user "user"
           :remote-write-password "password"
           :hetzner-cloud-ro-token "ro-token"})

(def invalid-conf {:cluster-name "clustername"
                   :cluster-stage "test"
                   :mode {:remote-writ-url "https://some.url/with/path"}})

(deftest should-not-generate-config
  (is (thrown?
       #?(:clj Exception :cljs js/Error)
       (cut/config-secret invalid-conf auth))))

(deftest should-generate-pvc
  (is (= []
         (cut/pvc conf)))
  (is (= {:accessModes ["ReadWriteOnce"],
          :resources {:requests {:storage "20Gi"}},
          :storageClassName "local-path"}
         (get-in (cut/pvc (merge conf
                                 {:mode {:storage-size-gb 20
                                         :storage-class "local-path"}}))
                 [0 :spec]))))

(deftest should-generate-deployment
  (is (= [{:name "prometheus-config-volume",
           :secret {:defaultMode 420, :secretName "prometheus-conf"}}
          {:emptyDir {}, :name "prometheus-storage-volume"}]
         (get-in (cut/deployment conf)
                 [:spec :template :spec :volumes])))
  (is (= [{:name "prometheus-config-volume",
           :secret {:defaultMode 420, :secretName "prometheus-conf"}}
          {:name "prometheus-storage-volume",
           :persistentVolumeClaim {:claimName "prometheus-storage"}}]
         (get-in (cut/deployment (merge conf
                                        {:mode {:storage-size-gb 20
                                                :storage-class "local-path"}}))
                 [:spec :template :spec :volumes]))))

(deftest should-generate-remote-write
  (is (= {:remote_write
          [{:basic_auth {:password "password", :username "user"},
            :url "https://some.url/with/path",
            :write_relabel_configs
            [{:action "keep",
              :regex
              "node_cpu_sec.+|node_load[0-9]+|node_memory_Buf.*|node_memory_Mem.*|node_memory_Cached.*|node_disk_[r,w,i].*|node_filesystem_[s,a].*|node_network_receive_bytes_total|node_network_transmit_bytes_total|traefik_entrypoint_.*_total|traefik_entrypoint_.*_seconds_count|traefik_router_.*_total|traefik_router_.*_seconds_count|traefik_service_.*_total|traefik_service_.*_seconds_count|traefik_tls_certs_not_after|kube_pod_container_status_restarts_total|kube_pod_status_reason|kube_node_status_capacity|kube_node_status_allocatable|kube_cronjob_status_active|kube_job_.*",
              :source_labels ["__name__"]}]}]}
         (cut/remote-write conf auth)))
  (is (= {}
         (cut/remote-write (merge conf {:mode {:storage-size-gb 20 :storage-class "local-path"}}) auth))))

(deftest should-generate-prometheus
  (is (contains? 
         (cut/prometheus-config conf auth)
          :remote_write))
  (is (= {:cluster "clustername",
          :stage "test"}
         (get-in
          (cut/prometheus-config conf auth)
          [:global :external_labels]))))

(deftest should-generate-config
  (is (str/starts-with?
       (get-in
        (cut/config-secret conf auth)
        [:stringData :prometheus.yaml])
       "global:\n  scrape_interval:")))

(deftest should-generate-objects
  (is (= 18
         (count (cut/config-objects conf))))
  (is (= 1
         (count (cut/auth-objects conf auth)))))