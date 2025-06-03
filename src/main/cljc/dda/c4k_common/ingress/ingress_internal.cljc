(ns dda.c4k-common.ingress.ingress-internal
  (:require
   [clojure.spec.alpha :as s]
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.namespace :as ns]
   [dda.c4k-common.predicate :as pred]))


#?(:cljs
   (defmethod yaml/load-resource :ingress [resource-name]
     (case resource-name
       "ingress/certificate.yaml"           (rc/inline "ingress/certificate.yaml")
       "ingress/host-rule.yaml"             (rc/inline "ingress/host-rule.yaml")
       "ingress/ingress.yaml"               (rc/inline "ingress/ingress.yaml")
       "ingress/middleware-ratelimit.yaml"  (rc/inline "ingress/middleware-ratelimit.yaml")
       "ingress/middleware-basic-auth.yaml" (rc/inline "ingress/middleware-basic-auth.yaml")
       (throw (js/Error. (str "Undefined Resource: " resource-name))))))


(s/def ::issuer pred/letsencrypt-issuer?)
(s/def ::service-name string?)
(s/def ::app-name string?)
(s/def ::ingress-name string?)
(s/def ::cert-name string?)
(s/def ::service-port pos-int?)
(s/def ::basic-auth-secret string?)
(s/def ::fqdn pred/fqdn-string?)
(s/def ::fqdns (s/coll-of ::fqdn))
(s/def ::average-rate pos-int?)

(s/def ::burst-rate pos-int?)

(s/def ::ingress (s/keys :req-un [::ingress-name 
                                  ::app-name
                                  ::ns/namespace
                                  ::service-name ::service-port
                                  ::issuer ::cert-name
                                  ::fqdns
                                  ::rate-limit-name
                                  ::average-rate
                                  ::burst-rate]
                         :opt-un [
                                  ::basic-auth-secret]))

(s/def ::certificate (s/keys :req-un [::fqdns ::app-name ::cert-name ::issuer ::ns/namespace]))


(s/def ::rate-limit (s/keys :req-un [::rate-limit-name
                                     ::ns/namespace
                                     ::average-rate
                                     ::burst-rate]))


(defn-spec host-rule map?
  [service-name ::service-name
   service-port ::service-port
   fqdn pred/fqdn-string?]
  (->
   (yaml/load-as-edn "ingress/host-rule.yaml")
   (cm/replace-all-matching "FQDN" fqdn)
   (cm/replace-all-matching "SERVICE_PORT" service-port)
   (cm/replace-all-matching "SERVICE_NAME" service-name)))


(defn-spec certificate map?
  [config ::certificate]
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


(defn-spec rate-limit-middleware map?
  [config ::rate-limit]
  (let [{:keys [rate-limit-name average-rate burst-rate namespace]} config]
    (->
     (yaml/load-as-edn "ingress/middleware-ratelimit.yaml")
     (assoc-in [:metadata :name] (str rate-limit-name "-ratelimit"))
     (assoc-in [:metadata :namespace] namespace)
     (assoc-in [:spec :rateLimit :average] average-rate)
     (assoc-in [:spec :rateLimit :burst] burst-rate))))

(defn-spec basic-auth-middleware seq?
  [config ::ingress]
  (let [{:keys [basic-auth-secret app-name namespace]} config]
    (if (some? basic-auth-secret)
      [(->
        (yaml/load-as-edn "ingress/middleware-basic-auth.yaml")
        (assoc-in [:metadata :name] (str app-name "-auth"))
        (assoc-in [:metadata :namespace] namespace)
        (assoc-in [:spec :basicAuth :secret] basic-auth-secret))]
      [])))


(defn-spec ingress map?
  [config ::ingress]
  (let [{:keys [ingress-name cert-name service-name service-port 
                fqdns app-name rate-limit-name namespace
                basic-auth-secret]} config]
    (->
     (yaml/load-as-edn "ingress/ingress.yaml")
     (assoc-in [:metadata :name] ingress-name)
     (assoc-in [:metadata :namespace] namespace)
     (assoc-in [:metadata :labels :app.kubernetes.part-of] app-name)
     (assoc-in [:metadata :annotations]
               {:traefik.ingress.kubernetes.io/router.entrypoints
                "web, websecure"
                :traefik.ingress.kubernetes.io/router.middlewares
                (if (some? basic-auth-secret)
                  (str "default-redirect-https@kubernetescrd, " 
                       namespace "-" rate-limit-name "-ratelimit@kubernetescrd, "
                       namespace "-" app-name "-auth@kubernetescrd" )
                  (str "default-redirect-https@kubernetescrd, "
                       namespace "-" rate-limit-name "-ratelimit@kubernetescrd"))
                :metallb.universe.tf/address-pool "public"})
     (assoc-in [:spec :tls 0 :secretName] cert-name)
     (assoc-in [:spec :tls 0 :hosts] fqdns)
     (assoc-in [:spec :rules]
               (mapv (partial host-rule service-name service-port) fqdns)))))

(defn-spec config-objects seq?
  [config ::ingress]
  (cm/concat-vec
   [(certificate config)
    (rate-limit-middleware config)]
   (basic-auth-middleware config)
   [(ingress config)]))
