(ns dda.c4k-common.common
  (:require
   [clojure.walk]
   [clojure.spec.alpha :as s]
   [clojure.string :as cs]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.predicate :as cp]))

;; deprecated functions were moved to dda.c4k-common.predicate
(defn ^{:deprecated "0.1"} bash-env-string?
  [input]
  (and (string? input)
       (not (re-matches #".*['\"\$]+.*" input))))

(defn ^{:deprecated "0.1"} fqdn-string?
  [input]
  (and (string? input)
       (some? (re-matches #"(?=^.{4,253}$)(^((?!-)[a-zA-Z0-9-]{0,62}[a-zA-Z0-9]\.)+[a-zA-Z]{2,63}$)" input))))

(defn ^{:deprecated "0.1"} letsencrypt-issuer?
  [input]
  (contains? #{:prod :staging} input))

(defn-spec replace-named-value cp/map-or-seq?
  [coll cp/map-or-seq?
   name string?
   value cp/str-or-number?]
  (clojure.walk/postwalk #(if (and (map? %)
                                   (= name (:name %)))
                            {:name name :value value}
                            %) 
                         coll))

(defn-spec replace-key-value cp/map-or-seq?
  [coll cp/map-or-seq?
   key keyword?
   value cp/str-or-number?]
  (clojure.walk/postwalk #(if (and (map? %)
                                   (contains? % key))
                            (assoc % key value)
                            %)
                         coll))

(defn-spec replace-all-matching-values-by-new-value cp/map-or-seq?
  [coll cp/map-or-seq?
   value-to-match string?
   value-to-replace cp/str-or-number?]
  (clojure.walk/postwalk #(if (and (= (type value-to-match) (type %))
                                   (= value-to-match %))
                            value-to-replace
                            %) 
                         coll))

(defn-spec concat-vec vector?
  [& vs (s/* cp/string-sequence?)]
  (into []
        (apply concat vs)))

(defn generate-common 
  [my-config 
   my-auth 
   config-defaults k8s-objects]
  (let [resulting-config (merge config-defaults my-config)]
    (cs/join
     "\n---\n"
     (k8s-objects resulting-config my-auth))))
