(ns dda.c4k-common.monitoring
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [clojure.string :as str]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.monitoring.monitoring-internal :as int]))

(s/def ::grafana-cloud-user cp/bash-env-string?)
(s/def ::grafana-cloud-password cp/bash-env-string?)
(s/def ::grafana-cloud-url string?)
(s/def ::cluster-name string?)
(s/def ::cluster-stage cp/stage?)
(s/def ::node-regex string?)
(s/def ::traefik-regex string?)
(s/def ::kube-state-regex string?)
(s/def ::mon-cfg (s/keys :req-un [::grafana-cloud-url
                                 ::cluster-name
                                 ::cluster-stage]))
(s/def ::mon-auth (s/keys :req-un [::grafana-cloud-user 
                               ::grafana-cloud-password]))
(s/def ::filter-regex (s/keys :req-un [::node-regex 
                                       ::traefik-regex 
                                       ::kube-state-regex]))


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
                                          "kube_pod_status_reason|kube_node_status_capacity|kube_node_status_allocatable|"
                                          "kube_cronjob_status_active|kube_job_status_failed")})

(def filter-regex-string
  (str/join "|" (vals metric-regex)))


(defn-spec generate seq?
  [config ::mon-cfg
   auth ::mon-auth]
  [(yaml/load-as-edn "monitoring/namespace.yaml")
   (yaml/load-as-edn "monitoring/prometheus-cluster-role.yaml")
   (yaml/load-as-edn "monitoring/prometheus-cluster-role-binding.yaml")
   (yaml/load-as-edn "monitoring/prometheus-service.yaml")
   (yaml/load-as-edn "monitoring/prometheus-service-account.yaml")
   (int/generate-config config auth)
   (yaml/load-as-edn "monitoring/prometheus-deployment.yaml")
   (yaml/load-as-edn "monitoring/node-exporter-service-account.yaml")
   (yaml/load-as-edn "monitoring/node-exporter-cluster-role.yaml")
   (yaml/load-as-edn "monitoring/node-exporter-cluster-role-binding.yaml")
   (yaml/load-as-edn "monitoring/node-exporter-daemon-set.yaml")
   (yaml/load-as-edn "monitoring/node-exporter-service.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics-cluster-role-binding.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics-cluster-role.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics-deployment.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics-service-account.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics-service.yaml")])
