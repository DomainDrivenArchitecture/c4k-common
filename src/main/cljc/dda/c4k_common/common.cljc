(ns dda.c4k-common.common
  (:require
   [clojure.walk]
   [clojure.spec.alpha :as s]
   [clojure.string :as cs]
   [orchestra.core :refer [defn-spec]]
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

(defn-spec replace-map-value map?
  [m map?
   keys (s/+ any?)
   searchstring string?
   replacestring string?]
  (let [value (get-in m keys)]
    (cond
      (nil? value) m
      (coll? value) (assoc-in m keys
                              (map (fn [val] (cs/replace val (re-pattern searchstring) replacestring)) value))
      :else (assoc-in m keys
                      (cs/replace value (re-pattern searchstring) replacestring)))))

(defn-spec ^{:deprecated "6.2.4"} replace-all-matching-values-by-new-value cp/map-or-seq?
  "Use replace-all-matching instead"
  [coll cp/map-or-seq?
   value-to-match string?
   value-to-replace cp/str-or-number?]
  (replace-all-matching coll value-to-match value-to-replace))


(defn-spec concat-vec vector?
  [& vs (s/* (s/or ::maps (s/* cp/map-or-nil?)
                   ::strings (s/* cp/string-or-nil?)
                   ::sequences (s/* cp/seq-or-nil?)
                   ::vectors (s/* cp/vec-or-nil?)))]
  (filter #(not (nil? %))
          (into []
                (apply concat vs))))

(defn generate
  [my-config
   my-auth
   config-defaults
   config-objects
   auth-objects
   config-select]
  (let [resulting-config (merge config-defaults my-config)
        res-vec (concat-vec
                 (config-objects config-select resulting-config)
                 (auth-objects config-select resulting-config my-auth))]
    (cs/join
     "\n---\n"
     res-vec)))

(defn ^{:deprecated "10.0.0"} generate-cm
  "use generate instead"
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
                  both (concat-vec (config-objects resulting-config) (auth-objects resulting-config my-auth))
                  only-config (config-objects resulting-config)
                  only-auth (auth-objects resulting-config my-auth))]
    (cs/join
     "\n---\n"
     res-vec)))

(defn ^{:deprecated "10.0.0"} generate-common 
  "use generate instead"
  [my-config 
   my-auth 
   config-defaults 
   k8s-objects]
  (let [resulting-config (merge config-defaults my-config)]
    (cs/join
     "\n---\n"
     (k8s-objects resulting-config my-auth))))
