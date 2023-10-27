(ns dda.c4k-common.macros
  (:require [clojure.java.io :as io]))

(defmacro inline-resources [resource-path]
  (let [files (.listFiles (io/file (io/resource resource-path)))
        file-contents (map slurp files)
        file-names (map #(str resource-path "/" (.getName %)) files)]
       (zipmap file-names file-contents)))
