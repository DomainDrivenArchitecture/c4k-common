(ns dda.c4k-common.monitoring-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.monitoring :as cut]))

(st/instrument `cut/generate)

(def conf {:cluster-name "clustername"
           :cluster-stage "test"
           :grafana-cloud-url "https://some.url/with/path"})

(def auth {:grafana-cloud-user "user"
           :grafana-cloud-password "password"
           :hetzner-cloud-ro-token "ro-token"})


(deftest should-generate
  (is (= 17
         (count (cut/generate conf auth)))))
