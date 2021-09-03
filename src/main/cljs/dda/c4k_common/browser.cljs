(ns dda.c4k-common.browser
  (:require
   [clojure.string :as st]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [orchestra.core :refer-macros [defn-spec]]
   [hickory.render :as hr]))

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

(defn replace-element-content
  [name
   content]
  (-> (get-element-by-id name)
      (.-innerHTML)
      (set! (hr/hickory-to-html content))))

(defn generate-feedback-tag
  [id]
  {:type :element :attrs {:class "invalid-feedback"} :tag :div :content [{:type :element :attrs {:id (str id "-validation")} :tag :pre :content nil}]})

(defn generate-label
  [id-for
   label]
  {:type :element :attrs {:for id-for :class "form-label"} :tag :label :content [label]})

(defn generate-input-field
  [id
   label
   default-value]
  (str (generate-label id label)
       {:type :element :attrs {:class "form-control" :type "text" :name id :value default-value} :tag :input :content nil}
       (generate-feedback-tag id)))

(defn generate-text-area
  [id
   label
   default-value
   rows]
  (str (generate-label id label)
       {:type :element :attrs {:name id :id id :class "form-control" :rows rows} :tag :textarea :content [default-value]}
       (generate-feedback-tag id)))

(defn generate-button
  [id
   label]
  {:type :element
    :attrs {:type "button", :id id, :class "btn btn-primary"}
    :tag :button
    :content [label]})

(defn generate-br
  []
  {:type :element, :attrs nil, :tag :br, :content nil})

(defn generate-output
  [id
   label
   rows]
  {:type :element, :attrs {:id id}, :tag :div, :content [{:type :element, :attrs {:for "output", :class "form-label"}, :tag :label, :content [label]}
                                                            {:type :element, :attrs {:name "output", :id "output", :class "form-control", :rows rows}, :tag :textarea, :content []}]})

(defn generate-needs-validation
  []
  {:type :element, :attrs {:class "needs-validation", :id "form"}, :tag :form, :content []})