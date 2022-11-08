(ns dda.c4k-common.yaml-test
  (:require
   [cljs.test :refer-macros [deftest is are testing run-tests]]
   [shadow.resource :as rc]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.yaml :as cut]))

(st/instrument `cut/from-string)
(st/instrument `cut/to-string)
(st/instrument `cut/dispatch-by-resource-name)

(defmethod cut/load-resource :test [resource-name]
  (case resource-name
    "test/ingress_test.yaml" (rc/inline "test/ingress_test.yaml")
    (throw (js/Error. "Undefined Resource!"))))

(deftest should-dispatch-by-resource-name
  (is (= :postgres
         (cut/dispatch-by-resource-name "postgres/etc"))))

(deftest should-parse-yaml-string
  (is (= {:hallo "welt"}
         (cut/from-string "hallo: welt"))))

(deftest should-generate-yaml-string
  (is (= "hallo: welt
"
         (cut/to-string {:hallo "welt"}))))

(deftest should-convert-config-yml-to-map
  (is (= {:apiVersion "networking.k8s.io/v1beta1"
          :kind "Ingress"
          :metadata
          {:name "ingress-cloud"
           :annotations
           {:cert-manager.io/cluster-issuer
            "letsencrypt-staging-issuer"
            :nginx.ingress.kubernetes.io/proxy-body-size "256m"
            :nginx.ingress.kubernetes.io/ssl-redirect "true"
            :nginx.ingress.kubernetes.io/rewrite-target "/"
            :nginx.ingress.kubernetes.io/proxy-connect-timeout "300"
            :nginx.ingress.kubernetes.io/proxy-send-timeout "300"
            :nginx.ingress.kubernetes.io/proxy-read-timeout "300"}
           :namespace "default"}
          :spec
          {:tls [{:hosts ["fqdn"], :secretName "keycloak-secret"}]
           :rules
           [{:host "fqdn"
             :http
             {:paths
              [{:backend
                {:serviceName "keycloak", :servicePort 8080}}]}}
            {:host "fqdn"
             :http
             {:paths
              [{:backend
                {:serviceName "another_keycloak"
                 :servicePort 8081}}]}}]}}
         (cut/load-as-edn "test/ingress_test.yaml"))))
