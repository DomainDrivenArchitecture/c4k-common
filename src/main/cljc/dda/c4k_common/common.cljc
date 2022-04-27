(ns dda.c4k-common.common
  (:require
   [clojure.walk]
   [clojure.spec.alpha :as s]
   [clojure.string :as cs]
   [clojure.tools.reader.edn :as edn]
   #?(:clj [orchestra.core :refer [defn-spec]]
      :cljs [orchestra.core :refer-macros [defn-spec]])
   [dda.c4k-common.predicate :as cp]
   [expound.alpha :as expound]))


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
   value string?]
  (clojure.walk/postwalk #(if (and (map? %)
                                   (= name (:name %)))
                            {:name name :value value}
                            %) 
                         coll))

(defn-spec replace-key-value cp/map-or-seq?
  [coll cp/map-or-seq?
   key keyword?
   value string?]
  (clojure.walk/postwalk #(if (and (map? %)
                                   (contains? % key))
                            (assoc % key value)
                            %)
                         coll))

(defn-spec replace-all-matching-values-by-new-value cp/map-or-seq?
  [coll string?
   value-to-match string?
   value-to-replace string?]
  (clojure.walk/postwalk #(if (and (= (type value-to-match) (type %))
                                   (= value-to-match %))
                            value-to-replace
                            %) 
                         coll))

(defn-spec concat-vec vector?
  [& vs (s/* seq?)]
  (into []
        (apply concat vs)))

(defn usage [name]
  (str 
   "usage:
        
   " name "{your configuraton file} {your authorization file}"))

(s/def ::options (s/* #{"-h"}))
(s/def ::filename (s/and string?
                         #(not (cs/starts-with? % "-"))))
(s/def ::cmd-args (s/cat :options ::options
                         :args (s/?
                                (s/cat :config ::filename
                                       :auth ::filename))))

(defn invalid-args-msg
  [name spec args]
  (s/explain spec args)
  (println (str "Bad commandline arguments\n" (usage name))))

(defn generate-common [my-config my-auth config-defaults k8s-objects]
  (let [resulting-config (merge config-defaults my-config my-auth)]
    (cs/join
     "\n---\n"
     (k8s-objects resulting-config))))

(defn main-common [name config-spec? auth-spec? config-defaults k8s-objects cmd-args]
  (let [parsed-args-cmd (s/conform ::cmd-args cmd-args)]
    (if (= ::s/invalid parsed-args-cmd)
      (invalid-args-msg name ::cmd-args cmd-args)
      (let [{:keys [options args]} parsed-args-cmd
            {:keys [config auth]} args]
        (cond
          (some #(= "-h" %) options)
          (println usage)
          :default
          (let [config-str (slurp config)
                auth-str (slurp auth)
                config-edn (edn/read-string config-str)
                auth-edn (edn/read-string auth-str)
                config-valid? (s/valid? config-spec? config-edn)
                auth-valid? (s/valid? auth-spec? auth-edn)]
            (if (and config-valid? auth-valid?)
              (println (generate-common config-edn auth-edn config-defaults k8s-objects))
              (do
                (when (not config-valid?)
                  (println
                   (expound/expound-str config-spec? config-edn {:print-specs? false})))
                (when (not auth-valid?)
                  (println
                   (expound/expound-str auth-spec? auth-edn {:print-specs? false})))))))))))
