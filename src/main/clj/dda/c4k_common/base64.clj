(ns dda.c4k-common.base64
  (:import (java.util Base64))
  (:require
   [orchestra.core :refer [defn-spec]]
   [orchestra.spec.test :as st]
   [clojure.spec.alpha :as s]))


(defn-spec encode string?
  [string string?]
  (.encodeToString 
   (Base64/getEncoder) 
   (.getBytes string "UTF-8"))) 

(defn-spec decode string?
  [string string?]
  (String. 
   (.decode (Base64/getDecoder) string) 
   "UTF-8"))

(st/instrument)