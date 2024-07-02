(ns dda.c4k-common.common
  (:require
   [clojure.walk]
   [clojure.spec.alpha :as s]
   [clojure.string :as cs]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.predicate :as cp]))

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

(defn-spec replace-all-matching cp/map-or-seq?
  [coll cp/map-or-seq?
   match-value string?
   replace-value cp/str-or-number?]
   (clojure.walk/postwalk #(if (and (= (type match-value) (type %))
                                   (= match-value %))
                            replace-value
                            %)
                         coll))


(defn-spec ^{:deprecated "6.2.4"} replace-all-matching-values-by-new-value cp/map-or-seq?
  "Use replace-all-matching instead"
  [coll cp/map-or-seq?
   value-to-match string?
   value-to-replace cp/str-or-number?]
  (replace-all-matching coll value-to-match value-to-replace))

(defn-spec concat-vec vector?
  [& vs (s/* cp/string-sequence?)]
  (into []
        (apply concat vs)))

(defn generate-cm
  [my-config
   my-auth
   config-defaults
   config-objects
   auth-objects
   only-config
   only-auth]
  (let [resulting-config (merge config-defaults my-config)
        both (or (and only-config only-auth) (and (not only-config) (not only-auth)))
        res-vec (cond
                  both (concat-vec (config-objects resulting-config) (auth-objects my-auth))
                  only-config (config-objects my-config)
                  only-auth (auth-objects my-auth))]
    (cs/join
     "\n---\n"
     res-vec)))

(defn generate-common 
  [my-config 
   my-auth 
   config-defaults 
   k8s-objects]
  (let [resulting-config (merge config-defaults my-config)]
    (cs/join
     "\n---\n"
     (k8s-objects resulting-config my-auth))))
