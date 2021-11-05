(ns dda.c4k-common.base64
  (:require
   ["js-base64" :as b64]
   [orchestra.core :refer-macros [defn-spec]]))


(defn-spec encode string?
  [input string?]
  (.encode b64/Base64 input))

(defn-spec decode string?
  [input string?]
  (.decode b64/Base64 input))
