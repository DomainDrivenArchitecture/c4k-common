(ns dda.c4k-common.core-test
  (:require
   #?(:cljs [shadow.resource :as rc])
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.alpha :as s]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.core :as cut]))

#?(:cljs
   (defmethod yaml/load-resource :common-test [resource-name]
     (case resource-name
       "common-test/valid-auth.yaml"   (rc/inline "common-test/valid-auth.yaml")
       "common-test/valid-config.yaml" (rc/inline "common-test/valid-config.yaml")
       (throw (js/Error. "Undefined Resource!")))))

(def conf (yaml/load-as-edn "common-test/valid-config.yaml"))
(def auth (yaml/load-as-edn "common-test/valid-auth.yaml"))

(deftest validate-valid-resources
  (is (s/valid? cut/config? conf))
  (is (s/valid? cut/auth? auth)))
