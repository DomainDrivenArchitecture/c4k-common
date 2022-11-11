(ns dda.c4k-common.ingress-cert
  (:require
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:clj [clojure.edn :as edn]
      :cljs [cljs.reader :as edn])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.predicate :as pred]))

(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::service-name string?)
(s/def ::app-name string?)
(s/def ::ingress-name string?)
(s/def ::cert-name string?)
(s/def ::service-port pos-int?)
(s/def ::fqdns (s/coll-of pred/fqdn-string?))

(def ingress? (s/keys :req-un [::fqdns ::app-name ::ingress-name ::service-name ::service-port]
                      :opt-un [::issuer ::cert-name]))

(def certificate? (s/keys :req-un [::fqdns ::app-name ::cert-name]
                          :opt-un [::issuer]))

#?(:cljs
   (defmethod yaml/load-resource :ingress [resource-name]
     (case resource-name
       "ingress/host-rule.yaml" (rc/inline "ingress/host-rule.yaml")
       "ingress/certificate.yaml" (rc/inline "ingress/certificate.yaml")
       "ingress/ingress.yaml" (rc/inline "ingress/ingress.yaml")
       (throw (js/Error. "Undefined Resource!")))))

(defn-spec generate-host-rule  pred/map-or-seq?
  [service-name ::service-name
   service-port ::service-port
   fqdn pred/fqdn-string?]
  (->
   (yaml/load-as-edn "ingress/host-rule.yaml")
   (cm/replace-all-matching-values-by-new-value "FQDN" fqdn)
   (cm/replace-all-matching-values-by-new-value "SERVICE_PORT" service-port)
   (cm/replace-all-matching-values-by-new-value "SERVICE_NAME" service-name)))

(defn-spec generate-ingress pred/map-or-seq?
  [config ingress?]
  (let [{:keys [ingress-name cert-name service-name service-port fqdns app-name]} config]
    (->
     (yaml/load-as-edn "ingress/ingress.yaml")
     (assoc-in [:metadata :name] ingress-name)
     (assoc-in [:metadata :labels :app.kubernetes.part-of] app-name)
     (assoc-in [:spec :tls 0 :secretName] cert-name)
     (assoc-in [:spec :tls 0 :hosts] fqdns)
     (assoc-in [:spec :rules] (mapv (partial generate-host-rule service-name service-port) fqdns)))))

(defn-spec generate-certificate pred/map-or-seq?
  [config certificate?]
  (let [{:keys [cert-name issuer fqdns app-name]
         :or {issuer "staging"}} config
        letsencrypt-issuer (name issuer)]
    (->
     (yaml/load-as-edn "ingress/certificate.yaml")
     (assoc-in [:metadata :name] cert-name)
     (assoc-in [:metadata :labels :app.kubernetes.part-of] app-name)
     (assoc-in [:spec :secretName] cert-name)
     (assoc-in [:spec :commonName] (first fqdns))
     (assoc-in [:spec :dnsNames] fqdns)
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer))))
