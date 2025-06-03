(ns dda.c4k-common.ingress
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.predicate :as pred]
   [dda.c4k-common.namespace :as ns]
   [dda.c4k-common.ingress.ingress-internal :as int]))

(s/def ::issuer ::int/issuer)
(s/def ::service-name ::int/service-name)
(s/def ::app-name ::int/app-name)
(s/def ::ingress-name ::int/ingress-name)
(s/def ::cert-name ::int/cert-name)
(s/def ::service-port ::int/service-port)
(s/def ::fqdn pred/fqdn-string?)
(s/def ::fqdns ::int/fqdns)
(s/def ::average-rate ::int/average-rate)
(s/def ::burst-rate ::int/burst-rate)

(def simple-ingress?
  (s/keys :req-un [::fqdns ::service-name ::service-port]
          :opt-un [::issuer ::average-rate ::burst-rate
                   ::ns/namespace ::int/basic-auth-secret]))

(s/def ::ingress simple-ingress?)

(def ingress? (s/keys :req-un [::fqdns ::app-name ::ingress-name ::service-name ::service-port]
                      :opt-un [::issuer ::cert-name ::rate-limit-name ::ns/namespace]))

(def certificate? (s/keys :req-un [::fqdns ::app-name ::cert-name]
                          :opt-un [::issuer ::ns/namespace]))

(def default-config
  (merge ns/default-config
         {:issuer "staging"
          :average-rate 10
          :burst-rate 20}))

(def config-defaults default-config)

(defn-spec dynamic-defaults ::int/ingress
  [config ::ingress]
  (let [{:keys [service-name]} config]
    (merge {:app-name service-name
            :ingress-name service-name
            :cert-name service-name
            :rate-limit-name service-name}
           config-defaults
           config)))


(defn-spec ^{:deprecated "10.0.0"} generate-certificate map?
  "Deprecated: use config-objects instead"
  [config certificate?]
  (let [final-config (merge default-config
                            config)]
    (int/certificate final-config)))


(defn-spec ^{:deprecated "10.0.0"} generate-ingress map?
  "Deprecated: use config-objects instead"
  [config ingress?]
  (let [final-config (merge default-config
                            config)]
    (int/ingress final-config)))


(defn-spec ^{:deprecated "10.0.0"} generate-ingress-and-cert seq?
  "Deprecated: use config-objects instead"
  [config simple-ingress?]
  (let [final-config (dynamic-defaults config)]
    [(int/certificate final-config)
     (int/ingress final-config)]))

(defn-spec config-objects seq?
  [config ::ingress]
  (let [resolved-config (dynamic-defaults config)]
    (int/config-objects resolved-config)))

(defn-spec ^{:deprecated "10.0.0"} generate-simple-ingress seq?
  "Deprecated: use config-objects instead"
  [config simple-ingress?]
  (config-objects config))