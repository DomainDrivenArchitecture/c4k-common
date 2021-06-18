(ns dda.c4k-common.browser
  (:require
   [clojure.string :as st]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [orchestra.core :refer-macros [defn-spec]]))

(defn-spec print-debug string?
  [sth string?]
  (print "debug " sth)
  sth)

(defn-spec get-element-by-id any?
  [name string?]
  (-> js/document
      (.getElementById name)))

(s/def ::deserializer fn?)
(s/def ::optional boolean?)
(defn-spec get-content-from-element any?
  [name string?
   & {:keys [deserializer optional]
      :or {deserializer nil optional false}} (s/keys :opt-un [::deserializer ::optional])]
  (let [content (-> (get-element-by-id name)
                    (.-value))]
    (cond
      (and optional (some? deserializer))
      (when-not (st/blank? content)
        (apply deserializer [content]))
      (and (false? optional) (some? deserializer))
      (apply deserializer [content])
      :else
      content)))

(defn-spec set-validation-result! any?
  [name string?
   validation-result any?]
  (-> (get-element-by-id (str name "-validation"))
      (.-innerHTML)
      (set! validation-result))
  (-> (get-element-by-id name)
      (.setCustomValidity validation-result))
  validation-result)

(defn-spec validate! any?
  [name string?
   spec any?
   & {:keys [deserializer optional]
      :or {deserializer nil optional false}} (s/keys :opt-un [::deserializer ::optional])]
  (let [content (get-content-from-element name :optional optional :deserializer deserializer)]
    (if (or (and optional (st/blank? content)) 
            (s/valid? spec content))
      (set-validation-result! name "")
      (set-validation-result! name
       (expound/expound-str spec content {:print-specs? false})))))

(defn set-output!
  [input]
  (-> js/document
      (.getElementById "output")
      (.-value)
      (set! input)))

(defn set-validated! []
  (-> (get-element-by-id "form")
      (.-classList)
      (.add "was-validated")))