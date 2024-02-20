(ns dda.c4k-common.macros-test
  (:require 
   [cljs.test :refer-macros [deftest is]]
   [dda.c4k-common.macros :refer-macros [inline-resources]]))

(deftest should-count-inline-resources
  (is (= 4 (count (inline-resources "ingress")))))

(deftest should-inline-resources
  (let [resource-path (fn [name] (str "dda/c4k_common/inline_resources_test/" name))]
    (is (= "1" (get (inline-resources "dda/c4k_common/inline_resources_test") (resource-path "inline_resource_1.yaml"))))
    (is (= "2" (get (inline-resources "dda/c4k_common/inline_resources_test") (resource-path "inline_resource_2.yaml"))))
    (is (= "3" (get (inline-resources "dda/c4k_common/inline_resources_test") (resource-path "inline_resource_3.yaml"))))))
