(ns dda.c4k-common.base64
  (:import (java.util Base64)))

(defn encode
  [string]
  (.encodeToString 
   (Base64/getEncoder) 
   (.getBytes string "UTF-8"))) 

(defn decode
  [string]
  (String. 
   (.decode (Base64/getDecoder) string) 
   "UTF-8"))