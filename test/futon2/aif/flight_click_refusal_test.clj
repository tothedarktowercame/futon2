(ns futon2.aif.flight-click-refusal-test
  "WM-SPIKE-FIX-I A: a click the server refuses carries the server's reason
  onto the flight record (the abstention and its :needs entry). The spike's
  click (flight-d00574c8) was refused and the record kept only
  {:kind :click-not-started :missing :click}."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]))

;; the body futon3c's handle-wm-click-start returns for the cast preflight's
;; refusal (runner-service/cast-preflight-refusal, 409; the catch puts :unready
;; under :details and names the error)
(def preflight-409
  {:status 409
   :body {:error "wm-click-cast-not-invoke-ready"
          :message "WM click refused: a cast seat cannot be invoked"
          :details {:unready {:author {:seat "zai-5" :reason "absent"}
                              :reviewer {:seat "codex-7" :reason "absent"}}}}})

(defn- fly [post!]
  (flight/run! (flight/start {:target "M-t" :chosen-because {:kind :requested}}
                             {:kind :a-exits :repo "futon3c" :path "p" :read-text (fn [& _] "")}
                             {:id "flight-refused"})
               {:click-fn (fr/http-click-fn {:today (constantly "2026-09-25") :post! post!
                                             :get-status! (fn [] (throw (ex-info "should not poll" {})))})
                :observe-fn (fn [_ _] {}) :sources-fn (constantly {}) :max-clicks 1}))

(deftest the-cast-preflight-refusal-reaches-the-flight-record
  (let [f (fly (constantly preflight-409))
        abstention (:abstention (first (:clicks f)))
        need (first (filter #(= :click-not-started (:kind %)) (:needs f)))]
    (is (= 409 (:status abstention)))
    (is (= "wm-click-cast-not-invoke-ready" (get-in abstention [:detail :error])))
    (is (= #{"zai-5" "codex-7"} (set (map :seat (vals (get-in abstention [:detail :details :unready]))))))
    (is (= (select-keys abstention [:status :detail]) (select-keys need [:status :detail]))
        "the :needs entry carries the same reason")))

(deftest a-click-never-answered-is-typed
  (let [abstention (:abstention (first (:clicks (fly (fn [_] (throw (java.net.ConnectException. "Connection refused")))))))]
    (is (= {:absent :no-response} (:status abstention)))
    (is (= {:absent :no-response :message "Connection refused"} (:detail abstention)))))
