(ns dda.c4k-common.ingress-cert-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.ingress-cert :as cut]))

(st/instrument `cut/generate-host-rule)
(st/instrument `cut/generate-ingress)
(st/instrument `cut/generate-certificate)

(deftest should-generate-rule
  (is (= {:host "test.com",
          :http
          {:paths
           [{:pathType "Prefix",
             :path "/",
             :backend
             {:service {:name "myservice", :port {:number 3000}}}}]}}
         (cut/generate-host-rule "myservice" 3000 "test.com"))))

(deftest should-generate-ingress
  (is (= {:apiVersion "networking.k8s.io/v1",
          :kind "Ingress",
          :metadata
          {:name "test-io-https-ingress",
           :namespace "default",
           :labels {:app.kubernetes.part-of "c4k-common-app"},
           :annotations #:traefik.ingress.kubernetes.io{:router.entrypoints "web, websecure", :router.middlewares "default-redirect-https@kubernetescrd"}}}
         (dissoc (cut/generate-ingress
                  {:issuer "prod"
                   :service-name "test-io-service"
                   :app-name "c4k-common-app"
                   :service-port 80
                   :ingress-name "test-io-https-ingress"
                   :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]}) :spec)))
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
         (:spec (cut/generate-ingress {:issuer "prod"
                                             :app-name "c4k-common-app"
                                             :service-name "test-io-service"
                                             :service-port 80
                                             :ingress-name "test-io-https-ingress"
                                             :cert-name "test-io-cert"
                                             :fqdns ["test.de" "www.test.de" "test-it.de" "www.test-it.de"]})))))

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
           :renewBefore "360h",
           :dnsNames ["test.de" "test.org" "www.test.de" "www.test.org"],
           :issuerRef {:name "prod", :kind "ClusterIssuer"}}}
         (cut/generate-certificate {:fqdns ["test.de" "test.org" "www.test.de" "www.test.org"]
                                    :app-name "c4k-common-app"
                                    :cert-name "test-io-cert"
                                    :issuer "prod"}))))