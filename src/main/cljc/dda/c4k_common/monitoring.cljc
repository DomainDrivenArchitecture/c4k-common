(ns dda.c4k-common.monitoring
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.monitoring.monitoring-internal :as int]))

(s/def ::grafana-cloud-user ::int/grafana-cloud-user)
(s/def ::grafana-cloud-password ::int/grafana-cloud-password)
(s/def ::grafana-cloud-url ::int/grafana-cloud-url)
(s/def ::cluster-name ::int/cluster-name)
(s/def ::cluster-stage ::int/cluster-stage)
(s/def ::mon-cfg (s/keys :req-un [::grafana-cloud-url
                                  ::cluster-name
                                  ::cluster-stage]))
(s/def ::mon-auth (s/keys :req-un [::grafana-cloud-user
                                   ::grafana-cloud-password]))


(def filter-regex-string int/filter-regex-string)


(defn-spec ^{:deprecated "6.4.1"} generate seq?
  "use generate-config and generate-auth instead"
  [config ::mon-cfg
   auth ::mon-auth]
  [(yaml/load-as-edn "monitoring/namespace.yaml")
   (yaml/load-as-edn "monitoring/prometheus-cluster-role.yaml")
   (yaml/load-as-edn "monitoring/prometheus-cluster-role-binding.yaml")
   (yaml/load-as-edn "monitoring/prometheus-service.yaml")
   (yaml/load-as-edn "monitoring/prometheus-service-account.yaml")
   (int/generate-config-secret config auth)
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

(defn-spec generate-config seq?
  []
  [(yaml/load-as-edn "monitoring/namespace.yaml")
   (yaml/load-as-edn "monitoring/prometheus-cluster-role.yaml")
   (yaml/load-as-edn "monitoring/prometheus-cluster-role-binding.yaml")
   (yaml/load-as-edn "monitoring/prometheus-service.yaml")
   (yaml/load-as-edn "monitoring/prometheus-service-account.yaml")
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

(defn-spec generate-auth seq?
  [config ::mon-cfg
   auth ::mon-auth]
  [(int/generate-config-secret config auth)])
