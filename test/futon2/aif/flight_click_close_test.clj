(ns futon2.aif.flight-click-close-test
  "WM-CAST-I 2, defect (iv): the flight's click entry keeps the click's
  selection (:chosen, from record-summary) and the tick's close kind
  (:outcome, the :via into FULL_LOOP_CLOSE). The seventh flight
  (flight-278b6988) selected :C1 and closed :agent-unavailable, and its
  click entry carried neither, so on its own record it read as a flight
  that never selected. Live pins: the seventh and fifth flights' run
  records (fixture headers: paths and shas)."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]))

(def seventh (edn/read-string (slurp "test/fixtures/flight-cast/seventh-flight.edn")))
(def seventh-run (edn/read-string (slurp "test/fixtures/flight-cast/seventh-run-record.edn")))
(def fifth-run (edn/read-string (slurp "test/fixtures/flight-cast/fifth-run-record.edn")))

(defn- entry
  "The click entry record-click writes over RUN-RECORD, through
  record-summary, as http-click-fn hands it on."
  [target run-record]
  (let [f (flight/start {:target target :chosen-because {:kind :requested}}
                        {:kind :a-exits :repo "futon3c" :path "p" :read-text (fn [& _] "")}
                        {:id (:flight/id seventh)})
        summary (fr/record-summary target "click-1" run-record)]
    (first (:clicks (flight/record-click f (merge summary {:wants (:wants seventh) :before {} :after {}}))))))

(deftest the-seventh-flights-selection-and-close-are-kept
  (let [e (entry (:target seventh) seventh-run)]
    (is (not-any? #{:chosen :outcome} (:click-entry-keys seventh)) "what the seventh flight's entry carried")
    (is (= :C1 (get-in e [:chosen :candidate])))
    (is (= 8 (count (get-in e [:chosen :precedence]))))
    (is (= (get-in seventh-run [:decision :chosen :precedence]) (get-in e [:chosen :precedence])))
    (is (= :agent-unavailable (:outcome e)))))

(deftest the-fifth-flights-untyped-close
  (let [e (entry "M-autoclock-in" fifth-run)]
    (is (= :incomplete (:outcome e)))
    (is (not (contains? e :chosen)) "it never selected")))

(deftest a-run-record-with-no-route-is-typed-absent
  (is (= {:absent :no-terminal-outcome-on-run-record}
         (:outcome (entry "M-autoclock-in" (dissoc seventh-run :route)))))
  (is (= {:absent :no-terminal-outcome-on-run-record}
         (:outcome (fr/record-summary "M-autoclock-in" "click-1" nil)))))

(deftest another-targets-selection-is-not-kept
  ;; the bad case: the guard in record-summary (:chosen only when its
  ;; :target is the flight's)
  (let [e (entry "M-some-other-target" seventh-run)]
    (is (not (contains? e :chosen)))
    (is (= :agent-unavailable (:outcome e)) "the close kind is the tick's, whatever it chose")))
