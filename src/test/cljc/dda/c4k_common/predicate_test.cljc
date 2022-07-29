(ns dda.c4k-common.predicate-test
  (:require
   #?(:clj [clojure.test :refer [deftest is are testing run-tests]]
      :cljs [cljs.test :refer-macros [deftest is are testing run-tests]])
   [dda.c4k-common.predicate :as cut]))

(deftest test-bash-env-string?
  (is (true? (cut/bash-env-string? "abcd")))
  (is (false? (cut/bash-env-string? "$abdc")))
  (is (false? (cut/bash-env-string? "\"abdc"))))

(deftest test-fqdn-string?
  (is (true? (cut/fqdn-string? "abc.test.de")))
  (is (true? (cut/fqdn-string? "abc.def.fed.cba.abc.def.fed.cba")))
  (is (true? (cut/fqdn-string? "123.456.de")))
  (is (false? (cut/fqdn-string? "123.456.789")))
  (is (false? (cut/fqdn-string? "test&test.de"))))

(deftest test-string-of-separated-by?
  (is (true? (cut/string-of-separated-by? cut/bash-env-string? #":" "abcd")))
  (is (true? (cut/string-of-separated-by? cut/bash-env-string? #":" "abcd:efgh")))
  (is (false? (cut/string-of-separated-by? cut/bash-env-string? #":" "abcd:ef$gh")))
  (is (true? (cut/string-of-separated-by? cut/fqdn-string? #"," "test.de,test-gmbh.de,test-llc.com")))
  (is (false? (cut/string-of-separated-by? cut/fqdn-string? #"," "test.123,test.de"))))

(deftest test-letsencrypt-issuer?
  (is (false? (cut/letsencrypt-issuer? "issuer")))
  (is (true? (cut/letsencrypt-issuer? "staging")))
  (is (true? (cut/letsencrypt-issuer? "prod"))))

(deftest test-map-or-seq?
  (is (true? (cut/map-or-seq? {:a 1 :b 2})))
  (is (true? (cut/map-or-seq? '(1 2 3))))
  (is (false? (cut/map-or-seq? "1,2,3"))))

(deftest test-pvc-storage-class-name?
  (is (true? (cut/pvc-storage-class-name? :manual)))
  (is (true? (cut/pvc-storage-class-name? :local-path)))
  (is (false? (cut/pvc-storage-class-name? :none))))

(deftest test-port-number?
  (is (true? (cut/port-number? 1)))
  (is (true? (cut/port-number? 65535)))
  (is (false? (cut/port-number? 0)))
  (is (false? (cut/port-number? 65536)))
  (is (false? (cut/port-number? "12345"))))

(deftest test-host-and-port-string?
  (is (true? (cut/host-and-port-string? "test.de:1234")))
  (is (false? (cut/host-and-port-string? "test.de,1234")))
  (is (false? (cut/host-and-port-string? "test.123:1234")))
  (is (false? (cut/host-and-port-string? "test.de:abc"))))

(deftest test-string-sequence?
  (is (true? (cut/string-sequence? ["hallo" "welt" "!"])))
  (is (false? (cut/string-sequence? ["hallo" 1 "welt" "!"])))
  (is (false? (cut/string-sequence? "hallo welt!"))))