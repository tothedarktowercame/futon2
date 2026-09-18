(ns futon2.aif.find-expectation-test
  "WM-08 external F2 expectation controls (agreed task futon2 371d89db).

  Five controls, exactly as agreed: correct independent input accepted;
  self-supplied author rejected (declared-role control, not authenticated
  identity); mismatched source/occurrence rejected; unexpected selected
  pattern rejected; valid but deliberately wrong expected clause rejected.
  Plus: there is no optional artifact path — nil is refused, and the
  builder pulls clause TEXT from the captured bytes, never from the
  author, so a wrong clause cannot even be declared."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.find-expectation :as fx]
            [futon2.aif.find-receipt :as find]
            [futon2.aif.find-receipt-test :as fixture]))

(def root fixture/root)
(defn refusal [f] (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(defn occurrence
  "The real fixture occurrence: record, captured bytes, the one firing
  pattern id, and its authored IF span in the captured bytes."
  []
  (let [{:keys [record captured id]} (fixture/sample)
        ctx (find/context record captured root)
        entry (get-in ctx [:repository :entries id])
        target-source (get-in record [:target :source])]
    {:record record :captured captured :id id :ctx ctx :entry entry
     :target-source target-source
     :pinned-at (get-in record [:target :pinned-at])
     :if-lines (:if-lines entry)
     :if-text (:if-text entry)}))

(def external-author {:role :reviewer :name "an agent that is not the finder"})

(defn build [occ expected]
  (fx/build-artifact {:library-root root
                      :sources (:sources (:record occ))
                      :read-bytes (:captured occ)
                      :author external-author
                      :target-source (:target-source occ)
                      :pinned-at (:pinned-at occ)
                      :expected expected}))

(deftest correct-independent-input-accepted
  (let [occ (occurrence)
        artifact (build occ {(:id occ) {:lines (:if-lines occ)}})
        result (find/find (:record occ) (:captured occ) root nil)]
    (is (= (:id occ) (first (:selected result))))
    (is (= result (fx/validate-external! (:ctx occ) artifact result))
        "an artifact built only from pinned bytes + declared spans, by an
         external declared role, validates the real occurrence's receipts")
    (testing "the four fields are full content, not span indices alone"
      (is (seq (get-in artifact [:expected (:id occ) :acknowledged-clause :text])))
      (is (= (get-in result [:receipts (:id occ) :acknowledged-clause :text])
             (get-in artifact [:expected (:id occ) :acknowledged-clause :text]))
          "the expected TEXT is the captured bytes at the declared span, matching what the receipt cites"))))

(deftest self-supplied-author-rejected
  (testing "at build time"
    (let [occ (occurrence)]
      (is (= :self-supplied-author
             (:reason (refusal #(fx/build-artifact
                                 {:library-root root
                                  :sources (:sources (:record occ))
                                  :read-bytes (:captured occ)
                                  :author {:role :interpreter :name "the interpreter itself"}
                                  :target-source (:target-source occ)
                                  :pinned-at (:pinned-at occ)
                                  :expected {(:id occ) {:lines (:if-lines occ)}}})))))))
  (testing "at validate time — a hand-made artifact declaring a finder-side role"
    (let [occ (occurrence)
          result (find/find (:record occ) (:captured occ) root nil)
          bad (assoc (build occ {(:id occ) {:lines (:if-lines occ)}})
                     :author {:role :finder :name "the finder"})]
      (is (= :self-supplied-author
             (:reason (refusal #(fx/validate-external! (:ctx occ) bad result)))))))
  (testing "the control is declared-role, and says so"
    (let [occ (occurrence)]
      (is (= "declared-role control: finder/interpreter output cannot provide its own expectations"
             (:note (refusal #(fx/build-artifact
                               {:library-root root
                                :sources (:sources (:record occ))
                                :read-bytes (:captured occ)
                                :author {:role :finder}
                                :target-source (:target-source occ)
                                :pinned-at (:pinned-at occ)
                                :expected {}}))))))))

(deftest mismatched-occurrence-rejected
  (let [occ (occurrence)
        result (find/find (:record occ) (:captured occ) root nil)]
    (testing "a stale repository digest"
      (let [stale (assoc-in (build occ {(:id occ) {:lines (:if-lines occ)}})
                            [:occurrence :repository-sha256] "a different repository")]
        (is (= :occurrence-mismatch
               (:reason (refusal #(fx/validate-external! (:ctx occ) stale result)))))
        (is (= [:repository-sha256]
               (:mismatched-fields (refusal #(fx/validate-external! (:ctx occ) stale result)))))))
    (testing "a different target"
      (let [other (assoc-in (build occ {(:id occ) {:lines (:if-lines occ)}})
                            [:occurrence :target-sha256] "another target")]
        (is (= :occurrence-mismatch
               (:reason (refusal #(fx/validate-external! (:ctx occ) other result)))))))
    (testing "a different pinned-at"
      (let [later (assoc-in (build occ {(:id occ) {:lines (:if-lines occ)}})
                            [:occurrence :pinned-at] "2026-09-19T00:00:00Z")]
        (is (= [:pinned-at]
               (:mismatched-fields (refusal #(fx/validate-external! (:ctx occ) later result)))))))))

(deftest unexpected-selected-pattern-rejected
  (let [occ (occurrence)
        result (find/find (:record occ) (:captured occ) root nil)
        empty-expectations (build occ {})]
    (is (= :unexpected-selection
           (:reason (refusal #(fx/validate-external! (:ctx occ) empty-expectations result)))))
    (is (= [(:id occ)] (:patterns (refusal #(fx/validate-external! (:ctx occ)
                                                                  empty-expectations result)))))))

(deftest deliberately-wrong-expected-clause-rejected
  (let [occ (occurrence)
        result (find/find (:record occ) (:captured occ) root nil)
        artifact (build occ {(:id occ) {:lines (:if-lines occ)}})]
    (testing "a wrong span over captured bytes is refused at BUILD (text comes from bytes, not the author)"
      (let [[lo hi] (:if-lines occ)]
        ;; one line outside the authored IF block, below it
        (is (= :not-authored-clause
               (:reason (refusal #(build occ {(:id occ) {:lines [(inc hi) (inc hi)]}})))))
        (is (= :not-authored-clause
               (:reason (refusal #(build occ {(:id occ) {:lines [(dec lo) (dec lo)]}})))))))
    (testing "a valid artifact whose expected TEXT was tampered with after build"
      (let [wrong (assoc-in artifact [:expected (:id occ) :acknowledged-clause :text]
                            "a clause the author preferred")]
        (is (= :expectation-mismatch
               (:reason (refusal #(fx/validate-external! (:ctx occ) wrong result)))))))
    (testing "a wrong route on an otherwise valid row"
      (let [wrong (assoc-in artifact [:expected (:id occ) :route] :invented-route)]
        (is (= :expectation-mismatch
               (:reason (refusal #(fx/validate-external! (:ctx occ) wrong result)))))))))

(deftest there-is-no-optional-artifact-path
  (let [occ (occurrence)
        result (find/find (:record occ) (:captured occ) root nil)]
    (is (= :expectation-artifact-required
           (:reason (refusal #(fx/validate-external! (:ctx occ) nil result)))))
    (is (= :expectation-artifact-required
           (:reason (refusal #(fx/validate-external! (:ctx occ)
                                                     {:schema :something/else} result)))))
    (is (= [1 3] [(count (:arglists (meta #'fx/validate-external!)))
                  (count (first (:arglists (meta #'fx/validate-external!))))])
        "exactly one arity of three required positionals — no skip path")))
