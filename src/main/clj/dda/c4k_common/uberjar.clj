(ns dda.c4k-common.uberjar
  (:gen-class)
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as cs]
   [clojure.tools.reader.edn :as edn]
   [clojure.java.io :as io]
   [dda.c4k-common.yaml :as yaml]
   [dda.c4k-common.common :as cm]
   [dda.c4k-common.core :as core]
   [expound.alpha :as expound]))

(defn usage [name]
  (str
   "usage:
  
  options
  -v | --version : Shows project version
  -h | --help    : Shows help 
  -c | --config  : Only generate the config
  -a | --auth    : Only generate the auth
    
  parameters
  -cp | --config-part [part-name,..] : Only generate the part names defined (comma separated, no space)

   " name " [options] [parameters] {your configuraton file} {your authorization file}"))

(s/def ::config-names (s/cat :name #{"-cp" "--config-part"} :values string?))
(s/def ::opts-and-params
  (s/alt
   :help (s/? #{"-h" "--help"})
   :version (s/? #{"-v" "--version"})
   :auth-only (s/? #{"-a" "--auth"})
   :config (s/alt :all (s/? #{"-c" "--config"})
                  :names ::config-names)
   ))
(s/def ::filename (s/and string?
                         #(not (cs/starts-with? % "-"))))
(s/def ::cmd-args (s/cat :opts-and-params ::opts-and-params
                         :args (s/?
                                (s/cat :config-file ::filename
                                       :auth-file ::filename))))

(defn invalid-args-msg
  [name spec args]
  (s/explain spec args)
  (println (str "Bad commandline arguments\n" (usage name))))

(defn parse-args
  [args]
  (let [parsed-args (s/conform ::cmd-args args)
        {:keys [opts-and-params args]} parsed-args
        opts-part (cond
                    (= opts-and-params [:help :clojure.spec.alpha/nil])
                    {}
                    (or (= opts-and-params [:help "-h"])
                        (= opts-and-params [:help "--help"]))
                    {:help true}
                    (some #(= :version %) opts-and-params)
                    {:version true}
                    (some #(= :auth-only %) opts-and-params)
                    {:config-select ["auth"]}
                    (some #(= :config %) opts-and-params)
                    {:config-select (if (= :all (get-in opts-and-params [1 0]))
                                      []
                                      (cs/split (:values (get-in opts-and-params [1 1])) #","))}
                    :else
                    {:invalid true})]
    (merge opts-part args)))


(defn main-cm [name config-spec auth-spec config-defaults config-objects auth-objects cmd-args]
  (let [parsed-args (parse-args cmd-args)
        {:keys [config-file auth-file]} parsed-args]
    (cond
      (contains? parsed-args :invalid)
      (invalid-args-msg name ::cmd-args cmd-args)
      (contains? parsed-args :help)
      (println (usage name))
      (contains? parsed-args :version)
      (println (some-> (io/resource "project.clj") slurp edn/read-string (nth 2)))
      :else
      (let [config-str (slurp config-file)
            auth-str (slurp auth-file)
            config-parse-fn (if (yaml/is-yaml? config-file) yaml/from-string edn/read-string)
            auth-parse-fn (if (yaml/is-yaml? auth-file) yaml/from-string edn/read-string)
            config-edn (config-parse-fn config-str)
            auth-edn (auth-parse-fn auth-str)]
        (cond
          (not (s/valid? config-spec config-edn))
          (println
           (expound/expound-str config-spec config-edn {:print-specs? false}))
          (not (s/valid? config-spec config-edn))
          (println
           (expound/expound-str auth-spec auth-edn {:print-specs? false}))
          :else
          (println (cm/generate config-edn auth-edn
                                config-defaults
                                config-objects
                                auth-objects
                                (:config-select parsed-args))))))))

(defn ^{:deprecated "6.3.1"} main-common [name config-spec? auth-spec? config-defaults k8s-objects cmd-args]
  (let [parsed-args-cmd (s/conform ::cmd-args cmd-args)]
    (if (= ::s/invalid parsed-args-cmd)
      (invalid-args-msg name ::cmd-args cmd-args)
      (let [{:keys [options args]} parsed-args-cmd
            {:keys [config auth]} args]
        (cond
          (some #(= "-h" %) options)
          (println (usage name))
          (some #(or (= "-v" %) (= "--version" %)) options)
          (println (some-> (io/resource "project.clj") slurp edn/read-string (nth 2)))
          :else
          (let [config-str (slurp config)
                auth-str (slurp auth)
                config-parse-fn (if (yaml/is-yaml? config) yaml/from-string edn/read-string)
                auth-parse-fn (if (yaml/is-yaml? auth) yaml/from-string edn/read-string)
                config-edn (config-parse-fn config-str)
                auth-edn (auth-parse-fn auth-str)
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

(defn -main [& cmd-args]
  (main-cm
   "c4k-common"
   core/config?
   core/auth?
   core/config-defaults
   core/config-objects
   core/auth-objects
   cmd-args))