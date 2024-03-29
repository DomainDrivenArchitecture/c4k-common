(ns dda.c4k-common.macros-test
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [dda.c4k-common.macros :as cut :refer [inline-resources]]))

(deftest should-count-inline-resources
  (is (= 3 (count (inline-resources "dda/c4k_common/inline_resources_test")))))

(deftest should-inline-resources
  (let [resource-path (fn [name] (str "dda/c4k_common/inline_resources_test/" name))
        inlined-resources (inline-resources "dda/c4k_common/inline_resources_test")]
    (is (= "1" (get inlined-resources (resource-path "inline_resource_1.yaml"))))
    (is (= "2" (get inlined-resources (resource-path "inline_resource_2.yaml"))))
    (is (= "3" (get inlined-resources (resource-path "inline_resource_3.yaml"))))))

(deftest should-inline-jar-resources
  (let [jar-url (java.net.URL. "jar:file:./src/test/resources/dda/c4k_common/inline_jar_test/test.jar!/inline_resources_test/")
        inlined-resources (cut/inline-resource-jar jar-url)]
    (is (= "1" (get inlined-resources "inline_resources_test/inline_resource_1.yaml")))
    (is (= "2" (get inlined-resources "inline_resources_test/inline_resource_2.yaml")))
    (is (= "3" (get inlined-resources "inline_resources_test/inline_resource_3.yaml")))))