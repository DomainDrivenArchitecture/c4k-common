(ns dda.c4k-common.browser-test
  (:require
   [cljs.test :refer-macros [deftest is are testing run-tests]]
   [clojure.spec.test.alpha :as st]
   [dda.c4k-common.browser :as cut]))

(st/instrument `cut/print-debug)
(st/instrument `cut/get-element-by-id)
(st/instrument `cut/get-content-from-element)
(st/instrument `cut/deserialize-content)
(st/instrument `cut/get-deserialized-content)
(st/instrument `cut/set-validation-result!)
(st/instrument `cut/validate!)
(st/instrument `cut/set-output!)
(st/instrument `cut/set-form-validated!)
(st/instrument `cut/create-js-obj-from-html)
(st/instrument `cut/append-to-c4k-content)
(st/instrument `cut/append-hickory)
(st/instrument `cut/generate-feedback-tag)
(st/instrument `cut/generate-label)
(st/instrument `cut/generate-br)
(st/instrument `cut/generate-input-field)
(st/instrument `cut/generate-text-area)
(st/instrument `cut/generate-button)
(st/instrument `cut/generate-output)
(st/instrument `cut/generate-needs-validation)
(st/instrument `cut/generate-group)

(deftest should-deserialize-content
  (is (= (cut/deserialize-content " " identity true) nil))
  (is (= (cut/deserialize-content "test" keyword false) :test))
  (is (= (cut/deserialize-content "test" identity false) "test"))
  (is (= (cut/deserialize-content "test" identity true) "test")))

