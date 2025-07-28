(ns dda.c4k-common.ingress-test
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.ingress :as cut]))

(st/instrument `cut/config-objects)

(def conf {:service-name "test-io-service" 
           :service-port 80
           :fqdns ["test.de"]})

(deftest should-generate
  (is (= 3
         (count (cut/config-objects conf)))))
