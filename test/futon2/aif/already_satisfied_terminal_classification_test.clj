(ns futon2.aif.already-satisfied-terminal-classification-test
  "Pinned acceptance for producer-qualified already-satisfied terminal closes.

  This boundary is non-injury but not repair success: an attributable retry is
  retained as :retry-inconclusive, never discharged."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def pinned-record-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-24-d0608eec-cca3-45de-9c19-14d6db26577d.edn")
(def pinned-record-sha256
  "6ca9604a70652c6dfb27c834248e5c875816540c285278f5c3918347e8fe9715")
(def pin-a {:path "fixtures/a.edn"
            :sha256 "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"})
(def patch "95145f09395145f09395145f09395145f0939514")
(def review "42bfb4bb542bfb4bb542bfb4bb542bfb4bb542bf")
(def integration "5a5b421bc5a5b421bc5a5b421bc5a5b421bc5a5b")

(defn- sha256 [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (java.nio.file.Files/readAllBytes (.toPath (io/file path))))
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn- pinned-record []
  (is (.isFile (io/file pinned-record-path)))
  (is (= pinned-record-sha256 (sha256 pinned-record-path)))
  (edn/read-string (slurp pinned-record-path)))

(defn- append! [history event]
  (let [result (ledger/append-transition history event)]
    (is (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(defn- enacted-ledger [retry-click]
  (let [failure (terminal/classify
                 {:run/id "failed" :click/id "detected"
                  :failure {:kind :reviewer-falsifier-failed
                            :stage :reviewer-wait :target "M-x"}}
                 pin-a)
        injury-id (:injury-id failure)
        open (:ledger (terminal/integrate ledger/empty-ledger failure))
        claim (append! open {:injury-id injury-id :state :claimed :attempt 1
                             :repair-id "repair/x" :algorithm "self-heal"
                             :author-session "author/x" :reviewer-session "reviewer/x"})
        authored (append! claim {:injury-id injury-id :state :authored :attempt 1
                                 :repair-id "repair/x" :patch-sha patch
                                 :worktree "/tmp/fake"})
        reviewed (append! authored {:injury-id injury-id :state :reviewed :attempt 1
                                    :repair-id "repair/x" :verdict :approved
                                    :reviewer-sha review
                                    :reviewer-session "reviewer/x"})
        enacted (append! reviewed {:injury-id injury-id :state :enacted :attempt 1
                                   :repair-id "repair/x"
                                   :integration-commit integration
                                   :retry-click retry-click})]
    {:injury-id injury-id :ledger enacted}))

(deftest pinned-producer-qualified-close-is-non-injury-and-deduplicates
  (let [record (pinned-record)
        pin {:path pinned-record-path :sha256 pinned-record-sha256}
        classification (terminal/classify record pin)
        first-result (terminal/integrate ledger/empty-ledger classification)
        duplicate (terminal/integrate (:ledger first-result) classification)]
    (is (= :non-injury (:classification classification)))
    (is (= :producer-qualified-already-satisfied
           (:non-injury-reason classification)))
    (is (= :producer-qualified-already-satisfied
           (:observation-class classification)))
    (is (= :observation (:transition first-result)))
    (is (empty? (:injuries (ledger/active-injuries (:ledger first-result))))
        "the valid producer close opens no injury")
    (is (= :duplicate (:status duplicate)))
    (is (= (:ledger first-result) (:ledger duplicate)))))

(deftest attributable-already-satisfied-retry-is-inconclusive-not-discharged
  (let [record (assoc (pinned-record) :click/id "retry-click")
        classification (terminal/classify
                        record {:path "fixtures/already-satisfied-retry.edn"
                                :sha256 (apply str (repeat 64 "b"))})
        {:keys [injury-id ledger]} (enacted-ledger "retry-click")
        result (terminal/integrate ledger classification)
        last-entry (last (:entries (:ledger result)))
        active (:injuries (ledger/active-injuries (:ledger result)))]
    (is (= :producer-qualified-already-satisfied
           (:non-injury-reason classification)))
    (is (= :retry-inconclusive (:transition result)))
    (is (= :retry-inconclusive (:observation-kind last-entry)))
    (is (= injury-id (:injury-id last-entry)))
    (is (= :enacted (:state (get active injury-id))))
    (is (not-any? #(= :discharged (:state %)) (:entries (:ledger result))))))

(deftest unsupported-refusal-and-stale-done-remain-injuries
  (let [claimed-sha "891001d08d8b37eaf50e13f985dee30c8d34340a"
        author-job {:result (str "FULL_LOOP_AUTHOR: REFUSE already true\n"
                                 "FULL_LOOP_FEATURE_CARD: {:built \"already satisfied\", "
                                 ":want-coverage \"already true\", :matches-intent? true, "
                                 ":things-to-try [\"inspect\"]}\n")
                    :artifact-ref claimed-sha}
        unsupported (runner/author-refusal-classification
                     author-job "/repo" (constantly false))
        unsupported-record
        {:run/id "unsupported" :click/id "unsupported-click"
         :failure {:kind (:outcome unsupported) :stage :author-wait
                   :detail {:outcome (:outcome unsupported)}}}
        base (apply str (repeat 40 "c"))
        binding (runner/fresh-artifact-binding
                 {:repo-head-observation-fn
                  (fn [repo] {:repo repo :head base :observed-at-ms 2000})
                  :resolve-commit-sha-fn (fn [_ sha] (when (= sha base) base))
                  :ancestor-fn (constantly true)
                  :commit-time-ms-fn (constantly 1500)}
                 "/repo" {:repo "/repo" :head base :observed-at-ms 1000}
                 {:result (str "FULL_LOOP_AUTHOR: DONE " base)})
        stale-kind (:failure-kind
                    (runner/unvalidated-artifact-failure true base (:commit binding)))
        stale-record {:run/id "stale-done" :click/id "stale-click"
                      :failure {:kind stale-kind :stage :author-wait
                                :detail {:metadata {:outcome :already-satisfied}}}}
        unrelated-record {:run/id "unrelated" :click/id "unrelated-click"
                          :failure {:kind :review-failed :stage :reviewer-wait
                                    :detail {:metadata
                                             {:outcome :already-satisfied
                                              :grounded-commit
                                              {:repo "futon5a" :sha claimed-sha}}}}}]
    (is (= :invalid-author-refusal (:outcome unsupported)))
    (is (= :injury (:classification (terminal/classify unsupported-record pin-a))))
    (is (= :artifact-binding-mismatch stale-kind))
    (is (= :injury (:classification (terminal/classify stale-record pin-a))))
    (is (= :injury (:classification (terminal/classify unrelated-record pin-a))))))
