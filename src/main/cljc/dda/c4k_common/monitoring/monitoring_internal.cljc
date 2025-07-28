(ns dda.c4k-common.monitoring.monitoring-internal
  (:require
   [clojure.spec.alpha :as s]
   [orchestra.core :refer [defn-spec]]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.common :as cm]
   [clojure.string :as str]))

(s/def ::remote-write-user cp/bash-env-string?)
(s/def ::remote-write-password cp/bash-env-string?)
(s/def ::remote-write-url string?)
(s/def ::storage-class string?)
(s/def ::storage-size-gb pos?)
(s/def ::cluster-name string?)
(s/def ::cluster-stage cp/stage?)
(s/def ::mode (s/or ::remote-write (s/keys :req-un [::remote-write-url])
                    ::persistent (s/keys :req-un [::storage-size-gb ::storage-class])))
(s/def ::mon-cfg (s/keys :req-un [::mode
                                  ::cluster-name
                                  ::cluster-stage]))
(s/def ::mon-auth (s/keys :opt-un [::remote-write-user
                                   ::remote-write-password]))

(def metric-regex {:node-regex
                   (str "node_cpu_sec.+|node_load[0-9]+|node_memory_Buf.*|node_memory_Mem.*|"
                        "node_memory_Cached.*|node_disk_[r,w,i].*|node_filesystem_[s,a].*|"
                        "node_network_receive_bytes_total|node_network_transmit_bytes_total")
                   :traefik-regex (str "traefik_entrypoint_.*_total|"
                                       "traefik_entrypoint_.*_seconds_count|"
                                       "traefik_router_.*_total|"
                                       "traefik_router_.*_seconds_count|"
                                       "traefik_service_.*_total|"
                                       "traefik_service_.*_seconds_count|"
                                       "traefik_tls_certs_not_after")
                   :kube-state-regex (str "kube_pod_container_status_restarts_total|"
                                          "kube_pod_status_reason|kube_node_status_capacity|"
                                          "kube_node_status_allocatable|"
                                          "kube_cronjob_status_active|"
                                          "kube_job_status_.*")})

(def filter-regex-string
  (str/join "|" (vals metric-regex)))

(defn-spec deployment map?
  [config ::mon-cfg]
  (let [{:keys [mode]} config
        args (if (contains? mode :remote-write-url)
               ["--config.file=/etc/prometheus/prometheus.yaml"
                "--storage.tsdb.path=/prometheus/"
                "--storage.tsdb.retention.time=1d"]
               ["--config.file=/etc/prometheus/prometheus.yaml"
                "--storage.tsdb.path=/prometheus/"
                "--storage.tsdb.retention.time=120d"
                "--web.enable-admin-api"
                "--web.enable-remote-write-receiver"])
        volume (if (contains? mode :storage-size-gb)
                 {:name "prometheus-storage-volume"
                   :persistentVolumeClaim {:claimName "prometheus-storage"}}
                 {:name "prometheus-storage-volume"
                   :emptyDir {}})]
    (->
     (yaml/load-as-edn "monitoring/prometheus-deployment.yaml")
     (assoc-in [:spec :template :spec :containers 0 :args] args)
     (assoc-in [:spec :template :spec :volumes 1] volume))))

(defn-spec pvc seq?
  [config ::mon-cfg]
  (let [{:keys [mode]} config
        {:keys [storage-size-gb storage-class]} mode]
    (if (contains? mode :storage-size-gb)
      [(->
        (yaml/load-as-edn "monitoring/prometheus-pvc.yaml")
        (assoc-in [:spec :resources :requests :storage] (str storage-size-gb "Gi"))
        (assoc-in [:spec :storageClassName] storage-class))]
      [])))

(defn-spec remote-write map?
  [config ::mon-cfg
   auth ::mon-auth]
  (let [{:keys [mode]} config
        {:keys [remote-write-url]} mode
        {:keys [remote-write-user remote-write-password]} auth]
    (if (contains? mode :remote-write-url)
      {:remote_write 
       [{:url remote-write-url
         :basic_auth {:username remote-write-user, 
                      :password remote-write-password},
         :write_relabel_configs
         [{:source_labels ["__name__"],
           :regex filter-regex-string,
           :action "keep"}]}]}
      {})))

(defn-spec prometheus-config map?
  [config ::mon-cfg
   auth ::mon-auth]
  (let [{:keys [cluster-name cluster-stage mode]} config]
    (->
     (yaml/load-as-edn "monitoring/prometheus-prometheus.yaml")
     (cm/replace-all-matching "CLUSTERNAME" cluster-name)
     (cm/replace-all-matching "STAGE" cluster-stage)
     (merge (remote-write config auth)))))

(defn-spec config-secret map?
  [config ::mon-cfg
   auth ::mon-auth]
  (->
   (yaml/load-as-edn "monitoring/prometheus-config-secret.yaml")
   (assoc-in [:stringData :prometheus.yaml]
             (yaml/to-string
              (prometheus-config config auth)))))

(defn-spec config-objects seq?
  [config ::mon-cfg]
  (cm/concat-vec
   [(yaml/load-as-edn "monitoring/prometheus-cluster-role.yaml")
    (yaml/load-as-edn "monitoring/prometheus-cluster-role-binding.yaml")
    (yaml/load-as-edn "monitoring/node-exporter-cluster-role.yaml")
    (yaml/load-as-edn "monitoring/node-exporter-cluster-role-binding.yaml")
    (yaml/load-as-edn "monitoring/kube-state-metrics-cluster-role.yaml")
    (yaml/load-as-edn "monitoring/kube-state-metrics-cluster-role-binding.yaml")

    (yaml/load-as-edn "monitoring/prometheus-service-account.yaml")
    (yaml/load-as-edn "monitoring/kube-state-metrics-service-account.yaml")
    (yaml/load-as-edn "monitoring/node-exporter-service-account.yaml")
    (yaml/load-as-edn "monitoring/push-gw-service-account.yaml")

    (yaml/load-as-edn "monitoring/node-exporter-service.yaml")
    (yaml/load-as-edn "monitoring/prometheus-service.yaml")
    (yaml/load-as-edn "monitoring/kube-state-metrics-service.yaml")
    (yaml/load-as-edn "monitoring/push-gw-service.yaml")

    (deployment config)]
   (pvc config)
   [(yaml/load-as-edn "monitoring/node-exporter-daemon-set.yaml")
    (yaml/load-as-edn "monitoring/kube-state-metrics-deployment.yaml")
    (yaml/load-as-edn "monitoring/push-gw-deployment.yaml")]))

  (defn-spec auth-objects seq?
    [config ::mon-cfg
     auth ::mon-auth]
    [(config-secret config auth)])
