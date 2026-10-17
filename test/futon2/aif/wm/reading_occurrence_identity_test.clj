(ns futon2.aif.wm.reading-occurrence-identity-test
  "E-implementing-G-over-cascades requisition packet: the occurrence-identity
  boundary of the reading-family construction/scoring path.

  The production route is reading-registry -> target-policy-family ->
  analysis-cascade -> cascade-shape-g (structural-identity, fit-evidence,
  arranged->candidate) -> family-selection -> prefix-admission
  reading-policy-key.  Every fixture here cites one pattern at several
  fragments, so a pattern-grain lookup anywhere on that route substitutes one
  occurrence's data for another's and these tests catch it."
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.analysis-cascade :as analysis]
            [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.wm.family-selection :as family-selection])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- with-empty-learning-ledger [run]
  (let [root (.getPath (.toFile (Files/createTempDirectory
                                 "occurrence-identity-ledger-"
                                 (make-array FileAttribute 0))))]
    (with-redefs [ledger/default-root root]
      (run))))

(use-fixtures :each with-empty-learning-ledger)

(defn- sha [c]
  (apply str (repeat 64 c)))

(def dup "p/dup")
(def other "p/other")

(defn- pref [pattern c]
  {:id pattern :status "candidate" :rationale (str "fixture " pattern)
   :source_sha256 (sha c)})

(defn- fragment [i text refs & [rejections]]
  (cond-> {:start (* 10 i) :end (* 10 (inc i)) :text text :intent "act"
           :target "T-occ" :rationale (str "fragment " i)
           :relations (if (zero? i) ["context"] ["action"])}
    (seq refs) (assoc :pattern_refs (vec refs))
    (seq rejections) (assoc :pattern_rejections (vec rejections))))

;; The production-shaped reading: p/dup cited at fragments 0, 1 and 2 (three
;; DISTINCT occurrences), p/other sharing fragment 1 with the second p/dup
;; citation (an overlap/gluing candidate), descriptive reading order
;; 0 -> 1 -> 2 supplied by fragment order with role labels.
(def occurrence-reading
  {:status "analyzed"
   :sentences [{:id "s1"
                :fragments [(fragment 0 "first" [(pref dup "a")])
                            (fragment 1 "second" [(pref other "b") (pref dup "c")])
                            (fragment 2 "third" [(pref dup "d")])]}]})

(defn- overlap-cascade [reading]
  (first (:cascades (analysis/analysis->cascades reading {:mode :overlap}))))

(defn- policy [id cascade reading]
  {:target "T-occ" :mission "T-occ" :policy-id (str "T-occ/policy-" id)
   :kind :reading-overlap :analysis reading :cascade cascade})

(deftest occurrences-survive-construction-distinct
  (let [cascade (overlap-cascade occurrence-reading)
        dup-nodes (filter #(= dup (:pattern %)) (:nodes cascade))]
    (is (= 3 (count dup-nodes)) "three citations of p/dup stay three nodes")
    (is (= 3 (count (set (map :fragment-index dup-nodes))))
        "their fragment indices are distinct occurrence keys")
    (is (= #{dup other} (set (map :pattern (:nodes cascade)))))
    (is (some #(= :overlap (:kind %)) (:edges cascade))
        "the shared fragment contributes an overlap (gluing candidate) edge")
    (is (some #(#{:context-enables :precedes} (:kind %)) (:edges cascade))
        "descriptive reading order is carried by role-qualified edges")))

(deftest candidate-units-and-patterns-are-occurrence-keyed
  (let [cascade (overlap-cascade occurrence-reading)
        candidate (shape-g/arranged->candidate "T-occ" "T-occ/policy-1" cascade)
        co (get-in candidate [:precedence :co-apply])
        {:keys [units patterns]} co]
    (is (= 4 (count units)))
    (is (= 4 (count (set units))) "no occurrence key collapses")
    (is (= 3 (count (filter #(= dup (first %)) units))))
    (is (= #{dup other} (set (map first units))))
    (is (= 4 (count (into [] patterns)))
        "the pattern map is keyed by occurrence id, not pattern id")
    ;; provenance Packet A needs, per occurrence
    (let [occ-nodes (vec (get (get candidate :arrangement) :nodes))]
      (is (every? #(= :unread (:status (get % :blend-square))) occ-nodes)
          "typed unread four-map square per occurrence, not fabricated")
      (is (= #{(sha "a") (sha "b") (sha "c") (sha "d")}
             (set (map :source-sha256 occ-nodes)))
          "each occurrence keeps ITS cited pattern bytes' sha")
      (is (= [{:start 0 :end 10} {:start 10 :end 20} {:start 10 :end 20}
              {:start 20 :end 30}]
             (mapv :fragment-span occ-nodes)))
      (is (every? string? (map :sentence-id occ-nodes))))))

(deftest reading-policy-key-is-occurrence-grain
  (let [cascade (overlap-cascade occurrence-reading)
        candidate (shape-g/arranged->candidate "T-occ" "T-occ/policy-1" cascade)
        key (prefix-admission/reading-policy-key candidate)
        node-ids (mapv first (get-in key [3 :nodes]))]
    (is (some? key))
    (is (= 4 (count node-ids)))
    (is (= 3 (count (filter #(= dup (second %)) (get-in key [3 :nodes])))))
    (is (= 4 (count (set node-ids)))
        "two citations of one pattern are distinct in the history identity")))

(deftest structural-identity-does-not-collapse-fragment-variants
  ;; control: the SAME patterns in the SAME order, cited at DIFFERENT
  ;; fragments.  A pattern-grain node sequence sees one policy where the
  ;; reading produced two.
  (let [reading-a {:status "analyzed"
                   :sentences [{:id "s1"
                                :fragments [(fragment 0 "a1" [(pref dup "a")])
                                            (fragment 1 "a2" [(pref other "b")])]}]}
        ;; fragment 0 carries only a rejection, so it yields no node but
        ;; still consumes fragment index 0: same pattern ORDER, different
        ;; occurrence keys ([dup 1] [other 2] instead of [dup 0] [other 1])
        reading-b {:status "analyzed"
                   :sentences [{:id "s1"
                                :fragments [(fragment 0 "b0" []
                                              [{:id "p/x" :reason "beside"
                                                :query "p/x"}])
                                            (fragment 1 "b1" [(pref dup "a")])
                                            (fragment 2 "b2" [(pref other "b")])]}]}
        pa (policy 1 (overlap-cascade reading-a) reading-a)
        pb (policy 2 (overlap-cascade reading-b) reading-b)
        pattern-grain (fn [p]
                        (mapv #(or (:pattern %) (:id %) %)
                              (:nodes (:cascade p))))]
    (is (= 1 (count (distinct (map pattern-grain [pa pb]))))
        "the pre-fix pattern-grain view collapsed these two policies")
    (is (= 2 (count (distinct (map shape-g/structural-identity [pa pb]))))
        "occurrence-grain structural identity keeps both")))

(deftest fit-evidence-is-per-occurrence
  ;; The reading accepts p/dup at fragments 0 and 1 only.  The cascade
  ;; carries two p/dup occurrences, one at fragment 0 and one at fragment 5
  ;; -- a citation the reading never made.  A pattern-grain join substitutes
  ;; fragment 0/1's accepted evidence into the fragment-5 occurrence; the
  ;; occurrence join calls it what it is: :not-read.
  (let [reading {:status "analyzed"
                 :sentences [{:id "s1"
                              :fragments [(fragment 0 "first" [(pref dup "a")])
                                          (fragment 1 "second" [(pref dup "c")])]}]}
        cascade {:nodes [{:pattern dup :fragment-index 0 :roles ["action"]}
                         {:pattern dup :fragment-index 5 :roles ["action"]}]
                 :edges [] :precedence [dup]}
        score (shape-g/score-policy (policy 1 cascade reading))
        fit-nodes (get-in score [:fit :nodes])
        by-occ (into {} (map (juxt :occurrence-id identity)) fit-nodes)
        pattern-grain-status
        (fn [pattern]
          (let [evidence (mapcat :pattern_refs
                                 (mapcat :fragments (:sentences reading)))]
            (if (some #(= pattern (:id %)) evidence) :accepted :not-read)))]
    (is (= :computed (:status score)))
    (is (= 2 (count fit-nodes)) "one fit node per occurrence, not per pattern")
    (is (= :accepted (get-in by-occ [[dup 0] :status])))
    (is (= [0] (mapv :fragment-index (:reading-evidence (by-occ [dup 0]))))
        "an occurrence's evidence is ITS fragment's refs only")
    (is (= :not-read (get-in by-occ [[dup 5] :status])))
    ;; negative control: the pattern-grain lookup this repairs substitutes
    ;; the other occurrence's accepted evidence for the unread citation
    (is (= :accepted (pattern-grain-status dup))
        "the pre-fix join would have called the unread citation accepted")
    (is (not= (get-in by-occ [[dup 5] :status]) (pattern-grain-status dup)))
    (is (= [[dup 5]] (get-in score [:fit :interpretation-owed]))
        "the owed interpretation names the occurrence, not the pattern")
    (is (Double/isFinite (double (:f score))))
    (is (Double/isFinite (double (:g score))))))

(deftest occurrence-keys-reach-the-one-selector-boundary
  (let [cascade (overlap-cascade occurrence-reading)
        family {:status :computed :target-id "T-occ"
                :policies [(policy 1 cascade occurrence-reading)]
                :distinct-count 1 :reported-count 1
                :failures [] :failure-count 0}
        result (family-selection/select-over-families
                [family] {:beta 1 :enactment-fold nil :novelty-inputs {}})]
    (is (= :selected (:status result)))
    (is (= 1 (count (:ranked result))))
    (let [entry (first (:ranked result))
          key (get-in entry [:action :policy-key])]
      (is (some? key))
      (is (= 4 (count (get-in key [3 :nodes]))))
      (is (= 4 (count (set (map first (get-in key [3 :nodes])))))
          "the selection certificate's history key keeps all four occurrences")
      (is (= 3 (count (filter #(= dup (second %))
                              (get-in key [3 :nodes]))))))
    (let [staged (get-in result [:policy-stages "T-occ" :scored])]
      (is (= 1 (count staged)))
      (is (= 4 (count (get-in (first staged) [3 :nodes])))
          "the scored stage key is one occurrence-grain reading-policy key"))))
