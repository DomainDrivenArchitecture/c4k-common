(ns dda.c4k-common.monitoring.monitoring-regex-test
  (:require
   [clojure.test :refer [deftest is are testing run-tests]]
   [data-test :refer :all]
   [dda.c4k-common.monitoring.monitoring-internal :as cut]))

(defn filter-by-regex
  [regex-str collection]
  (filterv #(re-matches (re-pattern regex-str) %)
           collection))

(defdatatest should-filter-metrik [input expected]
  (is (= (:node-metrics expected)
         (filter-by-regex
          (:node-regex cut/metric-regex)
          (into (:node-metrics expected) (:additional-node-metrics input)))))
  (is (= (:traefik-metrics expected)
         (filter-by-regex
          (:traefik-regex cut/metric-regex)
          (into (:traefik-metrics expected) (:additional-traefik-metrics input)))))
  (is (= (:kube-state-metrics expected)
         (filter-by-regex
          (:kube-state-regex cut/metric-regex)
          (into (:kube-state-metrics expected) (:additional-kube-state-metrics input))))))
