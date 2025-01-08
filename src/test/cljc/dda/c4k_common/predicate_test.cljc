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

(deftest test-ipv4-string?
  (is (true? (cut/ipv4-string? "127.0.0.1")))
  (is (true? (cut/ipv4-string? "192.168.192.168")))
  (is (true? (cut/ipv4-string? "1.2.3.4")))
  (is (false? (cut/ipv4-string? "1.a.2.b")))
  (is (false? (cut/ipv4-string? "f::f::f::f"))))

(deftest test-ipv6-string?
  ;(is (true? (cut/ipv6-string? "2a01:4f8:c012:cb41::1")))
  (is (false? (cut/ipv6-string? "1.a.2.b")))
  (is (false? (cut/ipv6-string? "f::f::f::f"))))

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
  (is (true? (cut/pvc-storage-class-name? "manual")))
  (is (true? (cut/pvc-storage-class-name? "local-path")))
  (is (false? (cut/pvc-storage-class-name? "none"))))

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

(deftest test-integer-string?
  (is (true? (cut/integer-string? "1")))
  (is (true? (cut/integer-string? "42")))
  (is (false? (cut/integer-string? 42)))
  (is (false? (cut/integer-string? "42.2")))
  (is (false? (cut/integer-string? "4 2")))
  (is (false? (cut/integer-string? "1e2")))
  (is (false? (cut/integer-string? true))))

(deftest test-string-sequence?
  (is (true? (cut/string-sequence? ["hallo" "welt" "!"])))
  (is (false? (cut/string-sequence? ["hallo" 1 "welt" "!"])))
  (is (false? (cut/string-sequence? "hallo welt!"))))

(deftest test-int-gt-n?
  (is (not (cut/int-gt-n? 5 0)))
  (is (not (cut/int-gt-n? 5 "s")))
  (is (not (cut/int-gt-n? 0 0)))
  (is (cut/int-gt-n? 5 6))
  (is ((partial cut/int-gt-n? 5) 10))
  (is (not ((partial cut/int-gt-n? 5) 4))))

(deftest test-str-or-number?
  (is (cut/str-or-number? "string"))
  (is (cut/str-or-number? 42))
  (is (not (cut/str-or-number? []))))
