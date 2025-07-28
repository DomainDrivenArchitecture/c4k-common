(ns dda.c4k-common.predicate
  (:require
   [clojure.string :as str]
   [clojure.edn :as edn]))

(defn bash-env-string?
  [input]
  (and (string? input)
       (not (re-matches #".*['\"\$]+.*" input))))

(defn fqdn-string?
  [input]
  (and (string? input)
       (some? (re-matches #"(?=^.{4,253}$)(^((?!-)[a-zA-Z0-9-]{0,62}[a-zA-Z0-9]\.)+[a-zA-Z]{2,63}$)" input))))

(defn ipv4-string?
  [input]
  (and (string? input)
       (some? (re-matches #"^((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}$" input))))

(defn ipv6-string?
  [input]
  (and (string? input)
       (some? (re-matches #"(([0-9a-fA-F]{1,4}:){7,7}[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,7}:|([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}|([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}|([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}|([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}|([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}|[0-9a-fA-F]{1,4}:((:[0-9a-fA-F]{1,4}){1,6})|:((:[0-9a-fA-F]{1,4}){1,7}|:)|fe80:(:[0-9a-fA-F]{0,4}){0,4}%[0-9a-zA-Z]{1,}|::(ffff(:0{1,4}){0,1}:){0,1}((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])|([0-9a-fA-F]{1,4}:){1,4}:((25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9])\.){3,3}(25[0-5]|(2[0-4]|1{0,1}[0-9]){0,1}[0-9]))" input))))

(defn string-of-separated-by?
  [spec-function separator input]
  (every? true? (map spec-function (str/split input separator))))

(defn letsencrypt-issuer?
  [input]
  (contains? #{"prod" "staging"} input))

(defn stage?
  [input]
  (contains? #{"prod" "acc" "int" "test" "dev"} input))

(defn map-or-seq?
  [input]
  (or (map? input) 
      (seq? input)))

(defn pvc-storage-class-name?
  [input]
  (contains? #{"manual" "local-path" "hcloud-volumes" "hcloud-volumes-encrypted"} input))

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

(defn integer-string?
  [input]
  (and (string? input)
       (some? (re-matches #"^\d+$" input))
       (integer? (edn/read-string input))))

(defn string-sequence?
  [input]
  (and (sequential? input)
       (every? true?
               (map #(string? %) input))))

(defn int-gt-n?
  [n input]
  (and (int? input)
       (> input n)))

(defn str-or-number? 
  [input]
  (or 
   (string? input) 
   (number? input)))

(defn map-or-nil?
  [input]
   (or
    (map? input)
    (nil? input)))

(defn seq-or-nil?
  [input]
  (or
   (seq? input)
   (nil? input)))

(defn vec-or-nil?
  [input]
  (or
   (vector? input)
   (nil? input)))

(defn string-or-nil?
  [input]
  (or
   (string? input)
   (nil? input)))

