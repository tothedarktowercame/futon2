(ns futon2.aif.repair-publication-write-test
  "HPUB-WRITE-I (PROOF-2a-PLAN ⟨1⟩3; closing packet from H13-D §2(d)): the
  WRITE side of H-publish witnessed once, hermetically. A repair obligation
  written AFTER H-PUBLISH-A1's rule — driven through the real
  repair-discharge/finalize! (bind-selected!, the review gate, the
  eligibility requires, evidence/observe!, record-implementation!, then
  successor-resolution! with the context finalize! retains) — reaches
  :receipt-committed via the real repair-discharge-receipt/publish! (a real
  git commit in a temp repo), and a run record carrying that
  publication-result! value under [:repair/publication] is read back from
  disk with :status :receipt-committed and no refusal.

  Fixture: futon2.aif.repair-discharge-test's fixture/attempt (the same
  hermetic temp store + temp git repo + admitted fixture evaluator that
  test's two-phases-and-crash-regeneration drives to :receipt-committed
  through catch-up!); here finalize! itself returns the publication result
  on the successor close. The run-record assembly follows
  publication-observed-test (the read-side witness): a record map with the
  catch-up!/publication-result! entries under :repair/publication, observed
  through flight-runner/observe-publication-fn — here the map is written to
  disk and read back before observing.

  Bad cases: the same close with the context removed refuses
  :discharge-context-missing at the write (A1) and writes no record; a
  legacy-shaped context-free resolution with an A2 marker in the same store
  reports :publication-unreachable with its marker class and does not block
  the post-A1 close.

  Temp store roots and temp repos only; data/ is never touched."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.repair-discharge :as discharge]
            [futon2.aif.repair-discharge-receipt :as receipt]
            [futon2.aif.repair-discharge-test :as dt]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir [prefix]
  (str (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(def repair-id "repair-fixture")

(defn- post-a1-close
  "Drive a full post-A1 discharge in dt's hermetic fixture: implementation
  close A, then successor close B whose finalize! calls the real
  publication-result!/publish!. Returns {:keys [root repo result] finding}."
  []
  (let [{:keys [base finding] :as fx} (dt/fixture)
        a (dt/attempt base "A" "2026-09-27T00:00:01Z")
        b (dt/attempt base "B" "2026-09-27T00:00:02Z")]
    (is (= :awaiting-successor (:status (discharge/finalize! a)))
        "the implementation close writes the implementation record")
    (assoc fx :result (discharge/finalize! b) :finding finding)))

(deftest post-a1-close-reaches-receipt-committed-on-the-run-record
  (let [{:keys [result]} (post-a1-close)]
    (is (= :receipt-committed (:status result)) (pr-str result))
    (is (= repair-id (:repair/id result)))
    (is (true? (:repair/discharged? result)))
    (is (not (contains? result :error))
        "a committed receipt carries no refusal payload")
    (is (string? (get-in result [:receipt :sha]))
        "the receipt names the commit publish! made in the temp repo")
    ;; The run-record assembly the read-side witness used: the tick's
    ;; publication results under :repair/publication, one per :repair/id.
    (let [path (io/file (temp-dir "hpub-run-record-") "tick-run-record-run-B.edn")
          _ (spit path (pr-str {:run/id "run-B" :repair/publication [result]}))
          record (edn/read-string (slurp path))
          entries (:repair/publication record)
          mine (filterv #(= repair-id (:repair/id %)) entries)]
      (is (= 1 (count mine)) "the run record carries [:repair/publication] for the id")
      (is (= :receipt-committed (:status (first mine))))
      (is (not-any? #(#{:publication-refused :publication-unreachable} (:status %)) entries)
          "no :publication-refused, no :publication-unreachable")
      ;; The read side (H13-D's witness) observes this record as published.
      (let [obs ((fr/observe-publication-fn {:fetch-run-record (fn [_] record)
                                             :repair-id-fn (constantly repair-id)})
                 {:target (str "T-" repair-id)} {:click-id "run-B"})]
        (is (true? (get-in obs [:publication-observed :observed])))
        (is (= :receipt-committed
               (get-in obs [:publication-observed :evidence :status])))))))

(deftest the-same-close-without-context-refuses-at-the-write
  ;; A1: finalize!'s successor close goes through repair/successor-resolution!;
  ;; remove the context and the write refuses :discharge-context-missing
  ;; before any record exists. (The refusal precedes close validation in
  ;; successor-resolution!, so the close maps here only name the shape.)
  (let [{:keys [root repo base finding]} (dt/fixture)
        a (dt/attempt base "A" "2026-09-27T00:00:01Z")
        _ (is (= :awaiting-successor (:status (discharge/finalize! a))))
        impl (:value (repair/discharge-record root "implementations" repair-id))
        sha (apply str (repeat 64 "a"))
        close-args {:obligation (assoc finding :repair/implementation impl)
                    :repair-close {:attempt/id "A" :run/id "run-A" :repair/id repair-id
                                   :closed-at "2026-09-27T00:00:01Z"
                                   :commit (:commit (:artifact base))
                                   :review-receipt-ids ["review-A"] :review-sha256 sha
                                   :grounded? true}
                    :successor-close {:attempt/id "B" :run/id "run-B" :repair/id repair-id
                                      :closed-at "2026-09-27T00:00:02Z"
                                      :witness-ref "successor-witness.edn"
                                      :witness-sha256 sha :grounded? true
                                      :production-shaped? true
                                      :witness {:resolved? true :dial-moved? true}}
                    :authority {:decided-by "reviewer" :review-job "review-B"}
                    ;; :discharge-context deliberately absent
                    :resolution-read-fn (constantly nil)
                    :resolve-fn (partial repair/resolve! root)}
        refusal (try (repair/successor-resolution! close-args) nil
                     (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :discharge-context-missing (:repair-discharge/refusal refusal)))
    (is (= :successor-validation (:phase refusal)))
    (is (= repair-id (:repair/id refusal)))
    (is (nil? (repair/discharge-record root "resolutions" repair-id)))
    (is (not (.exists (io/file root "resolutions" (str repair-id ".edn"))))
        "the refusal precedes the write")
    (is (= (dt/git repo "rev-parse" "HEAD") (dt/git repo "rev-parse" "HEAD"))
        "no publication was attempted")
    (is (not (.exists (io/file repo (receipt/receipt-path repair-id)))))))

(deftest a-legacy-resolution-in-the-same-store-does-not-block-the-post-a1-close
  ;; A2's disposition beside A1's guard: a legacy-shaped resolution (no
  ;; :repair/discharge-context anywhere, as all 68 pre-A2 records were) with
  ;; its :wm/publication-unreachable-v1 marker reports
  ;; :publication-unreachable with the marker class, and the post-A1 close in
  ;; the same store still publishes.
  (let [{:keys [root repo result]} (post-a1-close)
        _ (is (= :receipt-committed (:status result)) (pr-str result))
        legacy-id "repair-legacy-context-free"
        _ (spit (doto (io/file root "implementations" (str legacy-id ".edn"))
                  io/make-parents)
                (pr-str {:repair/id legacy-id :repair/phase :implementation
                         :implementation-attempt "repair-1"}))
        _ (spit (doto (io/file root "resolutions" (str legacy-id ".edn"))
                  io/make-parents)
                (pr-str {:repair/id legacy-id :repair/status :resolved
                         :validation-attempt "successor-1"}))
        _ (repair/write-publication-unreachable!
           root legacy-id
           {:schema :wm/publication-unreachable-v1 :repair/id legacy-id
            :class :late-script-b :reason ":resolution-context-unavailable"
            :ground "H-PUBLISH-D 5cbf0031" :ruled-by "joe" :ruled-at "2026-09-24"
            :written-by "kimi-5" :written-at "2026-09-27T00:00:00Z"})
        results (receipt/catch-up! root repo)
        by-id (into {} (map (juxt :repair/id identity)) results)
        legacy (get by-id legacy-id)
        mine (get by-id repair-id)]
    (is (= 2 (count results)))
    (is (= :publication-unreachable (:status legacy)) (pr-str legacy))
    (is (= :late-script-b (:class legacy)))
    (is (= "H-PUBLISH-D 5cbf0031" (:ground legacy)))
    (is (false? (:repair/discharged? legacy)))
    (is (= :receipt-committed (:status mine)) (pr-str mine))
    (is (true? (:repair/discharged? mine))
        "the legacy marker does not block the post-A1 close")))
