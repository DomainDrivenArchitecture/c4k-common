(ns dda.c4k-common.monitoring-test
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.monitoring :as cut]))

(st/instrument `cut/config-objects)
(st/instrument `cut/auth-objects)

(def conf {:cluster-name "clustername"
           :cluster-stage "test"
           :mode {:remote-write-url "https://some.url/with/path"}})

(def auth {:remote-write-user "user"
           :remote-write-password "password"})


(deftest should-generate
  (is (= 19
         (count (cut/config-objects conf))))
    (is (= 20
         (count (cut/config-objects
                 (merge conf {:mode {:storage-size-gb 20
                                     :storage-class "local-path"}})))))
  (is (= 1
         (count (cut/auth-objects conf auth)))))
