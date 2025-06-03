(ns dda.c4k-common.ingress.ingress-internal-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.ingress.ingress-internal :as cut]))

(st/instrument `cut/host-rule)
(st/instrument `cut/certificate)
(st/instrument `cut/rate-limit-middleware)
(st/instrument `cut/basic-auth-middleware)
(st/instrument `cut/ingress)

(def config {:ingress-name "test-io-https-ingress"
             :app-name "c4k-common-app"
             :namespace "default"
             :service-name "test-io-service" :service-port 80
             :rate-limit-name "normal"
             :average-rate 10
             :burst-rate 100
             :issuer "prod" :cert-name "noname"
             :fqdns ["test.de"]})

(deftest should-generate-rule
  (is (= {:host "test.com",
          :http
          {:paths
           [{:pathType "Prefix",
             :path "/",
             :backend
             {:service {:name "myservice", :port {:number 3000}}}}]}}
         (cut/host-rule "myservice" 3000 "test.com"))))


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
         (cut/certificate {:fqdns ["test.de" "test.org" "www.test.de" "www.test.org"]
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
         (cut/certificate {:fqdns ["test.de" "test.org" "www.test.de" "www.test.org"]
                                    :app-name "c4k-common-app"
                                    :cert-name "test-io-cert"
                                    :issuer "prod"
                                    :namespace "myapp"}))))


(deftest should-generate-middleware-ratelimit
  (is (= {:apiVersion "traefik.io/v1alpha1",
          :kind "Middleware",
          :metadata {:name "normal-ratelimit"
                     :namespace "myapp",},
          :spec {:rateLimit {:average 10, :burst 5}}}
         (cut/rate-limit-middleware {:rate-limit-name "normal"
                                     :namespace "myapp"
                                     :average-rate 10, :burst-rate 5}))))

(deftest should-generate-middleware-basic-auth
  (is (= []
         (cut/basic-auth-middleware config)))
  (is (= [{:apiVersion "traefik.io/v1alpha1",
           :kind "Middleware",
           :metadata {:name "c4k-common-app-auth", :namespace "default"},
           :spec {:basicAuth {:secret "basic-auth-secret"}}}]
         (cut/basic-auth-middleware (merge config
                                           {:basic-auth-secret "basic-auth-secret"})))))


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
                         "default-redirect-https@kubernetescrd, myapp-normal-ratelimit@kubernetescrd"
                         :metallb.universe.tf/address-pool "public"}}}
         (dissoc (cut/ingress
                  (merge config
                         {:namespace "myapp"
                          :fqdns ["test.de" "www.test.de" "test-it.de"
                                  "www.test-it.de"]})) :spec)))
  (is (= {:name "test-io-https-ingress",
          :namespace "default",
          :labels {:app.kubernetes.part-of "c4k-common-app"},
          :annotations {:traefik.ingress.kubernetes.io/router.entrypoints
                        "web, websecure"
                        :traefik.ingress.kubernetes.io/router.middlewares
                        "default-redirect-https@kubernetescrd, default-normal-ratelimit@kubernetescrd",
                        :metallb.universe.tf/address-pool "public"}}
         (:metadata (cut/ingress config))))
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
         (:spec (cut/ingress (merge config
                                    {:issuer "prod" :cert-name "test-io-cert"
                                     :fqdns ["test.de" "www.test.de"
                                             "test-it.de"
                                             "www.test-it.de"]})))))
  (is (= "default-redirect-https@kubernetescrd, default-normal-ratelimit@kubernetescrd, default-c4k-common-app-auth@kubernetescrd"
         (get-in (cut/ingress (merge config
                                     {:basic-auth-secret "basic-auth-secret"}))
                 [:metadata :annotations :traefik.ingress.kubernetes.io/router.middlewares]))))

(deftest should-generate-objects
  (is (= 3
         (count (cut/config-objects config))))
  (is (= 4
         (count (cut/config-objects 
                 (merge config 
                        {:basic-auth-secret "basic-auth-secret"}))))))