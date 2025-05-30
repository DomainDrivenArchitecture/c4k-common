(ns dda.c4k-common.monitoring
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.namespace :as ns]
   [dda.c4k-common.monitoring.monitoring-internal :as int]))

(s/def ::grafana-cloud-user ::int/remote-write-user)
(s/def ::remote-write-user ::int/remote-write-user)
(s/def ::grafana-cloud-password ::int/remote-write-password)
(s/def ::remote-write-password ::int/remote-write-password)
(s/def ::grafana-cloud-url ::int/remote-write-url)
(s/def ::remote-write-url ::int/remote-write-url)
(s/def ::cluster-name ::int/cluster-name)
(s/def ::cluster-stage ::int/cluster-stage)
(s/def ::storage-size-gb ::int/storage-size-gb)
(s/def ::cluster-name ::int/cluster-name)
(s/def ::mode ::int/mode)
(s/def ::mon-cfg (s/keys :req-un [::cluster-name
                                  ::cluster-stage]
                         :opt-un [::mode]))
(s/def ::mon-auth (s/keys :opt-un [::remote-write-user
                                   ::remote-write-password]))

(defn-spec dynamic-defaults ::int/mon-cfg
  [conf ::mon-cfg]
  (if (contains? conf :grafana-cloud-url)
    (merge conf {:mode {:remote-write-url (:grafana-cloud-url conf)}})
    conf))

(defn- dynamic-auth
  [conf
   auth]
  (if (and (contains? conf :grafana-cloud-url)
           (contains? auth :grafana-cloud-user))
    (merge auth {:remote-write-user (:grafana-cloud-user auth)
                 :remote-write-password (:grafana-cloud-password auth)})
    auth))

(defn-spec ^{:deprecated "10.0.0"} generate-config seq?
  "Use config-objects instead"
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
   (yaml/load-as-edn "monitoring/kube-state-metrics-service.yaml")   
   (yaml/load-as-edn "monitoring/push-gw-deployment.yaml")
   (yaml/load-as-edn "monitoring/push-gw-service-account.yaml")
   (yaml/load-as-edn "monitoring/push-gw-service.yaml")])

(defn-spec config-objects seq?
  [config ::mon-cfg]
  (let [resolved-config (dynamic-defaults config)]
    (cm/concat-vec
     (ns/generate {:namespace "monitoring"})
     (int/config-objects resolved-config))))

(defn-spec auth-objects seq?
  [config ::mon-cfg
   auth ::mon-auth]
  (let [resolved-config (dynamic-defaults config)
        resolved-auth (dynamic-auth config auth)]
  (int/auth-objects resolved-config resolved-auth)))

(defn-spec ^{:deprecated "10.0.0"} generate-auth seq?
  "Use auth-objects instead"
  [config ::mon-cfg
   auth ::mon-auth]
  (auth-objects config auth))
