(ns futon2.aif.job-text-dispatch-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.d-predecessor-task-authority :as d-task]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.job-text-retention :as retention]))

(deftest production-dispatch-exposes-the-exact-bound-prompt
  (let [sent (atom nil) captured {:status :captured :dispatch {:task :test}}
        response
        (with-redefs-fn {#'runner/post-json! (fn [_ body]
                                             (reset! sent body)
                                             {:job-id "job"})}
          #(runner/dispatch! {:agency-base "http://unused"
                              :d-task-dispatch-state (atom captured)}
                             "author" "caller" "mission" "raw prompt"))
        expected (str (d-task/prompt-binding (:dispatch captured)) "\nraw prompt")]
    (is (= expected (:prompt @sent)))
    (is (= expected (::retention/dispatched-prompt (meta response))))
    (is (= {:job-id "job"} response))))
