(ns dda.c4k-common.predicate
  (:require
   [clojure.string :as str]
   #?(:clj [clojure.edn :as edn]
      :cljs [cljs.reader :as edn])))

(defn bash-env-string?
  [input]
  (and (string? input)
       (not (re-matches #".*['\"\$]+.*" input))))

(defn fqdn-string?
  [input]
  (and (string? input)
       (some? (re-matches #"(?=^.{4,253}$)(^((?!-)[a-zA-Z0-9-]{0,62}[a-zA-Z0-9]\.)+[a-zA-Z]{2,63}$)" input))))

(defn string-of-separated-by?
  [spec-function separator input]
  (every? true? (map spec-function (str/split input separator))))

(defn letsencrypt-issuer?
  [input]
  (contains? #{"prod" "staging"} input))

(defn map-or-seq?
  [input]
  (or (map? input) (seq? input)))

(defn pvc-storage-class-name?
  [input]
  (contains? #{:manual :local-path} input))

(defn port-number?
  [input]
  (and (integer? input)
       (> input 0)
       (<= input 65535)))

(defn host-and-port-string?
  [input]
  (and (string? input)
       (let [split-string (str/split input #":")]
         (and (= (count split-string) 2)
              (fqdn-string? (first split-string))
              (port-number? (edn/read-string (second split-string)))))))

(defn string-sequence?
  [input]
  (and (sequential? input)
       (every? true?
               (map #(string? %) input))))

(defn int-gt-n?
  [n input]
  (and (int? input)
       (> input n)))

