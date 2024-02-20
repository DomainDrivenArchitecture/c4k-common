(ns dda.c4k-common.monitoring.monitoring-internal
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:cljs [dda.c4k-common.macros :refer-macros [inline-resources]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.common :as cm]
   [clojure.string :as str]))

(s/def ::grafana-cloud-user cp/bash-env-string?)
(s/def ::grafana-cloud-password cp/bash-env-string?)
(s/def ::grafana-cloud-url string?)
(s/def ::cluster-name string?)
(s/def ::cluster-stage cp/stage?)
(s/def ::mon-cfg (s/keys :req-un [::grafana-cloud-url
                                 ::cluster-name
                                 ::cluster-stage]))
(s/def ::mon-auth (s/keys :req-un [::grafana-cloud-user 
                               ::grafana-cloud-password]))

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

(defn-spec generate-prometheus-config map?
  [config ::mon-cfg
   auth ::mon-auth]
  (let [{:keys [grafana-cloud-url cluster-name cluster-stage]} config
        {:keys [grafana-cloud-user grafana-cloud-password]} auth]
    (->
     (yaml/load-as-edn "monitoring/prometheus-prometheus.yaml")
     (assoc-in [:global :external_labels :cluster]
               cluster-name)
     (assoc-in [:global :external_labels :stage]
               cluster-stage)
     (assoc-in [:remote_write 0 :url]
               grafana-cloud-url)
     (assoc-in [:remote_write 0 :basic_auth :username]
               grafana-cloud-user)
     (assoc-in [:remote_write 0 :basic_auth :password]
               grafana-cloud-password)
     (cm/replace-all-matching-values-by-new-value "FILTER_REGEX" filter-regex-string))))

(defn-spec generate-config map?
  [config ::mon-cfg
   auth ::mon-auth]
  (->
   (yaml/load-as-edn "monitoring/prometheus-config.yaml")
   (assoc-in [:stringData :prometheus.yaml]
             (yaml/to-string
              (generate-prometheus-config config auth)))))

#?(:cljs
   (defmethod yaml/load-resource :monitoring [resource-name]
     (get (inline-resources "monitoring") resource-name)))
