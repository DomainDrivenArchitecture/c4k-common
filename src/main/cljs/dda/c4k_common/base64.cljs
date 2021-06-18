(ns dda.c4k_common.base64
  (:require
   ["js-base64" :as b64]))

(defn encode
  [string]
  (.encode b64/Base64 string))

(defn decode
  [string]
  (.decode b64/Base64 string))
