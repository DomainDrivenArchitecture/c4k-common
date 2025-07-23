(ns dda.c4k-common.core-test
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [clojure.spec.alpha :as s]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.core :as cut]))

(def conf (yaml/load-as-edn "common-test/valid-config.yaml"))
(def auth (yaml/load-as-edn "common-test/valid-auth.yaml"))

(deftest validate-valid-resources
  (is (s/valid? cut/config? conf))
  (is (s/valid? cut/auth? auth)))
