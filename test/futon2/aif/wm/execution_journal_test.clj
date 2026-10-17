(ns futon2.aif.wm.execution-journal-test
  "Packet C focused tests: the append-only execution journal.  Every
  planted control names the defect it must catch."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.wm.execution-journal :as j])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- hex? [s] (boolean (and s (re-matches #"[0-9a-f]{64}" s))))

(def cascade-digest (evidence/value-digest [:packet-c :test-cascade]))
(def other-cascade-digest (evidence/value-digest [:packet-c :other-cascade]))

(def idx {:digest cascade-digest
          :nodes #{[:p/a 0] :p/b :group-1}
          :groups #{:group-1}})

(defn- e
  ([node phase valid recorded]
   (j/entry {:journal-id cascade-digest
             :node-or-group node
             :phase phase
             :evidence {:kind (if (= :enacted phase) :verified-effect
                                  :witnessed-attempt)
                        :digest (evidence/value-digest [node phase valid])}
             :valid-time (str "2026-10-17T00:0" valid ":00Z")
             :recorded-time (str "2026-10-17T00:0" recorded ":00Z")})))

(deftest entry-shape-and-seal
  (let [entry (e :p/b :enacted 1 1)]
    (is (hex? (:entry-id entry)))
    (is (= (:entry-id entry) (j/entry-digest entry))))
  ;; malformed entries refuse construction with the exact defect
  (is (= :journal-entry-invalid
         (get-in (j/entry {:journal-id "not-a-digest" :node-or-group :p/b
                           :phase :enacted
                           :evidence {:kind :verified-effect :digest ""}
                           :valid-time "nope" :recorded-time "nope"})
                 [:refusal :kind])))
  (is (contains? (set (get-in (j/entry {:journal-id "x" :node-or-group :p/b
                                        :phase :enacted
                                        :evidence {:kind :verified-effect
                                                   :digest ""}
                                        :valid-time "2026-10-17T00:00:00Z"
                                        :recorded-time "2026-10-17T00:00:00Z"})
                                 [:refusal :defects]))
                 :journal-id-not-a-cascade-digest)))


(deftest save-reload-and-idempotent-append
  (let [store (temp-dir "journal-store-")
        en (e :p/b :enacted 1 1)]
    ;; first append writes
    (is (:appended (j/append! store en)))
    ;; identical re-append is idempotent, not a collision
    (is (not (:appended (j/append! store en))))
    ;; a DIFFERENT entry whose content collides with an existing entry-id is
    ;; refused (entry-id is content-derived, so simulate by editing phase of
    ;; a copy that keeps the id — the store must never be overwritten)
    (let [forged (assoc en :phase :attempted)]
      (is (thrown-with-msg? Exception #"refused"
                            (j/append! store forged))))
    ;; reload returns the identical entry with seal intact
    (let [loaded (j/load-journal store)]
      (is (vector? loaded))
      (is (= 1 (count loaded)))
      (is (= en (first loaded))))
    ;; tampering a stored file breaks the seal on load — typed, not silent
    (let [f (io/file store (str (:entry-id en) ".edn"))]
      (spit f (pr-str (assoc en :phase :attempted)))
      (is (= :journal-entry-seal-broken
             (:clause (j/load-journal store)))))))

(deftest foreign-cascade-node-and-group
  ;; an entry naming another cascade is foreign
  (let [foreign (assoc (e :p/b :enacted 1 1)
                       :journal-id other-cascade-digest
                       :entry-id nil)
        foreign (assoc foreign :entry-id (j/entry-digest foreign))]
    (is (= :journal-foreign-cascade
           (:clause (j/validate-journal [foreign] idx)))))
  ;; an entry naming a node outside the cascade index is foreign
  (let [foreign (assoc (e :p/zzz :enacted 1 1) :entry-id nil)
        foreign (assoc foreign :entry-id (j/entry-digest foreign))]
    (is (= :journal-foreign-node-or-group
           (:clause (j/validate-journal [foreign] idx)))))
  ;; a declared co-application group IS admitted; an undeclared one is not
  (let [group-entry (assoc (e :group-1 :enacted 1 1) :entry-id nil)
        group-entry (assoc group-entry :entry-id (j/entry-digest group-entry))]
    (is (j/certified? (j/validate-journal [group-entry] idx))))
  (let [undeclared (assoc (e :group-9 :enacted 1 1) :entry-id nil)
        undeclared (assoc undeclared :entry-id (j/entry-digest undeclared))]
    (is (= :journal-foreign-node-or-group
           (:clause (j/validate-journal [undeclared] idx))))))

(deftest temporal-ordering
  ;; recorded-time runs backwards: typed inversion
  (let [later-file-first [(e :p/b :enacted 1 5) (e [:p/a 0] :enacted 1 2)]]
    (is (= :journal-recorded-time-inversion
           (:clause (j/validate-journal later-file-first idx)))))
  ;; valid-time for the SAME node runs backwards: typed inversion
  (let [a1 (e :p/b :attempted 3 1)
        a2 (e :p/b :enacted 2 2)]
    (is (= :journal-valid-time-inversion
           (:clause (j/validate-journal [a1 a2] idx)))))
  ;; An intervening node must not hide a same-node inversion.
  (let [a1 (e :p/b :attempted 3 1)
        other (e [:p/a 0] :attempted 1 2)
        a2 (e :p/b :enacted 2 3)]
    (is (= :journal-valid-time-inversion
           (:clause (j/validate-journal [a1 other a2] idx)))))
  ;; Equal recorded times cannot reconstruct a unique append order.
  (let [a1 (e :p/b :attempted 1 1)
        a2 (e [:p/a 0] :enacted 1 1)]
    (is (= :journal-recorded-time-duplicate
           (:clause (j/validate-journal [a1 a2] idx)))))
  ;; valid-time across DIFFERENT nodes may interleave freely
  (let [a1 (e :p/b :attempted 3 1)
        a2 (e [:p/a 0] :enacted 1 2)]
    (is (j/certified? (j/validate-journal [a1 a2] idx)))))

(deftest supersession-without-mutation
  (let [orig (e :p/b :enacted 1 1)
        ;; a later entry with corrected evidence supersedes the first
        sup (j/entry {:journal-id cascade-digest
                      :node-or-group :p/b
                      :phase :enacted
                      :evidence {:kind :verified-effect
                                 :digest (evidence/value-digest [:corrected])}
                      :valid-time "2026-10-17T00:01:00Z"
                      :recorded-time "2026-10-17T00:09:00Z"
                      :supersedes (:entry-id orig)})]
    ;; the original entry is unchanged on disk; the supersede is a NEW entry
    (is (not= (:entry-id orig) (:entry-id sup)))
    (is (j/certified? (j/validate-journal [orig sup] idx)))
    ;; replay: the superseded enactment no longer counts, the live one does
    (let [state (j/replay [orig sup])]
      (is (contains? (:enacted state) :p/b))
      (is (contains? (:superseded state) (:entry-id orig))))
    ;; superseding an unknown entry is a typed error
    (let [ghost (assoc sup :supersedes (evidence/value-digest [:ghost])
                       :entry-id nil)
          ghost (assoc ghost :entry-id (j/entry-digest ghost))]
      (is (= :journal-supersede-unknown
             (:clause (j/validate-journal [ghost] idx)))))
    ;; double supersede of one entry is a conflict
    (let [sup2 (j/entry {:journal-id cascade-digest
                         :node-or-group :p/b
                         :phase :attempted
                         :evidence {:kind :witnessed-attempt
                                    :digest (evidence/value-digest [:again])}
                         :valid-time "2026-10-17T00:01:30Z"
                         :recorded-time "2026-10-17T00:09:30Z"
                         :supersedes (:entry-id orig)})]
      (is (= :journal-supersede-conflict
             (:clause (j/validate-journal [orig sup sup2] idx)))))
    ;; a sorry records a contradiction without mutating either side
    (let [sorry (j/entry {:journal-id cascade-digest
                          :node-or-group :p/b
                          :phase :sorry
                          :evidence {:kind :sorry-witness
                                     :digest (evidence/value-digest [:sorry])}
                          :valid-time "2026-10-17T00:02:00Z"
                          :recorded-time "2026-10-17T00:03:00Z"
                          :sorry {:contradicts (:entry-id orig)
                                  :why "two verified effects for one guard"}})]
      (is (map? sorry))
      (is (j/certified? (j/validate-journal [orig sorry] idx)))
      (is (contains? (:sorries (j/replay [orig sorry])) :p/b)))))

(deftest open-cascade-continuation
  ;; an open-hole entry keeps the cascade open: continuation, not failure
  (let [hole (j/entry {:journal-id cascade-digest
                       :node-or-group [:p/a 0]
                       :phase :open-hole
                       :evidence {:kind :hole-witness
                                  :digest (evidence/value-digest [:hole])}
                       :valid-time "2026-10-17T00:01:00Z"
                       :recorded-time "2026-10-17T00:02:00Z"})]
    (is (j/certified? (j/validate-journal [hole] idx)))
    (let [state (j/replay [hole])]
      (is (:open-cascade? state))
      (is (contains? (:open-holes state) [:p/a 0]))
      ;; enactment is never inferred from an open hole
      (is (empty? (:enacted state))))
    ;; and later the same cascade continues: a new enacted entry for the
    ;; resolved node joins the same journal without error
    (let [resolved (e [:p/a 0] :enacted 1 3)]
      (is (j/certified? (j/validate-journal [hole resolved] idx)))
      (is (contains? (:enacted (j/replay [hole resolved])) [:p/a 0])))))

(deftest phase-and-evidence-must-agree
  ;; An enacted label with attempt evidence is contradictory input, not a
  ;; valid no-op that replay may silently ignore.
  (let [weak (j/entry {:journal-id cascade-digest
                       :node-or-group :p/b
                       :phase :enacted
                       :evidence {:kind :witnessed-attempt
                                  :digest (evidence/value-digest [:weak])}
                       :valid-time "2026-10-17T00:01:00Z"
                       :recorded-time "2026-10-17T00:02:00Z"})]
    (is (= :journal-entry-invalid (get-in weak [:refusal :kind])))
    (is (contains? (set (get-in weak [:refusal :defects]))
                   :phase-evidence-mismatch))))

(deftest append-order-does-not-depend-on-filesystem-mtime
  (let [store (temp-dir "journal-order-")
        a1 (e :p/b :attempted 1 1)
        a2 (e :p/b :enacted 2 2)]
    (j/append! store a1)
    (j/append! store a2)
    ;; Reverse the filesystem times. Loading still reconstructs the only
    ;; permitted append order from strictly increasing recorded-time.
    (.setLastModified (io/file store (str (:entry-id a1) ".edn")) 2000)
    (.setLastModified (io/file store (str (:entry-id a2) ".edn")) 1000)
    (is (= [(:entry-id a1) (:entry-id a2)]
           (mapv :entry-id (j/load-journal store))))
    ;; Backward and equal times are refused before a new file is written.
    (is (thrown-with-msg?
         Exception #"recorded time must increase"
         (j/append! store (e [:p/a 0] :enacted 1 1))))))

(deftest store-lifecycle-through-load-and-replay
  (let [store (temp-dir "journal-lifecycle-")
        a1 (e [:p/a 0] :enacted 1 1)
        hole (j/entry {:journal-id cascade-digest
                       :node-or-group :p/b
                       :phase :open-hole
                       :evidence {:kind :hole-witness
                                  :digest (evidence/value-digest [:hole2])}
                       :valid-time "2026-10-17T00:01:00Z"
                       :recorded-time "2026-10-17T00:02:00Z"})
        a2 (e :group-1 :attempted 3 3)]
    (doseq [en [a1 hole a2]] (j/append! store en))
    (let [entries (j/load-journal store)]
      (is (= 3 (count entries)))
      (is (j/certified? (j/validate-journal entries idx)))
      (let [state (j/replay entries)]
        (is (= #{[:p/a 0]} (:enacted state)))
        (is (= #{:p/b} (:open-holes state)))
        (is (= #{:group-1} (:attempted state))
            "a declared co-application group journals as a group")))))
