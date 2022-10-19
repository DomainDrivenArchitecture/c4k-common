(ns dda.c4k-common.ingress
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
(s/def ::ingress-name string?)
(s/def ::cert-name string?)
(s/def ::service-port pos-int?)
(s/def ::fqdns (s/coll-of pred/fqdn-string?))

(def ingress? (s/keys :req-un [::fqdns ::ingress-name ::service-name ::service-port]
                      :opt-un [::issuer ::cert-name]))

(def certificate? (s/keys :req-un [::fqdns ::cert-name]
                          :opt-un [::issuer]))

#?(:cljs
   (defmethod yaml/load-resource :ingress [resource-name]
     (case resource-name
       "ingress/host-rule.yaml" (rc/inline "ingress/host-rule.yaml")
       "ingress/certificate.yaml" (rc/inline "ingress/certificate.yaml")
       "ingress/http-ingress.yaml" (rc/inline "ingress/http-ingress.yaml")
       "ingress/https-ingress.yaml" (rc/inline "ingress/https-ingress.yaml")
       (throw (js/Error. "Undefined Resource!")))))

#?(:cljs
   (defmethod yaml/load-as-edn :ingress [resource-name]
     (yaml/from-string (yaml/load-resource resource-name))))

(defn-spec generate-host-rule  pred/map-or-seq?
  [service-name ::service-name
   service-port ::service-port   
   fqdn pred/fqdn-string?]
    (->
     (yaml/load-as-edn "ingress/host-rule.yaml")
     (cm/replace-all-matching-values-by-new-value "FQDN" fqdn)
     (cm/replace-all-matching-values-by-new-value "SERVICE_PORT" service-port)
     (cm/replace-all-matching-values-by-new-value "SERVICE_NAME" service-name)))

(defn-spec generate-http-ingress pred/map-or-seq?
  [config ingress?]
  (let [{:keys [ingress-name service-name service-port fqdns]} config]
    (->
     (yaml/load-as-edn "ingress/http-ingress.yaml")
     (assoc-in [:metadata :name] ingress-name)     
     (assoc-in [:spec :rules] (mapv (partial generate-host-rule service-name service-port) fqdns)))))

(defn-spec generate-https-ingress pred/map-or-seq?
  [config ingress?]
  (let [{:keys [ingress-name cert-name service-name service-port fqdns]} config]
    (->
     (yaml/load-as-edn "ingress/https-ingress.yaml")
     (assoc-in [:metadata :name] ingress-name)
     (assoc-in [:spec :tls 0 :secretName] cert-name)
     (assoc-in [:spec :tls 0 :hosts] fqdns)
     (assoc-in [:spec :rules] (mapv (partial generate-host-rule service-name service-port) fqdns)))))

(defn-spec generate-certificate pred/map-or-seq?
  [config certificate?]
  (let [{:keys [cert-name issuer fqdns]
         :or {issuer "staging"}} config
        letsencrypt-issuer (name issuer)]
    (->
     (yaml/load-as-edn "ingress/certificate.yaml")     
     (assoc-in [:metadata :name] cert-name)
     (assoc-in [:spec :secretName] cert-name)
     (assoc-in [:spec :commonName] (first fqdns))
     (assoc-in [:spec :dnsNames] fqdns)
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer))))
 