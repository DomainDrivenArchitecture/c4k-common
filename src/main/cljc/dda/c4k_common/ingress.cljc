(ns dda.c4k-common.ingress
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.namespace :as ns]
   [dda.c4k-common.ingress.ingress-internal :as int]))

(s/def ::issuer ::int/issuer)
(s/def ::service-name ::int/service-name)
(s/def ::app-name ::int/app-name)
(s/def ::ingress-name ::int/ingress-name)
(s/def ::cert-name ::int/cert-name)
(s/def ::service-port ::int/service-port)
(s/def ::fqdns ::int/fqdns)
(s/def ::average-rate ::int/average-rate)
(s/def ::burst-rate ::int/burst-rate)

(def simple-ingress? (s/keys :req-un [::fqdns ::service-name ::service-port]
                             :opt-un [::issuer ::average-rate ::ns/namespace]))

(def ingress? (s/keys :req-un [::fqdns ::app-name ::ingress-name ::service-name ::service-port]
                      :opt-un [::issuer ::cert-name ::rate-limit-name ::ns/namespace]))

(def certificate? (s/keys :req-un [::fqdns ::app-name ::cert-name]
                          :opt-un [::issuer ::ns/namespace]))

(def rate-limit-config? (s/keys :req-un [::rate-limit-name
                                         ::average-rate
                                         ::burst-rate]))

(def default-config
  (merge ns/default-config
         {:issuer "staging"
          :average-rate 10}))


(defn-spec generate-certificate map?
  [config certificate?]
  (let [final-config (merge default-config
                            config)]
    (int/generate-certificate final-config)))


(defn-spec generate-ingress map?
  [config ingress?]
  (let [final-config (merge default-config
                            config)]
    (int/generate-ingress final-config)))


(defn-spec generate-ingress-and-cert seq?
  [config simple-ingress?]
  (let [{:keys [service-name]} config
        final-config (merge {:app-name service-name
                             :ingress-name service-name
                             :cert-name service-name}
                            default-config
                            config)]
    [(int/generate-certificate final-config)
     (int/generate-ingress final-config)]))


(defn-spec generate-simple-ingress seq?
  [config simple-ingress?]
  (let [{:keys [service-name]} config
        final-config (merge {:app-name service-name
                             :ingress-name service-name
                             :cert-name service-name
                             :rate-limit-name service-name}
                            default-config
                            config)
        {:keys [average-rate]} final-config]
    [(int/generate-certificate final-config)
     (int/generate-rate-limit-middleware {:rate-limit-name service-name
                                          :average-rate average-rate
                                          :burst-rate average-rate})
     (int/generate-ingress final-config)]))