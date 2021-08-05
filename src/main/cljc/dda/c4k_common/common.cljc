(ns dda.c4k-common.common
  (:require
   [clojure.walk]))

;; deprecated functions were moved to dda.c4k-common.prefixes
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

(defn replace-named-value
  [coll name value]
  (clojure.walk/postwalk #(if (and (map? %)
                                   (= name (:name %)))
                            {:name name :value value}
                            %) 
                         coll))

(defn replace-key-value
  [coll key value]
  (clojure.walk/postwalk #(if (and (map? %)
                                   (contains? % key))
                            (assoc % key value)
                            %)
                         coll))

(defn replace-all-matching-values-by-new-value
  [coll value-to-match value-to-replace]
  (clojure.walk/postwalk #(if (and (= (type value-to-match) (type %))
                                   (= value-to-match %))
                            value-to-replace
                            %) 
                         coll))
