(ns dda.c4k-common.monitoring-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.monitoring :as cut]))

(st/instrument `cut/generate-config)
(st/instrument `cut/generate-auth)

(def conf {:cluster-name "clustername"
           :cluster-stage "test"
           :grafana-cloud-url "https://some.url/with/path"})

(def auth {:grafana-cloud-user "user"
           :grafana-cloud-password "password"
           :hetzner-cloud-ro-token "ro-token"})


(deftest should-generate
  (is (= 16
         (count (cut/generate-config conf auth))))
  (is (= 1
         (count (cut/generate-auth conf auth)))))
