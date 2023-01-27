(ns dda.c4k-common.monitoring-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.string :as s]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.monitoring :as cut]
   [dda.c4k-common.yaml :as yaml]
   [clojure.string :as str]))

(st/instrument `cut/generate)
(st/instrument `cut/generate-stateful-set)
(st/instrument `cut/generate-agent-config)
(st/instrument `cut/generate-config)

(def conf {:cluster-name "clustername"
           :cluster-stage :test
           :grafana-cloud-url "https://some.url/with/path"})

(def auth {:grafana-cloud-user "user"
           :grafana-cloud-password "password"
           :hetzner-cloud-ro-token "ro-token"})

(def invalid-conf {:cluster-name "clustername"
                   :cluster-stage :test
                   :grafana-clud-url "https://some.url/with/path"})

(def invalid-auth {:grafana-cloud-user "user"
                   :grafana-clod-password "password"
                   :hetzner-cloud-ro-token "ro-token"})

(deftest should-not-generate-config
  (is (thrown?
       Exception
       (cut/generate-config invalid-conf auth))))

(deftest should-not-generate-auth
  (is (thrown?
       Exception
       (cut/generate-config conf invalid-auth))))

(deftest should-generate
  (is (= 17
         (count (cut/generate conf auth)))))

(deftest should-generate-prometheus-remote-write-auth
  (is (= {:username "user",
          :password "password"}
         (get-in
          (cut/generate-prometheus-config conf auth)
          [:remote_write 0 :basic_auth]))))

(deftest should-generate-prometheus-external-labels
  (is (= {:cluster "clustername",
          :stage :test}
         (get-in
          (cut/generate-prometheus-config conf auth)
          [:global :external_labels]))))

(deftest should-generate-config
  (is (s/starts-with?
       (get-in
        (cut/generate-config conf auth)
        [:stringData :prometheus.yaml])
       "global:\n  scrape_interval:")))