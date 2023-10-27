(ns dda.c4k-common.browser
  (:require
   [clojure.string :as st]
   [clojure.spec.alpha :as s]
   [expound.alpha :as expound]
   [orchestra.core :refer-macros [defn-spec]]
   [hickory.render :as hr]))

(def js-object? any?)

(defn-spec print-debug string?
  [sth string?]
  (print "debug " sth)
  sth)

(defn-spec get-element-by-id js-object?
  [name string?]
  (-> js/document
      (.getElementById name)))

(s/def ::deserializer fn?)
(s/def ::optional boolean?)
(def dom-function-parameter (s/keys :opt-un [::deserializer ::optional]))
(defn-spec get-content-from-element js-object?
  [name string?]
  (-> (get-element-by-id name)
      (.-value)))

(defn-spec deserialize-content js-object?
  [content string?
   deserializer ::deserializer
   optional ::optional]
  (cond
    (and optional (st/blank? content))
    nil
    :else
    (apply deserializer [content])))

(defn-spec get-deserialized-content js-object?
  [name string?
   & {:keys [deserializer optional]
      :or {deserializer identity optional false}} dom-function-parameter]
  (-> (get-content-from-element name)
      (deserialize-content deserializer optional)))

(defn-spec set-validation-result! js-object?
  [name string?
   validation-result js-object?]
  (-> (get-element-by-id (str name "-validation"))
      (.-innerHTML)
      (set! validation-result))
  (-> (get-element-by-id name)
      (.setCustomValidity validation-result))
  validation-result)

(defn-spec validate! js-object?
  [name string?
   spec js-object?
   & {:keys [deserializer optional]
      :or {deserializer identity optional false}} dom-function-parameter]
  (let [content (get-deserialized-content name :optional optional :deserializer deserializer)]
    (if (or (and optional (st/blank? content)) 
            (s/valid? spec content))
      (set-validation-result! name "")
      (set-validation-result! name
       (expound/expound-str spec content {:print-specs? false})))))

(defn-spec set-output! js-object?
  [input string?]
  (-> js/document
      (.getElementById "output")
      (.-value)
      (set! input)))

(defn-spec set-form-validated! js-object? 
  []
  (-> (get-element-by-id "form")
      (.-classList)
      (.add "was-validated")))
(defn-spec ^{:deprecated "0.4"} set-validated! js-object?
  []
  (set-form-validated!))

(defn-spec create-js-obj-from-html js-object?
  [html-string string?]
  (-> js/document
      .createRange
      (.createContextualFragment html-string)))

(defn-spec append-to-c4k-content js-object?
  [js-obj js-object?]
  (-> (get-element-by-id "c4k-content")
      (.appendChild js-obj)))

(defn-spec append-hickory js-object?
  [hickory-obj map?]
  (-> hickory-obj
      (hr/hickory-to-html)
      (create-js-obj-from-html)
      (append-to-c4k-content)))

(defn-spec generate-feedback-tag map?
  [id string?]
  {:type :element 
   :attrs {:class "invalid-feedback"} 
   :tag :div 
   :content [{:type :element 
              :attrs {:id (str id "-validation")} 
              :tag :pre 
              :content nil}]})

(defn-spec generate-label map?
  [id-for string?
   label string?]
  {:type :element
   :attrs {:for id-for :class "form-label"} 
   :tag :label 
   :content [label]})

(defn-spec generate-br map?
  []
  {:type :element, :attrs nil, :tag :br, :content nil})

(defn-spec generate-input-field map?
  [id string?
   label string?
   default-value string?]
  [(generate-label id label)
   {:type :element 
    :attrs {:class "form-control" :type "text" :name id :id id :value default-value} 
    :tag :input 
    :content nil}
   (generate-feedback-tag id)
   (generate-br)])

(defn-spec generate-text-area map?
  [id string?
   label string?
   default-value string?
   rows pos-int?]
  [(generate-label id label)
   {:type :element 
    :attrs {:name id :id id :class "form-control" :rows rows} 
    :tag :textarea 
    :content [default-value]}
   (generate-feedback-tag id)
   (generate-br)])

(defn-spec generate-button map?
  [id string?
   label string?]
  [{:type :element
    :attrs {:type "button", :id id, :class "btn btn-primary"}
    :tag :button
    :content [label]}
   (generate-br)])

(defn-spec generate-output vector?
  [id string?
   label string?
   rows pos-int?]
  [{:type :element, 
    :attrs {:id id}, 
    :tag :div, 
    :content [{:type :element
               :attrs {:for "output", :class "form-label"}
               :tag :label, :content [label]}
              {:type :element, :attrs {:name "output", 
                                       :id "output", 
                                       :class "form-control", 
                                       :rows rows}, 
               :tag :textarea, :content []}]}
   (generate-br)])

(defn-spec generate-needs-validation map?
  []
  {:type :element, 
   :attrs {:class "needs-validation", :id "form"}, 
   :tag :form, 
   :content []})

(defn-spec generate-group map?
  [name string?
   content any?]
  [{:type :element
    :tag :div
    :attrs {:class "rounded border border-3  m-3 p-2"}
    :content [{:type :element
               :tag :b
               :attrs {:style "z-index: 1; position: relative; top: -1.3rem;"}
               :content name}
              {:type :element
               :tag :fieldset
               :content content}]}])
