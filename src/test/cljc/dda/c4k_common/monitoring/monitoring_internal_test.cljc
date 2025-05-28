(ns dda.c4k-common.monitoring.monitoring-internal-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.string :as str]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.monitoring.monitoring-internal :as cut]))

(st/instrument `cut/generate-stateful-set)
(st/instrument `cut/generate-agent-config)
(st/instrument `cut/generate-config-secret)

(def conf {:cluster-name "clustername"
           :cluster-stage "test"
           :grafana-cloud-url "https://some.url/with/path"})

(def auth {:grafana-cloud-user "user"
           :grafana-cloud-password "password"
           :hetzner-cloud-ro-token "ro-token"})

(def invalid-conf {:cluster-name "clustername"
                   :cluster-stage "test"
                   :grafana-clud-url "https://some.url/with/path"})

(def invalid-auth {:grafana-cloud-user "user"
                   :grafana-clod-password "password"
                   :hetzner-cloud-ro-token "ro-token"})

(deftest should-not-generate-config
  (is (thrown?
       #?(:clj Exception :cljs js/Error)
       (cut/generate-config-secret invalid-conf auth))))

(deftest should-not-generate-auth
  (is (thrown?
       #?(:clj Exception :cljs js/Error)
       (cut/generate-config-secret conf invalid-auth))))


(deftest should-generate-prometheus-remote-write-auth
  (is (= {:username "user",
          :password "password"}
         (get-in
          (cut/generate-prometheus-config conf auth)
          [:remote_write 0 :basic_auth]))))

(deftest should-generate-prometheus-external-labels
  (is (= {:cluster "clustername",
          :stage "test"}
         (get-in
          (cut/generate-prometheus-config conf auth)
          [:global :external_labels]))))

(deftest should-generate-config
  (is (str/starts-with?
       (get-in
        (cut/generate-config-secret conf auth)
        [:stringData :prometheus.yaml])
       "global:\n  scrape_interval:")))

(deftest should-generate-objects
  (is (= 18
         (count (cut/config-objects conf))))
  (is (= 1
         (count (cut/auth-objects conf auth)))))