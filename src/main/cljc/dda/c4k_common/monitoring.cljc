(ns dda.c4k-common.monitoring
  (:require
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.predicate :as cp]
   [dda.c4k-common.common :as cm]
   [clojure.string :as str]))

(s/def ::grafana-cloud-user cp/bash-env-string?)
(s/def ::grafana-cloud-password cp/bash-env-string?)
(s/def ::grafana-cloud-url string?)
(s/def ::cluster-name string?)
(s/def ::cluster-stage cp/stage?)
(s/def ::pvc-storage-class-name cp/pvc-storage-class-name?)
(s/def ::node-regex string?)
(s/def ::traefik-regex string?)
(s/def ::kube-state-regex string?)

(defn config? [input]
  (s/keys :req-un [::grafana-cloud-url 
                   ::cluster-name 
                   ::cluster-stage]))

(defn auth? [input]
  (s/keys :req-un [::grafana-cloud-user ::grafana-cloud-password]))

(defn storage? [input]
  (s/keys :opt-un [::pvc-storage-class-name]))

(defn filter-regex? [input]
  (s/keys :req-un [::node-regex ::traefik-regex ::kube-state-regex]))

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

#?(:cljs
   (defmethod yaml/load-resource :monitoring [resource-name]
     (case resource-name
       "monitoring/namespace.yaml" (rc/inline "monitoring/namespace.yaml")

       "monitoring/kube-state-metrics/cluster-role-binding.yaml" (rc/inline "monitoring/kube-state-metrics/cluster-role-binding.yaml")
       "monitoring/kube-state-metrics/cluster-role.yaml" (rc/inline "monitoring/kube-state-metrics/cluster-role.yaml")
       "monitoring/kube-state-metrics/deployment.yaml" (rc/inline "monitoring/kube-state-metrics/deployment.yaml")
       "monitoring/kube-state-metrics/service-account.yaml" (rc/inline "monitoring/kube-state-metrics/service-account.yaml")
       "monitoring/kube-state-metrics/service.yaml" (rc/inline "monitoring/kube-state-metrics/service.yaml")
       "monitoring/node-exporter/cluster-role-binding.yaml" (rc/inline "monitoring/node-exporter/cluster-role-binding.yaml")
       "monitoring/node-exporter/cluster-role.yaml" (rc/inline "monitoring/node-exporter/cluster-role.yaml")
       "monitoring/node-exporter/daemon-set.yaml" (rc/inline "monitoring/node-exporter/daemon-set.yaml")
       "monitoring/node-exporter/service-account.yaml" (rc/inline "monitoring/node-exporter/service-account.yaml")
       "monitoring/node-exporter/service.yaml" (rc/inline "monitoring/node-exporter/service.yaml")
       "monitoring/prometheus/cluster-role-binding.yaml" (rc/inline "monitoring/prometheus/cluster-role-binding.yaml")
       "monitoring/prometheus/cluster-role.yaml" (rc/inline "monitoring/prometheus/cluster-role.yaml")
       "monitoring/prometheus/config.yaml" (rc/inline "monitoring/prometheus/config.yaml")
       "monitoring/prometheus/deployment.yaml" (rc/inline "monitoring/prometheus/deployment.yaml")
       "monitoring/prometheus/prometheus.yaml" (rc/inline "monitoring/prometheus/prometheus.yaml")
       "monitoring/prometheus/service-account.yaml" (rc/inline "monitoring/prometheus/service-account.yaml")
       "monitoring/prometheus/service.yaml" (rc/inline "monitoring/prometheus/service.yaml")
       (throw (js/Error. "Undefined Resource!")))))

(defn-spec generate-stateful-set cp/map-or-seq?
  [config storage?]
  (let [{:keys [pvc-storage-class-name]
         :or {pvc-storage-class-name :manual}} config]
    (->
     (yaml/load-as-edn "monitoring/stateful-set.yaml")
     (assoc-in [:spec :volumeClaimTemplates 0 :spec :storageClassName] (name pvc-storage-class-name)))))

(defn-spec generate-prometheus-config cp/map-or-seq?
  [config config?
   auth auth?]
  (let [{:keys [grafana-cloud-url cluster-name cluster-stage]} config
        {:keys [grafana-cloud-user grafana-cloud-password]} auth]
    (->
     (yaml/load-as-edn "monitoring/prometheus/prometheus.yaml")
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

(defn-spec generate-config cp/map-or-seq?
  [config config?
   auth auth?]
  (->
   (yaml/load-as-edn "monitoring/prometheus/config.yaml")
   (assoc-in [:stringData :prometheus.yaml]
             (yaml/to-string
              (generate-prometheus-config config auth)))))

(defn-spec generate cp/map-or-seq?
  [config config?
   auth auth?]
  [(yaml/load-as-edn "monitoring/namespace.yaml")
   (yaml/load-as-edn "monitoring/prometheus/cluster-role.yaml")
   (yaml/load-as-edn "monitoring/prometheus/cluster-role-binding.yaml")
   (yaml/load-as-edn "monitoring/prometheus/service.yaml")
   (yaml/load-as-edn "monitoring/prometheus/service-account.yaml")
   (generate-config config auth)
   (yaml/load-as-edn "monitoring/prometheus/deployment.yaml")
   (yaml/load-as-edn "monitoring/node-exporter/service-account.yaml")
   (yaml/load-as-edn "monitoring/node-exporter/cluster-role.yaml")
   (yaml/load-as-edn "monitoring/node-exporter/cluster-role-binding.yaml")
   (yaml/load-as-edn "monitoring/node-exporter/daemon-set.yaml")
   (yaml/load-as-edn "monitoring/node-exporter/service.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics/cluster-role-binding.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics/cluster-role.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics/deployment.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics/service-account.yaml")
   (yaml/load-as-edn "monitoring/kube-state-metrics/service.yaml")])
