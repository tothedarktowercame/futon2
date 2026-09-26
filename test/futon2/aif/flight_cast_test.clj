(ns futon2.aif.flight-cast-test
  "WM-CAST-I 1: the flight names the tick's cast in the click. The seventh
  flight (flight-278b6988) selected and closed :agent-unavailable because
  http-click-fn sent no cast; the endpoint (futon3c handle-wm-click-start)
  already reads :author/:reviewer/:repair-reviewer, nonblank strings only.
  Live pins: that flight's id, target and wants, and its tick run record
  (fixture headers: paths and shas)."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-driver :as fd]
            [futon2.aif.flight-runner :as fr]))

(def seventh (edn/read-string (slurp "test/fixtures/flight-cast/seventh-flight.edn")))
(def seventh-run (edn/read-string (slurp "test/fixtures/flight-cast/seventh-run-record.edn")))

(defn- click [cast-opts]
  (let [posted (atom nil)
        cf (fr/http-click-fn (merge {:today (constantly "2026-09-26")
                                     :post! (fn [b] (reset! posted b) {:status 200 :body {:click-id "wm-click-7"}})
                                     :get-status! (constantly {:running? false})
                                     :sleep! (fn [_])
                                     :read-record! (constantly seventh-run)}
                                    cast-opts))
        f (flight/run! (flight/start {:target (:target seventh) :chosen-because {:kind :requested}}
                                     {:kind :a-exits :repo "futon3c" :path "p" :read-text (fn [& _] "")}
                                     {:id (:flight/id seventh)})
                       {:click-fn cf :observe-fn (fn [_ _] {}) :sources-fn (constantly {}) :max-clicks 1})]
    {:body @posted :entry (first (:clicks f))}))

(deftest the-cast-goes-on-the-body-and-the-click-entry
  (let [{:keys [body entry]} (click {:author "a" :reviewer "b"})]
    (is (not-any? #{:cast} (:click-entry-keys seventh)) "the seventh flight's entry carried no cast")
    (is (= "a" (:author body)))
    (is (= "b" (:reviewer body)))
    (is (not (contains? body :repair-reviewer)))
    (is (= "2026-09-26-flight-278b6988-click-1" (:run-id body)))
    (is (= {:author "a" :reviewer "b" :repair-reviewer {:absent :no-repair-reviewer-given}}
           (:cast entry)))))

(deftest no-cast-given-is-typed-and-sends-no-keys
  (let [{:keys [body entry]} (click {})]
    (is (= #{:flight-edn :run-id :issuing-caller :trigger} (set (keys body))))
    (is (= {:author {:absent :no-author-given} :reviewer {:absent :no-reviewer-given}
            :repair-reviewer {:absent :no-repair-reviewer-given}}
           (:cast entry)))))

(deftest a-blank-seat-is-absent
  ;; the endpoint's own rule (nonblank-string?)
  (let [{:keys [body entry]} (click {:author "  " :reviewer "" :repair-reviewer "c"})]
    (is (not (contains? body :author)))
    (is (not (contains? body :reviewer)))
    (is (= "c" (:repair-reviewer body)))
    (is (= {:absent :no-author-given} (get-in entry [:cast :author])))
    (is (= {:absent :no-reviewer-given} (get-in entry [:cast :reviewer])))))

(deftest a-click-not-started-still-records-its-cast
  (let [cf (fr/http-click-fn {:today (constantly "2026-09-26") :author "a"
                              :post! (constantly {:status 409 :body {:error "wm-click-cast-not-invoke-ready"}})
                              :get-status! (fn [] (throw (ex-info "should not poll" {})))})]
    (is (= "a" (get-in (cf {:flight {:flight/id "f" :target "M" :click 1}}) [:cast :author])))))

(deftest the-driver-flags-reach-the-plan
  (is (= {:author "a" :reviewer "b" :repair-reviewer {:absent :no-repair-reviewer-given}}
         (:cast (fd/resolved-steps (fd/parse-args ["M" "--author" "a" "--reviewer" "b"])))))
  (is (= {:author {:absent :no-author-given} :reviewer {:absent :no-reviewer-given}
          :repair-reviewer {:absent :no-repair-reviewer-given}}
         (:cast (fd/resolved-steps {})))))
