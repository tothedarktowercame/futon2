(ns futon2.aif.outer-loop-test
  "H-T-CALLER-I, the first layer: `select` -> `resolve-target` -> the plan's
  placement, through the loop entry's `plan-from-field!`. The field is a
  fixture and the mission text the driver test's pinned M-futon-seams; the plan
  is the real `flight-driver/plan`. Nothing is sent and nothing is written."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight-driver :as fd]
            [futon2.aif.enactment-fold-source :as fold-source]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.outer-loop :as outer-loop]
            [futon2.aif.outer-cascade :as cascade]
            [futon2.aif.outer-cascade-test :as oc-test])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def mission-text (slurp "test/fixtures/mission-criteria/M-futon-seams@futon3c-20959e4f.md"))
(def lifecycle-text (slurp "test/fixtures/mission-criteria/M-futon-seams-lifecycle@futon3c-d74a7c5a.edn"))

(defn- store-dir [] (.toFile (Files/createTempDirectory "outer-loop-store" (make-array FileAttribute 0))))

(def field
  {:considered [{:target "M-futon-seams" :kind :mission :repo "futon3c" :path "holes/missions/M-futon-seams.md"}
                {:target "M-mooted" :kind :mission :repo "futon3c" :path "holes/missions/M-mooted.md"}
                {:target "E-not-a-target" :kind :excursion :repo "futon2" :path "holes/E-not-a-target.md"}]
   :feasible [{:target "M-futon-seams" :kind :mission :next-step :ask-interpretation :eligible true
               :requisition {:absent :text-unread}}
              {:target "M-mooted" :kind :mission :next-step :read-criteria :eligible false
               :ineligible-reason :requisition/mooted}]
   :exclusions [{:target "E-not-a-target" :kind :excursion :reason :not-lifecycle-shaped
                 :what-would-make-feasible {:form "futon4/holes/mission-lifecycle.md"}}]})

(defn- entry-opts
  "plan-from-field!'s options over FIELD, planning offline."
  [store & [{:keys [field seed] :or {field field seed 42}}]]
  {:trigger :wallclock-cron :seed seed :seat "kimi-6"
   :load-field-fn (fn [] {:field field :opts {:sources {:beta-by-context {:WM {:beta 1}}}
                                              :store (str store) :code-root "/nonexistent"}})
   :plan-opts {:id "flight-test"
               :lifecycle-path "holes/labs/M-futon-seams/lifecycle.edn"
               :read-text (fn [_ _ path] (if (str/ends-with? path ".edn") lifecycle-text mission-text))
               :observe #(checks/decl-present? mission-text (:decl %))}})

(defn- files-under [dir] (vec (sort (map str (filter #(.isFile %) (file-seq (io/file dir)))))))

(deftest select-to-plan-placement
  (let [store (store-dir)
        before (files-under store)
        r (outer-loop/plan-from-field! (entry-opts store))
        p (:plan r)]
    (testing "1. only the eligible target is chosen; the ineligible and the excluded are on the record"
      (is (= "M-futon-seams" (:requisition p)))
      ;; Captured from unchanged HEAD 1114a8bd0c, seed 42, this fixture.
      (is (= 6 (count (get-in p [:wants :in-view]))))
      (is (= ["M-futon-seams"] (get-in r [:target-selection :support])))
      (is (= ["E-not-a-target" "M-mooted"] (mapv :target (get-in r [:target-selection :excluded]))))
      (is (= [:not-lifecycle-shaped :requisition/mooted] (mapv :reason (get-in r [:target-selection :excluded])))))
    (testing "4. resolve-target says :chosen, with the seed and the selection, and the plan's placement carries them"
      (is (= {:target "M-futon-seams" :target-source :chosen :draw-seed 42 :target-selection (:target-selection r)}
             (:placement p)))
      (is (= (:target-selection r) (:target-selection p)) "and the plan carries the selection on every plan")
      (is (= {"M-futon-seams" {:absent :no-target-grain-g}} (get-in p [:target-selection :g])))
      (is (= {:basis :target-habit-prior :alpha 1
              :counts {"M-futon-seams" 0} :unjoined []}
             (get-in p [:target-selection :E]))))
    (testing "the chosen entry rides beside it, and the plan is a plan"
      (is (= :ask-interpretation (get-in p [:resolved-steps :field-entry :next-step])))
      (is (false? (:run? p))))
    (testing "answering seat: the given seat, else a typed absence"
      (is (= "kimi-6" (:answering-seat p)))
      (is (= {:absent :no-seat-configured}
             (:answering-seat (:plan (outer-loop/plan-from-field! (dissoc (entry-opts store) :seat)))))))
    (testing "nothing is written"
      (is (= before (files-under store))))))

(deftest a-hand-target-alongside-a-chosen-one-is-recorded
  (let [store (store-dir)
        p (:plan (outer-loop/plan-from-field!
                  (update (entry-opts store) :plan-opts assoc :target "M-hand")))]
    (is (= "M-futon-seams" (:requisition p)))
    (is (= "M-hand" (get-in p [:placement :hand-target-overridden])))))

(deftest the-same-seed-plans-the-same-flight-target
  (let [store (store-dir)
        two (assoc field :feasible (conj (:feasible field)
                                        {:target "M-other" :kind :mission :next-step :ready :eligible true}))
        two (update two :considered conj {:target "M-other" :kind :mission :repo "futon3c" :path "holes/missions/M-other.md"})
        sel (fn [seed] (with-redefs [fd/plan (fn [o] {:chosen (:chosen-target o)})]
                         (get-in (outer-loop/plan-from-field! (entry-opts store {:field two :seed seed}))
                                 [:plan :chosen])))]
    (is (= (sel 11) (sel 11)))
    (is (= #{"M-futon-seams" "M-other"} (set (map sel (range 40)))))))

(deftest an-empty-support-plans-nothing-and-throws-nothing
  (let [store (store-dir)
        none (update field :feasible (fn [fs] (mapv #(assoc % :eligible false :ineligible-reason :requisition/mooted) fs)))
        r (outer-loop/plan-from-field! (entry-opts store {:field none}))]
    (is (= {:absent :no-eligible-target} (:plan r)))
    (is (= {:absent :no-eligible-target} (get-in r [:target-selection :chosen])))
    (is (= 3 (count (get-in r [:target-selection :excluded]))))))

(deftest the-hand-path-says-so-on-every-plan
  (let [p (fd/plan {:target "M-futon-seams" :seat "kimi-6" :repo "futon3c" :path "holes/missions/M-futon-seams.md"
                    :lifecycle-path "holes/labs/M-futon-seams/lifecycle.edn" :store (str (store-dir)) :id "flight-hand"
                    :read-text (fn [_ _ path] (if (str/ends-with? path ".edn") lifecycle-text mission-text))
                    :observe #(checks/decl-present? mission-text (:decl %))
                    :sources {:beta-by-context {:WM {:beta 1}}}})]
    (is (= {:target "M-futon-seams" :target-source :hand-placed} (:placement p)) "placement is exactly as before")
    (is (= {:absent :hand-placed} (:target-selection p)))))

(deftest a-chosen-target-with-no-selection-record-is-typed
  (let [p (fd/plan {:chosen-target "M-futon-seams" :seat "kimi-6" :repo "futon3c" :path "holes/missions/M-futon-seams.md"
                    :lifecycle-path "holes/labs/M-futon-seams/lifecycle.edn" :store (str (store-dir)) :id "flight-x"
                    :read-text (fn [_ _ path] (if (str/ends-with? path ".edn") lifecycle-text mission-text))
                    :observe #(checks/decl-present? mission-text (:decl %))
                    :sources {:beta-by-context {:WM {:beta 1}}}})]
    (is (= {:absent :no-selection-record} (:target-selection p)))
    (is (= :chosen (get-in p [:placement :target-source])))))

(deftest caller-inputs-reach-selection-and-real-plan
  (let [store (store-dir)
        inputs (select-keys (oc-test/declared-input-fixture)
                            [:enactment-records :publication-observed :clock-lineage])
        opts (entry-opts store)
        baseline (outer-loop/plan-from-field! opts)
        result (outer-loop/plan-from-field! (merge opts inputs))]
    (doseq [[k value] inputs]
      (is (= value (get-in result [:target-selection :inputs k])))
      (is (= value (get-in result [:plan :target-selection :inputs k])))
      (is (= {:absent :not-supplied} (get-in baseline [:target-selection :inputs k]))))
    (is (= (dissoc (:target-selection baseline) :inputs)
           (dissoc (:target-selection result) :inputs)))
    (is (= (get-in baseline [:plan :requisition]) (get-in result [:plan :requisition])))))

(defn- receipt [record-id target]
  {:record-id record-id :delta 1
   :policy-key [:pattern-cascade target [] {}]})

(deftest caller-supplied-enactment-fold-reaches-target-selection
  (let [store (store-dir)
        fold {:enactment-records
              {[:click-1 :c] (receipt [:click-1 :c] "M-futon-seams")
               [:click-2 :c] (receipt [:click-2 :c] "M-futon-seams")}}
        result (outer-loop/plan-from-field!
                (assoc (entry-opts store) :enactment-fold fold))]
    (is (= :target-habit-prior
           (get-in result [:target-selection :E :basis])))
    (is (= {"M-futon-seams" 2}
           (get-in result [:target-selection :E :counts])))))

(deftest loaded-store-flights-supply-the-enactment-fold
  (let [store (store-dir)
        flights (io/file store "flights")
        _ (.mkdirs flights)
        record {:flight {:enactments
                         [{:increment (receipt [:click-1 :c] "M-futon-seams")}]}}
        _ (spit (io/file flights "flight-one.edn") (pr-str record))
        result (outer-loop/plan-from-field! (entry-opts store))]
    (is (= :target-habit-prior
           (get-in result [:target-selection :E :basis])))
    (is (= {"M-futon-seams" 1}
           (get-in result [:target-selection :E :counts])))))

(deftest nil-store-and-no-caller-fold-retain-uniform-selection
  (let [opts (assoc (entry-opts (store-dir))
                    :load-field-fn
                    (fn [] {:field field
                            :opts {:sources {} :store nil
                                   :code-root "/nonexistent"}}))
        result (with-redefs [fd/plan (fn [_] {:planned true})]
                 (outer-loop/plan-from-field! opts))]
    (is (= {:basis :uniform-no-data}
           (get-in result [:target-selection :E])))))

(deftest missing-chosen-location-is-data-not-an-empty-mission
  (doseq [[f missing] [[(assoc field :considered []) [:considered-entry]]
                       [(assoc-in field [:considered 0 :path] "  ") [:path]]
                       [(assoc-in field [:considered 0 :repo] "") [:repo]]
                       [(update-in field [:considered 0] dissoc :repo :path) [:repo :path]]]]
    (let [called (atom false)
          store (store-dir)
          expected (cascade/select {:field f :seed 42 :trigger :wallclock-cron
                                    :enactment-fold (fold-source/fold-from-flights
                                                     (str (io/file store "flights")))})
          r (with-redefs [fd/plan (fn [_] (reset! called true) :unexpected-plan)]
              (outer-loop/plan-from-field! (entry-opts store {:field f})))]
      (is (= {:absent :chosen-target-not-in-field
              :chosen-target "M-futon-seams" :missing missing} (:plan r)))
      (is (= (:target-selection expected) (:target-selection r)))
      (is (false? @called))
      (println :missing-location r))))
