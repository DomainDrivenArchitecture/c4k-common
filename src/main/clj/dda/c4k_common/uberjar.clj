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

  -v | --version : Shows project version
  -h             : Shows help 

   " name " {your configuraton file} {your authorization file}"))

(s/def ::options (s/* #{"-h" 
                        "-v" "--version"}))
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
  (main-common "c4k-common"
               core/config?
               core/auth?
               core/config-defaults
               core/k8s-objects
               cmd-args))