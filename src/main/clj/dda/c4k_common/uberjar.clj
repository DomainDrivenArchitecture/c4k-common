(ns dda.c4k-common.uberjar
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as cs]
   [clojure.tools.reader.edn :as edn]
   [dda.c4k-common.common :as cm]
   [expound.alpha :as expound]))

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

(defn main-common [name config-spec? auth-spec? config-defaults k8s-objects cmd-args]
  (let [parsed-args-cmd (s/conform ::cmd-args cmd-args)]
    (if (= ::s/invalid parsed-args-cmd)
      (invalid-args-msg name ::cmd-args cmd-args)
      (let [{:keys [options args]} parsed-args-cmd
            {:keys [config auth]} args]
        (cond
          (some #(= "-h" %) options)
          (println usage)
          :else
          (let [config-str (slurp config)
                auth-str (slurp auth)
                config-edn (edn/read-string config-str)
                auth-edn (edn/read-string auth-str)
                config-valid? (s/valid? config-spec? config-edn)
                auth-valid? (s/valid? auth-spec? auth-edn)]
            (if (and config-valid? auth-valid?)
              (println (cm/generate-common config-edn auth-edn config-defaults k8s-objects))
              (do
                (when (not config-valid?)
                  (println
                   (expound/expound-str config-spec? config-edn {:print-specs? false})))
                (when (not auth-valid?)
                  (println
                   (expound/expound-str auth-spec? auth-edn {:print-specs? false})))))))))))