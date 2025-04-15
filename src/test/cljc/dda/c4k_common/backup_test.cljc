(ns dda.c4k-common.backup-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.backup :as cut]))

(st/instrument `cut/config-objects)
(st/instrument `cut/auth-objects)

(def config {:namespace "ns"
             :app-name "app-name"
             :image "image"
             :backup-postgres true
             :restic-repository "repo"
             :backup-volume-mount
             {:mount-name "forgejo-data-volume"
              :pvc-name "forgejo-data-pvc"
              :mount-path "/var/backups"}})

(deftest should-generate
  (is (= 3
         (count (cut/config-objects config))))
  (is (= 1
         (count (cut/auth-objects config
                                  {:restic-password "rpw"
                                   :restic-new-password "nrpw"
                                   :aws-access-key-id "aki"
                                   :aws-secret-access-key "asak"})))))