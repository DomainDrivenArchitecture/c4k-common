(ns dda.c4k-common.ingress
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   #?(:cljs [dda.c4k-common.macros :refer-macros [inline-resources]])
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
(s/def ::average-rate pos-int?)
(s/def ::burst-rate pos-int?)

(def simple-ingress? (s/keys :req-un [::fqdns ::service-name ::service-port]
                             :opt-un [::issuer ::average-rate]))

(def ingress? (s/keys :req-un [::fqdns ::app-name ::ingress-name ::service-name ::service-port]
                      :opt-un [::issuer ::cert-name ::rate-limit-name]))

(def certificate? (s/keys :req-un [::fqdns ::app-name ::cert-name]
                          :opt-un [::issuer]))

(def rate-limit-config? (s/keys :req-un [::rate-limit-name
                                         ::average-rate
                                         ::burst-rate]))

(def simple-ingress-defaults {:issuer "staging"
                              :average-rate 10})

#?(:cljs
   (defmethod yaml/load-resource :ingress [resource-name]
     (get (inline-resources "ingress") resource-name)))

(defn-spec generate-host-rule  pred/map-or-seq?
  [service-name ::service-name
   service-port ::service-port
   fqdn pred/fqdn-string?]
  (->
   (yaml/load-as-edn "ingress/host-rule.yaml")
   (cm/replace-all-matching-values-by-new-value "FQDN" fqdn)
   (cm/replace-all-matching-values-by-new-value "SERVICE_PORT" service-port)
   (cm/replace-all-matching-values-by-new-value "SERVICE_NAME" service-name)))

(defn-spec generate-rate-limit-middleware pred/map-or-seq?
  [config rate-limit-config?]
  (let [{:keys [rate-limit-name average-rate burst-rate]} config]
    (->
     (yaml/load-as-edn "ingress/middleware-ratelimit.yaml")
     (assoc-in [:metadata :name] (str rate-limit-name "-ratelimit"))
     (assoc-in [:spec :rateLimit :average] average-rate)
     (assoc-in [:spec :rateLimit :burst] burst-rate))))

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

(defn-spec generate-ingress pred/map-or-seq?
  [config ingress?]
  (let [{:keys [ingress-name cert-name service-name service-port fqdns app-name rate-limit-name]} config]
    (->
     (yaml/load-as-edn "ingress/ingress.yaml")
     (assoc-in [:metadata :name] ingress-name)
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

(defn-spec generate-ingress-and-cert any?
  [simple-ingress-config simple-ingress?]
  (let [{:keys [service-name]} simple-ingress-config
        config (merge {:app-name service-name
                       :ingress-name service-name
                       :cert-name service-name}
                      simple-ingress-defaults
                      simple-ingress-config)]
    [(generate-certificate config)
     (generate-ingress config)]))

(defn-spec generate-simple-ingress any?
  [simple-ingress-config simple-ingress?]
  (let [{:keys [service-name]} simple-ingress-config
        config (merge {:app-name service-name
                       :ingress-name service-name
                       :cert-name service-name
                       :rate-limit-name service-name}
                      simple-ingress-defaults
                      simple-ingress-config)
        {:keys [average-rate]} config]
    [(generate-certificate config)
     (generate-rate-limit-middleware {:rate-limit-name service-name
                                      :average-rate average-rate 
                                      :burst-rate average-rate})
     (generate-ingress config)]))