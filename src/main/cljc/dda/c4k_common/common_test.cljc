(ns dda.c4k-common.common-test)

; Heavily assumes c1 and c2 have an identical structure but maybe different values
; This function finds the diff of two nested maps/vectors
(defn map-diff
  ([c1 c2]
   (into {}
         (cond (or (map? c1) (vector? c1))
               (map
                #(if (= (% c1) (% c2))
                   nil
                   (let [c1-value (% c1)
                         c2-value (% c2)
                         key-name (name %)]
                     (cond
                       (map? c1-value) (map-diff c1-value c2-value)
                       (vector? c1-value) (map-diff c1-value c2-value key-name)
                       :else {(keyword (str key-name "-c1")) c1-value
                              (keyword (str key-name "-c2")) c2-value})))
                (keys c1)))))
  ([c1 c2 last-name]
   (first (for [x c1 y c2] (cond
                             (map? x) (map-diff x y)
                             (vector? x) (map-diff x y last-name)
                             :else {(keyword (str last-name "-c1")) x
                                    (keyword (str last-name "-c2")) y})))))