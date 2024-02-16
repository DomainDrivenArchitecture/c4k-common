(ns dda.c4k-common.base64
  (:import (java.util Base64))
  (:require
   [orchestra.core :refer [defn-spec]]))


(defn-spec encode string?
  [input string?]
  (.encodeToString 
   (Base64/getEncoder) 
   (.getBytes ^String input "UTF-8"))) 

(defn-spec decode string?
  [input string?]
  (String. 
   (.decode (Base64/getDecoder) ^String input) 
   "UTF-8"))
