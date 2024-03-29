(ns dda.c4k-common.macros
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import java.util.jar.JarFile))

(defn inline-resource-file [resource-url relative-resource-folder-path]
  (let [files (.listFiles (io/file resource-url))
        file-contents (map slurp files)
        file-names (map #(str relative-resource-folder-path "/" (.getName %)) files)]
    (zipmap file-names file-contents)))

(defn inline-resource-jar [resource-url]
  (let [resource-url-string (.toString resource-url)
        ; Remove jar:file:
        start-absolute (str/replace-first resource-url-string "jar:file:" "")
        ; Split path into jar base and search folder
        jar-split (str/split start-absolute #"!/")
        absolute-jar-path (first jar-split)
        relative-file-path (second jar-split)
        jar          (JarFile. absolute-jar-path)
        files        (->> (enumeration-seq (.entries jar))
                          (filter #(str/starts-with? % relative-file-path))
                          (filter #(not (.isDirectory %))))
        file-names (map #(.getName %) files)
        file-contents (map #(slurp (.getInputStream jar %)) files)]
    (zipmap file-names file-contents)))

(defmacro inline-resources [resource-path]
  (let [resource-url (io/resource resource-path)
        resource-protocol (.getProtocol resource-url)]
    (case resource-protocol
      "file" (inline-resource-file resource-url resource-path)
      "jar" (inline-resource-jar resource-url))))

