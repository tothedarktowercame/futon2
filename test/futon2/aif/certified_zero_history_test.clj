(ns futon2.aif.certified-zero-history-test
  "Prefix-F applicability (codex-68 ruling, 2026-10-15): a policy the pinned
  flight ledger proves has never run carries a certified zero-history carrier
  and may be enacted; unknown history stays :not-supplied and refuses; a
  policy with recorded history needs its own computed Prefix-F.

  The ledger fixture is a real flight record (data/wm-interpretations/flights,
  an explicit-absence enactment) plus records written in the flight writer's
  shapes (flight.clj: an enactment entry carries :record-path and :step)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.enactment-fold-source :as fold-source]
            [futon2.aif.full-loop-runner]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.policy-prefix-evidence :as evidence]
            [wm-run-facts :as facts]))

(def real-flight "test/fixtures/prefix-history/flight-278b6988.edn")

(def action {:kind :cascade-candidate :id :C1 :target "M-selected"
             :precedence [:pattern/a :pattern/b]})

(def other-action {:kind :cascade-candidate :id :C2 :target "M-other"
                   :precedence [:pattern/c]})

(defn- temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "prefix-history-" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- step [act occurrence & {:as more}]
  (merge {:schema :wm/conditioning-step-v1
          :status :present
          :policy-key (admission/candidate-key act)
          :occurrence occurrence
          :s-prev {:value {:s0 1}}
          :q {:s0 1}
          :f 0.5}
         more))

(defn- flight [id target enactments]
  {:plan {} :flight {:flight/id id :target target :enactments enactments}})

(defn- ledger!
  "A flights directory: the real explicit-absence record plus RECORDS."
  [records]
  (let [dir (temp-dir)]
    (io/copy (io/file real-flight) (io/file dir "flight-278b6988.edn"))
    (doseq [[name rec] records]
      (spit (io/file dir name) (if (string? rec) rec (pr-str rec))))
    dir))

(def other-policy-flight
  ;; another policy really ran: its step names its own key
  ["flight-other.edn"
   (flight "f-other" "M-other"
           [{:record-path "/runs/c9.edn" :click-id "c9"
             :step (step other-action {:click "c9" :flight "f-other"})}])])

(defn- ranked-for
  "Rank ACT against the ledger in DIR exactly as cascade-decision does: the
  prefixes and the census come from one conditioning snapshot, and :pin is
  that census's authoritative pin."
  [act dir]
  (let [steps (fold-source/conditioning-steps (str dir))
        prefixes (admission/prefixes [act] steps)
        census (admission/history-census steps)]
    {:steps steps
     :pin (admission/census-pin census)
     :ranked (evidence/production-ranked
              [{:action act :controller-score 0.0}]
              {:conditioning-status :admitted}
              prefixes
              census)}))

(defn- select [ranked pin]
  (policy/select-action-cascades ranked {:beta 1 :policy-history-pin pin}))

(defn- refusal-kind [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e
                 (or (:kind (ex-data e)) (get-in (ex-data e) [:refusal :kind])))))

(defn- selected-entry [decision]
  ((ns-resolve 'futon2.aif.full-loop-runner 'selected-entry) {:decision decision}))

(defn- export-record [decision]
  {:decision (assoc decision :chosen (:action decision))
   :participants {:author "author"}
   :terminal-receipt {:outcome :changed}})

(def snap {:open-missions #{"M-selected"} :open-excursions #{} :open-tickets #{}
           :patterns #{"p1"} :seats #{"author"} :library-pattern-count 1})

(defn- absences [decision]
  (get-in (facts/facts-for-record (export-record decision) "r" snap)
          [:facts "pathAbsenceCount"]))

(deftest certified-zero-history-enacts
  (let [dir (ledger! [other-policy-flight])
        {:keys [ranked steps pin]} (ranked-for action dir)
        prefix (:f-prefix (first ranked))
        decision (select ranked pin)
        candidate (first (get-in decision [:selection-certificate :candidates]))]
    (testing "the census is complete over the real record and the written one"
      (is (= :complete (:status (admission/history-census steps))))
      (is (= 2 (get-in prefix [:ledger :records]))))
    (testing "the carrier is the applicability certificate, not F"
      (is (= :not-applicable (:status prefix)))
      (is (= :certified-zero-history (:reason prefix)))
      (is (= 0 (:history-count prefix)))
      (is (nil? (:f prefix)))
      (is (evidence/certified-zero-history? prefix action pin)))
    (testing "the decision records the pin apart from the candidates"
      (is (= pin (get-in decision [:selection-certificate :policy-history-pin]))))
    (testing "the selection consumes it and the law names the omission"
      (is (= action (:action decision)))
      (is (= :not-applicable (:f-status candidate)))
      (is (= {:status :not-applicable :reason :certified-zero-history
              :term :historical-prefix-f :consumed :no-term}
             (:f-consumed candidate)))
      (is (= :f-not-applicable-certified-zero-history
             (get-in decision [:selection-certificate :law-applied :reason])))
      (is (= [action] (get-in decision [:selection-certificate :law-applied
                                        :candidates-f-not-applicable]))))
    (testing "enactment accepts it and the exporter counts no absence"
      (is (= action (:action (selected-entry decision))))
      (let [export (facts/facts-for-record (export-record decision) "r" snap)]
        (is (= 0 (get-in export [:facts "pathAbsenceCount"])))
        (is (empty? (get-in export [:diagnostics :absenceGroups])))))))

(deftest carrier-from-another-ledger-refuses
  ;; Ledgers A and B are both complete and both record no step of ACTION,
  ;; so each yields a coherent carrier. Only the decision's own pin counts.
  (let [a (ranked-for action (ledger! [other-policy-flight]))
        b (ranked-for action (ledger! []))
        carrier-a (get-in a [:ranked 0 :f-prefix])]
    (is (not= (:pin a) (:pin b)))
    (is (evidence/certified-zero-history? carrier-a action (:pin a)))
    (is (not (evidence/certified-zero-history? carrier-a action (:pin b))))
    (testing "the selector refuses A's carrier under pin B"
      (is (= :invalid-policy-prefix (refusal-kind #(select (:ranked a) (:pin b))))))
    (testing "enactment refuses A's carrier when the decision's pin is B"
      (let [decision (select (:ranked a) (:pin a))
            swapped (assoc-in decision [:selection-certificate :policy-history-pin] (:pin b))
            unpinned (update decision :selection-certificate dissoc :policy-history-pin)]
        (is (= action (:action (selected-entry decision))))
        (is (= 0 (absences decision)))
        (is (nil? (selected-entry swapped)))
        (is (pos? (absences swapped)))
        (is (nil? (selected-entry unpinned)))
        (is (pos? (absences unpinned)))))
    (testing "an incomplete authoritative pin certifies nothing"
      (is (not (evidence/certified-zero-history?
                carrier-a action (assoc (:pin a) :status :incomplete)))))))

(deftest unknown-history-refuses
  (doseq [[label dir]
          [[:no-ledger (io/file (temp-dir) "absent")]
           [:unparseable-record (ledger! [["flight-bad.edn" "{:flight"]])]
           [:not-a-flight-record (ledger! [["flight-str.edn" "\"a string\""]])]
           [:unattributed-enactment
            ;; a real enactment entry that never got its step: it could have
            ;; been this policy
            (ledger! [["flight-x.edn"
                       (flight "f-x" "M-selected"
                               [{:record-path "/runs/c1.edn" :click-id "c1"}])]])]
           [:step-without-policy-key
            (ledger! [["flight-y.edn"
                       (flight "f-y" "M-selected"
                               [{:record-path "/runs/c2.edn" :click-id "c2"
                                 :step (dissoc (step action {:click "c2" :flight "f-y"})
                                               :policy-key)}])]])]]]
    (testing (name label)
      (let [{:keys [ranked steps pin]} (ranked-for action dir)]
        (is (= :incomplete (:status (admission/history-census steps))))
        (is (= :not-supplied (get-in ranked [0 :f-prefix :status])))
        (is (= :free-energy-not-supplied (refusal-kind #(select ranked pin))))))))

(deftest forged-or-unpinned-census-refuses
  (let [dir (ledger! [other-policy-flight])
        {:keys [ranked pin]} (ranked-for action dir)
        carrier (:f-prefix (first ranked))
        history-dir (ledger! [["flight-mine.edn"
                               (flight "f-mine" "M-selected"
                                       [{:record-path "/runs/c3.edn" :click-id "c3"
                                         :step (step action {:click "c3" :flight "f-mine"}
                                                     :status :refused
                                                     :reason :unmeasured-class)}])]])
        history-census (admission/history-census
                        (fold-source/conditioning-steps (str history-dir)))
        forgeries
        {;; content edited after pinning: the digest no longer matches
         :edited-census (update-in carrier [:census :records] pop)
         ;; the carrier's own pin rewritten
         :repinned (assoc-in carrier [:ledger :digest] (apply str (repeat 64 "0")))
         ;; no directory: not a pinned ledger
         :unpinned (let [c (assoc (:census carrier) :dir nil)]
                     (assoc carrier :census c))
         ;; a complete, correctly digested census that records this policy
         ;; running, presented as zero history
         :nonzero-history (assoc carrier :census history-census
                                 :ledger {:digest (:digest history-census)
                                          :dir (:dir history-census)
                                          :records (count (:records history-census))})
         ;; the certificate of another policy
         :foreign-policy (assoc carrier :policy other-action)}]
    (is (evidence/certified-zero-history? carrier action pin))
    (doseq [[label forged] forgeries]
      (testing (name label)
        (is (not (evidence/certified-zero-history? forged action pin)))
        (is (= :invalid-policy-prefix
               (refusal-kind #(select [{:action action :controller-score 0.0
                                        :f-prefix forged}]
                                      pin))))
        (let [decision (select [{:action action :controller-score 0.0 :f-prefix carrier}] pin)
              row (first (get-in decision [:selection-certificate :candidates]))]
          (is (some? (selected-entry decision)))
          (let [forged-decision (assoc-in decision [:selection-certificate :candidates]
                                          [(assoc row :f-prefix forged)])]
            (is (nil? (selected-entry forged-decision)))
            (is (pos? (absences forged-decision)))))))))

(deftest nonzero-history-gets-no-escape
  (testing "a recorded but unadmitted step keeps the policy :not-supplied"
    (let [dir (ledger! [["flight-mine.edn"
                         (flight "f-mine" "M-selected"
                                 [{:record-path "/runs/c3.edn" :click-id "c3"
                                   :step (step action {:click "c3" :flight "f-mine"}
                                               :status :refused
                                               :reason :unmeasured-class)}])]])
          {:keys [ranked steps pin]} (ranked-for action dir)
          census (admission/history-census steps)]
      (is (= :complete (:status census)))
      (is (= 1 (admission/census-history-count census (admission/candidate-key action))))
      (is (= :not-supplied (get-in ranked [0 :f-prefix :status])))
      (is (nil? (evidence/certified-zero-history {:action action} census)))
      (is (= :free-energy-not-supplied (refusal-kind #(select ranked pin))))))
  (testing "an admitted step yields the computed Prefix-F, which enacts"
    (let [dir (ledger! [["flight-mine.edn"
                         (flight "f-mine" "M-selected"
                                 [{:record-path "/runs/c4.edn" :click-id "c4"
                                   :step (step action {:click "c4" :flight "f-mine"})}])]])
          {:keys [ranked pin]} (ranked-for action dir)
          decision (select ranked pin)
          candidate (first (get-in decision [:selection-certificate :candidates]))]
      (is (= :computed (get-in ranked [0 :f-prefix :status])))
      (is (= 0.5 (:f-consumed candidate)))
      (is (= action (:action (selected-entry decision))))
      (is (= 0 (absences decision)))
      (testing "computed F does not depend on the pin"
        (is (= action (:action (selected-entry
                                (update decision :selection-certificate
                                        dissoc :policy-history-pin)))))))))
