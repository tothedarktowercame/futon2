(ns futon2.aif.c5-focus-carry-test
  "C5: a sealed Q6 terminal, rather than a new source read, supplies a focus
   when the next click has no covering discovery window."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.focus-receipt :as focus]
            [futon2.aif.previous-run :as previous-run])
  (:import [java.nio.file Files]))

(def no-current-window
  {:windows [{:from "2026-01-01T00:00:00Z"
              :until "2026-01-01T01:00:00Z"
              :valid-through "2026-01-01T01:00:00Z"}]
   :commits []
   :facet-edges []})

(def discovered-focus
  {:status :present
   :as-of "2026-01-01T00:30:00Z"
   :focus "F"
   :focus-status :discovered
   :focus-origin :commit-facets})

(defn- temp-dir []
  (str (Files/createTempDirectory "c5-focus-carry-"
                                  (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- terminal [id at focus]
  (previous-run/terminal {:run-id id :started-at at :focus focus}))

(deftest retained-focus-crosses-the-real-sealed-q6-stream
  (let [dir (temp-dir)
        _ (previous-run/start! dir "one" "2026-01-01T00:00:00Z")
        _ (previous-run/close! dir (terminal "one" "2026-01-01T00:00:00Z" discovered-focus)
                               "run-one.edn")
        predecessor (previous-run/start! dir "two" "2026-01-02T00:00:00Z")
        carried (focus/predecessor-focus predecessor)
        info (focus/decision-focus-info no-current-window "2026-01-02T00:00:00Z" carried)]
    (is (= :present (:status predecessor)))
    (is (= "F" (:focus carried)))
    (is (= "one" (:previous-run/id carried)))
    (is (= :retained (:status info)))
    (is (= "F" (:focus info)))
    (is (= "one" (get-in info [:previous-focus :previous-run/id])))
    (is (= :retained-unfinished-focus (:focus-origin info)))))

(deftest absent-or-unsealed-predecessor-focus-stays-typed-absence
  (testing "a resealed terminal that lacks its focus is not treated as a discovery"
    (let [dir (temp-dir)
          _ (previous-run/start! dir "one" "2026-01-01T00:00:00Z")
          missing (previous-run/seal (dissoc (terminal "one" "2026-01-01T00:00:00Z"
                                                        discovered-focus)
                                              :focus :sha256))
          _ (previous-run/close! dir missing "run-one.edn")
          predecessor (previous-run/start! dir "two" "2026-01-02T00:00:00Z")]
      (is (= {:status :absent :reason :previous-focus-not-retained}
             (focus/predecessor-focus predecessor)))))
  (testing "a broken terminal seal remains typed absence"
    (let [dir (temp-dir)
          _ (previous-run/start! dir "one" "2026-01-01T00:00:00Z")
          forged (assoc-in (terminal "one" "2026-01-01T00:00:00Z" discovered-focus)
                            [:focus :focus] "forged")
          _ (previous-run/close! dir forged "run-one.edn")
          predecessor (previous-run/start! dir "two" "2026-01-02T00:00:00Z")]
      (is (= :previous-terminal-carrier-tampered (:reason predecessor)))
      (is (= {:status :absent :reason :previous-terminal-carrier-tampered}
             (focus/predecessor-focus predecessor)))))
  (testing "cold start retains the predecessor's specific absence"
    (let [predecessor (previous-run/start! (temp-dir) "one" "2026-01-01T00:00:00Z")]
      (is (= {:status :absent :reason :no-earlier-started-run}
             (focus/predecessor-focus predecessor))))))

(deftest absent-predecessor-keeps-the-in-click-retention
  ;; Every terminal sealed before C5 lacks :focus, so the first carried click
  ;; sees an absent predecessor; the earlier window's focus is still retained.
  (let [ended-window {:windows [{:from "2026-01-01T00:00:00Z"
                                 :until "2026-01-01T01:00:00Z"
                                 :valid-through "2026-01-01T01:00:00Z"
                                 :commits [{:at "2026-01-01T00:10:00Z"
                                            :paths ["holes/labs/wm-contract/x.md"]}]}]
                      :facet-edges []}]
    (doseq [previous [nil
                      {:status :absent :reason :no-earlier-started-run}
                      {:status :absent :reason :previous-focus-not-retained}]]
      (let [info (focus/decision-focus-info ended-window "2026-01-02T00:00:00Z" previous)]
        (is (= [:retained "WM"] [(:status info) (:focus info)]) (pr-str previous))))))
