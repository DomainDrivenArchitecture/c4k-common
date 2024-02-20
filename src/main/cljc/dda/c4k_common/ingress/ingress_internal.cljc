(ns dda.c4k-common.ingress.ingress-internal
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:cljs [dda.c4k-common.macros :refer-macros [inline-resources]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.namespace :as ns]
   [dda.c4k-common.predicate :as pred]))

(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::service-name string?)
(s/def ::app-name string?)
(s/def ::ingress-name string?)
(s/def ::cert-name string?)
(s/def ::service-port pos-int?)
(s/def ::fqdns (s/coll-of pred/fqdn-string?))
(s/def ::average-rate pos-int?)
(s/def ::burst-rate pos-int?)

(def ingress? (s/keys :req-un [::ingress-name ::app-name 
                               ::ns/namespace 
                               ::service-name ::service-port                                
                               ::issuer ::cert-name 
                               ::fqdns]
                      :opt-un [::rate-limit-name]))

(def certificate? (s/keys :req-un [::fqdns ::app-name ::cert-name ::issuer ::ns/namespace]))

(def rate-limit-config? (s/keys :req-un [::rate-limit-name ::average-rate ::burst-rate]))


(defn-spec generate-host-rule map?
  [service-name ::service-name
   service-port ::service-port
   fqdn pred/fqdn-string?]
  (->
   (yaml/load-as-edn "ingress/host-rule.yaml")
   (cm/replace-all-matching-values-by-new-value "FQDN" fqdn)
   (cm/replace-all-matching-values-by-new-value "SERVICE_PORT" service-port)
   (cm/replace-all-matching-values-by-new-value "SERVICE_NAME" service-name)))


(defn-spec generate-certificate map?
  [config certificate?]
  (let [{:keys [cert-name issuer fqdns app-name namespace]} config
        letsencrypt-issuer (name issuer)]
    (->
     (yaml/load-as-edn "ingress/certificate.yaml")
     (assoc-in [:metadata :name] cert-name)
     (assoc-in [:metadata :namespace] namespace)
     (assoc-in [:metadata :labels :app.kubernetes.part-of] app-name)
     (assoc-in [:spec :secretName] cert-name)
     (assoc-in [:spec :commonName] (first fqdns))
     (assoc-in [:spec :dnsNames] fqdns)
     (assoc-in [:spec :issuerRef :name] letsencrypt-issuer))))


(defn-spec generate-rate-limit-middleware map?
  [config rate-limit-config?]
  (let [{:keys [rate-limit-name average-rate burst-rate]} config]
    (->
     (yaml/load-as-edn "ingress/middleware-ratelimit.yaml")
     (assoc-in [:metadata :name] (str rate-limit-name "-ratelimit"))
     (assoc-in [:spec :rateLimit :average] average-rate)
     (assoc-in [:spec :rateLimit :burst] burst-rate))))


(defn-spec generate-ingress map?
  [config ingress?]
  (let [{:keys [ingress-name cert-name service-name service-port 
                fqdns app-name rate-limit-name namespace]} config]
    (->
     (yaml/load-as-edn "ingress/ingress.yaml")
     (assoc-in [:metadata :name] ingress-name)
     (assoc-in [:metadata :namespace] namespace)
     (assoc-in [:metadata :labels :app.kubernetes.part-of] app-name)
     (assoc-in [:metadata :annotations]
               {:traefik.ingress.kubernetes.io/router.entrypoints
                "web, websecure"
                :traefik.ingress.kubernetes.io/router.middlewares
                (if rate-limit-name
                  (str "default-redirect-https@kubernetescrd, " rate-limit-name "-ratelimit@kubernetescrd")
                  "default-redirect-https@kubernetescrd")
                :metallb.universe.tf/address-pool "public"})
     (assoc-in [:spec :tls 0 :secretName] cert-name)
     (assoc-in [:spec :tls 0 :hosts] fqdns)
     (assoc-in [:spec :rules]
               (mapv (partial generate-host-rule service-name service-port) fqdns)))))


#?(:cljs
   (defmethod yaml/load-resource :ingress [resource-name]
     (get (inline-resources "ingress") resource-name)))
