(ns dda.c4k-common.ingress.ingress-internal-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.ingress.ingress-internal :as cut]))

(st/instrument `cut/generate-host-rule)
(st/instrument `cut/generate-certificate)
(st/instrument `cut/generate-rate-limit-middleware)
(st/instrument `cut/generate-ingress)


(deftest should-generate-rule
  (is (= {:host "test.com",
          :http
          {:paths
           [{:pathType "Prefix",
             :path "/",
             :backend
             {:service {:name "myservice", :port {:number 3000}}}}]}}
         (cut/generate-host-rule "myservice" 3000 "test.com"))))


(deftest should-generate-certificate
  (is (= {:apiVersion "cert-manager.io/v1",
          :kind "Certificate",
          :metadata {:name "test-io-cert",
                     :namespace "default",
                     :labels {:app.kubernetes.part-of "c4k-common-app"}},
          :spec
          {:secretName "test-io-cert",
           :commonName "test.de",
           :duration "2160h",
           :renewBefore "720h",
           :dnsNames ["test.de" "test.org" "www.test.de" "www.test.org"],
           :issuerRef {:name "prod", :kind "ClusterIssuer"}}}
         (cut/generate-certificate {:fqdns ["test.de" "test.org" "www.test.de" "www.test.org"]
                                    :app-name "c4k-common-app"
                                    :cert-name "test-io-cert"
                                    :issuer "prod"
                                    :namespace "default"})))
  (is (= {:apiVersion "cert-manager.io/v1",
          :kind "Certificate",
          :metadata {:name "test-io-cert",
                     :namespace "myapp",
                     :labels {:app.kubernetes.part-of "c4k-common-app"}},
          :spec
          {:secretName "test-io-cert",
           :commonName "test.de",
           :duration "2160h",
           :renewBefore "720h",
           :dnsNames ["test.de" "test.org" "www.test.de" "www.test.org"],
           :issuerRef {:name "prod", :kind "ClusterIssuer"}}}
         (cut/generate-certificate {:fqdns ["test.de" "test.org" "www.test.de" "www.test.org"]
                                    :app-name "c4k-common-app"
                                    :cert-name "test-io-cert"
                                    :issuer "prod"
                                    :namespace "myapp"}))))


(deftest should-generate-middleware-ratelimit
  (is (= {:apiVersion "traefik.containo.us/v1alpha1",
          :kind "Middleware",
          :metadata {:name "normal-ratelimit"
                     :namespace "myapp",},
          :spec {:rateLimit {:average 10, :burst 5}}}
         (cut/generate-rate-limit-middleware {:rate-limit-name "normal"
                                              :namespace "myapp"
                                              :average-rate 10, :burst-rate 5}))))


(deftest should-generate-ingress
  (is (= {:apiVersion "networking.k8s.io/v1",
          :kind "Ingress",
          :metadata
          {:namespace "myapp",
           :name "test-io-https-ingress",
           :labels {:app.kubernetes.part-of "c4k-common-app"},
           :annotations {:traefik.ingress.kubernetes.io/router.entrypoints
                         "web, websecure"
                         :traefik.ingress.kubernetes.io/router.middlewares
                         "default-redirect-https@kubernetescrd"
                         :metallb.universe.tf/address-pool "public"}}}
         (dissoc (cut/generate-ingress
                  {:ingress-name "test-io-https-ingress"
                   :app-name "c4k-common-app"
                   :namespace "myapp"
                   :service-name "test-io-service" :service-port 80                   
                   :issuer "prod" :cert-name "noname"
                   :fqdns ["test.de" "www.test.de" "test-it.de"
                           "www.test-it.de"]}) :spec)))
  (is (= {:name "test-io-https-ingress",
          :namespace "default",
          :labels {:app.kubernetes.part-of "c4k-common-app"},
          :annotations {:traefik.ingress.kubernetes.io/router.entrypoints
                        "web, websecure"
                        :traefik.ingress.kubernetes.io/router.middlewares
                        "default-redirect-https@kubernetescrd, default-normal-ratelimit@kubernetescrd",
                        :metallb.universe.tf/address-pool "public"}}
         (:metadata (cut/generate-ingress
                     {
                      :ingress-name "test-io-https-ingress"
                      :app-name "c4k-common-app"
                      :namespace "default"
                      :service-name "test-io-service" :service-port 80
                      :rate-limit-name "normal"
                      :issuer "prod" :cert-name "noname"
                      :fqdns ["test.de"]}))))
  (is (= {:tls
          [{:hosts
            ["test.de" "www.test.de" "test-it.de" "www.test-it.de"],
            :secretName "test-io-cert"}]
          :rules
          [{:host "test.de",
            :http
            {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "test-io-service", :port {:number 80}}}}]}}
           {:host "www.test.de",
            :http
            {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "test-io-service", :port {:number 80}}}}]}}
           {:host "test-it.de",
            :http
            {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "test-io-service", :port {:number 80}}}}]}}
           {:host "www.test-it.de",
            :http
            {:paths [{:pathType "Prefix", :path "/", :backend {:service {:name "test-io-service", :port {:number 80}}}}]}}]}
         (:spec (cut/generate-ingress {
                                       :ingress-name "test-io-https-ingress"
                                       :app-name "c4k-common-app"
                                       :namespace "default"
                                       :service-name "test-io-service" :service-port 80
                                       :issuer "prod" :cert-name "test-io-cert"
                                       :fqdns ["test.de" "www.test.de"
                                               "test-it.de"
                                               "www.test-it.de"]})))))