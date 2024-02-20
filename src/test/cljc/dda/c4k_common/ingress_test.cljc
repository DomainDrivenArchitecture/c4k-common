(ns dda.c4k-common.ingress-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.ingress :as cut]))

(st/instrument `cut/generate-ingress)
(st/instrument `cut/generate-certificate)
(st/instrument `cut/generate-ingress-and-cert)
(st/instrument `cut/generate-simple-ingress)

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
           :dnsNames ["test.de"],
           :issuerRef {:name "staging", :kind "ClusterIssuer"}}}
         (cut/generate-certificate {:fqdns ["test.de"]
                                    :app-name "c4k-common-app"
                                    :cert-name "test-io-cert"}))))


(deftest should-generate-ingress
  (is (= {:name "test-io-https-ingress",
          :namespace "default",
          :labels {:app.kubernetes.part-of "c4k-common-app"},
          :annotations {:traefik.ingress.kubernetes.io/router.entrypoints
                        "web, websecure"
                        :traefik.ingress.kubernetes.io/router.middlewares
                        "default-redirect-https@kubernetescrd",
                        :metallb.universe.tf/address-pool "public"}}
         (:metadata (cut/generate-ingress
                     {:ingress-name "test-io-https-ingress"
                      :app-name "c4k-common-app" 
                      :service-name "test-io-service" :service-port 80
                      :cert-name "myCert"
                      :fqdns ["test.de"]})))))


(deftest should-generate-ingress-and-cert
  (is (= [{:apiVersion "cert-manager.io/v1",
           :kind "Certificate",
           :metadata
           {:name "web",
            :labels {:app.kubernetes.part-of "web"},
            :namespace "default"},
           :spec
           {:secretName "web",
            :commonName "test.jit.si",
            :duration "2160h",
            :renewBefore "720h",
            :dnsNames ["test.jit.si"],
            :issuerRef {:name "staging", :kind "ClusterIssuer"}}}
          {:apiVersion "networking.k8s.io/v1",
           :kind "Ingress",
           :metadata
           {:name "web",
            :namespace "default",
            :labels {:app.kubernetes.part-of "web"},
            :annotations
            {:traefik.ingress.kubernetes.io/router.entrypoints "web, websecure",
             :traefik.ingress.kubernetes.io/router.middlewares
             "default-redirect-https@kubernetescrd",
             :metallb.universe.tf/address-pool "public"}},
           :spec
           {:tls [{:hosts ["test.jit.si"], :secretName "web"}],
            :rules
            [{:host "test.jit.si",
              :http {:paths [{:path "/",
                              :pathType "Prefix",
                              :backend
                              {:service {:name "web",
                                         :port {:number 80}}}}]}}]}}]
         (cut/generate-ingress-and-cert {:fqdns ["test.jit.si"]
                                          :service-name "web"
                                          :service-port 80}))))

(deftest should-generate-simple-ingress
  (is (= [{:apiVersion "cert-manager.io/v1",
           :kind "Certificate",
           :metadata
           {:name "web",
            :labels {:app.kubernetes.part-of "web"},
            :namespace "default"},
           :spec
           {:secretName "web",
            :commonName "test.jit.si",
            :duration "2160h",
            :renewBefore "720h",
            :dnsNames ["test.jit.si"],
            :issuerRef {:name "staging", :kind "ClusterIssuer"}}}
          {:apiVersion "traefik.containo.us/v1alpha1",
           :kind "Middleware",
           :metadata {:name "web-ratelimit"},
           :spec {:rateLimit {:average 10, :burst 10}}}
          {:apiVersion "networking.k8s.io/v1",
           :kind "Ingress",
           :metadata
           {:name "web",
            :namespace "default",
            :labels {:app.kubernetes.part-of "web"},
            :annotations
            {:traefik.ingress.kubernetes.io/router.entrypoints "web, websecure",
             :traefik.ingress.kubernetes.io/router.middlewares
             "default-redirect-https@kubernetescrd, web-ratelimit@kubernetescrd",
             :metallb.universe.tf/address-pool "public"}},
           :spec
           {:tls [{:hosts ["test.jit.si"], :secretName "web"}],
            :rules
            [{:host "test.jit.si",
              :http {:paths [{:path "/",
                              :pathType "Prefix",
                              :backend
                              {:service {:name "web",
                                         :port {:number 80}}}}]}}]}}]
         (cut/generate-simple-ingress {:fqdns ["test.jit.si"]
                                       :service-name "web"
                                       :service-port 80}))))