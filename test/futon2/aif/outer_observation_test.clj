(ns futon2.aif.outer-observation-test
  (:require [futon2.aif.outer-menu-fixture :as menu-fixture]
            [clojure.edn :as edn]
            [futon2.aif.policy-satisfaction :as satisfaction]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-observation :as observation]
            [futon2.aif.focus-receipt :as focus]
            [futon2.aif.outer-select :as select]
            [futon2.aif.outer-state :as state]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.selection-world :as selection-world]
            [wm-run-facts :as run-facts]))

(clojure.test/use-fixtures :each menu-fixture/with-fixture-constructor)

(def makeup {:default-reach 1/2})

(def fixture-authority
  {:loaded-missions
   {:missions (mapv (fn [id]
                      {:id id :path (str "/fixture/" id ".md")
                       :source {:path (str "/fixture/" id ".md") :sha256 "fixture-sha"}
                       :status-class :open :status-line "OPEN"})
                    ["M-ready" "M-cycle" "M-waiting"])}
   :loaded-tickets {:tickets []}
   :loaded-excursions {:excursions []}
   :ticket-targets []
   :excursion-targets []})

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "outer-observation-test"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defonce ^:private giraffe-loader
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn actual-var [symbol]
  ;; The production serving JVM already has this source root.  The isolated
  ;; test process adds it explicitly so these tests exercise Giraffe's own
  ;; verifier without making futon2 depend on futon3c.
  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
    (requiring-resolve symbol)))

(defn fixture-run! []
  (let [root (temp-dir)
        pass {:as-of "2026-10-28"
              :counts {:open 3 :ready 1 :ready-ignoring-externals 1}
              :order [{:id :M-ready :status :ready :rank 1 :reason :order/ready-by-unblocks
                       :unblocks 2 :cone 4 :source-order 0}
                      {:id :M-cycle :status :undetermined :unblocks 0 :cone 1}
                      {:id :M-waiting :status :not-ready :unblocks 0 :cone 0}]
              ;; Closed rows may exist in readiness, but never in :order.
              :readiness {:M-ready {:status :ready}
                          :M-cycle {:status :undetermined}
                          :M-waiting {:status :not-ready}
                          :M-closed {:status :done}}
              :cones {:M-ready [:a :b :c :d] :M-cycle [:x] :M-waiting []}
              :unblocks {:M-ready [:a :b] :M-cycle [] :M-waiting []}
              :cycles [] :anomalies []}
        write-run! (actual-var 'futon3c.giraffe.run/write-run!)
        result (write-run! {:root (.getPath root) :run-id "fixture-run"
                            :git-sha "fixture"
                            :adapter-result {:input {:as-of "2026-10-28"
                                                     :items [] :edges []}
                                             :rejected []}
                            :pass-output (update pass :order #(mapv (fn [row] (assoc row :dependency-reading {:record-pin {:sha256 (identity/sha256 (pr-str row))}})) %))})]
    (io/file (:dir result))))

(defn requisition-fixture-run! []
  (let [root (temp-dir)
        rows (mapv (fn [id]
                     (let [row {:id (keyword id) :status :ready :rank 1
                                :unblocks 0 :cone 0}]
                       (assoc row :dependency-reading
                              {:record-pin {:sha256 (identity/sha256 (pr-str row))}})))
                   ["E-requisition-in-progress" "E-requisition-completed"
                    "E-requisition-other" "E-ordinary" "T-parent-only"])
        write-run! (actual-var 'futon3c.giraffe.run/write-run!)
        result (write-run! {:root (.getPath root) :run-id "requisition-fixture-run"
                            :git-sha "fixture"
                            :adapter-result {:input {:as-of "2026-11-04" :items [] :edges []}
                                             :rejected []}
                            :pass-output {:as-of "2026-11-04" :order rows}})]
    (io/file (:dir result))))

(deftest requisition-records-leave-the-outer-population-before-scoring
  (let [entry (fn [id path requisition]
                {:id id :path path :source {:path path :sha256 "fixture-sha"}
                 :status-class :open :requisition requisition})
        entries [(entry "E-requisition-in-progress" "/fixture/in-progress.md"
                        {:state :in-progress})
                 (entry "E-requisition-completed" "/fixture/completed.md"
                        {:state :completed})
                 (entry "E-requisition-other" "/fixture/other.md"
                        {:malformed "**Requisition:** queued"})
                 (entry "E-ordinary" "/fixture/ordinary.md"
                        {:absent :no-requisition})]
        ticket (assoc (entry "T-parent-only" "/fixture/ticket.md"
                             {:absent :no-requisition})
                      :status-class :live :parent "E-ordinary")
        authority {:loaded-missions {:missions []}
                   :loaded-tickets {:tickets [ticket]}
                   :loaded-excursions {:excursions entries}
                   :ticket-targets ["T-parent-only"]
                   :excursion-targets (mapv :id entries)}
        result (binding [observation/*resolve-var* actual-var]
                 (observation/giraffe-field
                  (requisition-fixture-run!) makeup authority))
        visible (concat (:field result) (:outer-field/excluded result))]
    (is (= ["E-ordinary" "T-parent-only"] (mapv :target (:field result))))
    (is (empty? (:outer-field/excluded result)))
    (is (not-any? #(re-find #"requisition" (:target %)) visible))
    (is (= #{"E-ordinary" "T-parent-only"} (set (keys (:facts result)))))))

(deftest verified-field-carries-open-facts
  (testing "closed items are excluded and Giraffe statuses remain distinct"
    (let [dir (fixture-run!)
          result (binding [observation/*resolve-var* actual-var]
                   (observation/giraffe-field dir makeup fixture-authority))]
      (is (= ["M-ready" "M-cycle" "M-waiting"] (mapv :target (:field result))))
      (is (= [3/4 1/2 1/4] (mapv :reach (:field result))))
      (is (= {:status :ready :unblocks 2 :cone 4 :rank 1 :reach-source :giraffe-status-readiness-prior-v1}
             (select-keys (get-in result [:facts "M-ready"]) [:status :unblocks :cone :rank :reach-source])))
      (is (= :undetermined (get-in result [:facts "M-cycle" :status])))
      (is (= :not-ready (get-in result [:facts "M-waiting" :status])))
      (is (nil? (get-in result [:facts "M-closed"])))
      (is (empty? (:outer-field/excluded result))))))

(deftest unverified-run-refuses
  (let [dir (fixture-run!)
        _ (spit (io/file dir "pass.edn") "{:tampered true}\n")
        error (binding [observation/*resolve-var* actual-var]
                (try (observation/giraffe-field dir makeup)
                     (catch clojure.lang.ExceptionInfo e e)))]
    (is (= :giraffe-run-unverified (:kind (ex-data error))))
    (is (some #(= "pass.edn" (:file %))
              (get-in (ex-data error) [:verification :mismatches])))))

(deftest unavailable-verifier-refuses
  (let [error (binding [observation/*resolve-var* (fn [_] nil)]
                (try (observation/giraffe-field (fixture-run!) makeup)
                     (catch clojure.lang.ExceptionInfo e e)))]
    (is (= :giraffe-verifier-unavailable (:kind (ex-data error))))))

(def fixture-authority-with-tickets
  "One ticket the fixture Giraffe field enumerated (T-in-field) and one that is
  open in the registry but absent from every pass row (T-minted-after-run) --
  the exact shape of run 4955b6e6's missing ticket."
  (-> fixture-authority
      (assoc :ticket-targets ["T-in-field"])
      (assoc-in [:loaded-tickets :tickets]
                [{:id "T-in-field" :path "/fixture/T-in-field.md"
                  :status-class :live :status-line "OPEN" :item-line nil}
                 {:id "T-minted-after-run" :path "/fixture/T-minted-after-run.md"
                  :status-class :live :status-line "OPEN" :item-line nil}])))

(deftest registry-open-ticket-absent-from-field-is-typed-excluded
  (testing "a live ticket the verified field never saw is excluded with a typed
            reason, not silently dropped; no facts are invented for it"
    (let [pass {:as-of "2026-10-28"
                :counts {:open 4 :ready 1 :ready-ignoring-externals 1}
                :order [{:id :M-ready :status :ready :reason :order/ready-by-unblocks
                         :unblocks 2 :cone 4 :source-order 0}
                        {:id :M-cycle :status :undetermined :unblocks 0 :cone 1}
                        {:id :M-waiting :status :not-ready :unblocks 0 :cone 0}
                        {:id :T-in-field :status :undetermined :unblocks 0 :cone 1}]
                :readiness {:M-ready {:status :ready}
                            :M-cycle {:status :undetermined}
                            :M-waiting {:status :not-ready}
                            :T-in-field {:status :undetermined}}
                :cones {:M-ready [:a :b :c :d] :M-cycle [:x] :M-waiting [] :T-in-field [:t]}
                :unblocks {:M-ready [:a :b] :M-cycle [] :M-waiting [] :T-in-field []}
                :cycles [] :anomalies []}
          write-run! (actual-var 'futon3c.giraffe.run/write-run!)
          run-dir (io/file (:dir (write-run! {:root (.getPath (temp-dir))
                                              :run-id "fixture-run-tickets"
                                              :git-sha "fixture"
                                              :adapter-result
                                              {:input {:as-of "2026-10-28"
                                                       :items [] :edges []}
                                               :rejected []}
                                              :pass-output (update pass :order #(mapv (fn [row] (assoc row :dependency-reading {:record-pin {:sha256 (identity/sha256 (pr-str row))}})) %))})))
          result (binding [observation/*resolve-var* actual-var]
                   (observation/giraffe-field run-dir makeup fixture-authority-with-tickets))
          excluded (into {} (map (juxt :target identity))
                         (:outer-field/excluded result))]
      ;; The enumerated ticket behaves as before: in the field, admitted.
      (is (contains? (set (map :target (:field result))) "T-in-field"))
      (is (nil? (get excluded "T-in-field")))
      ;; The unseen open ticket: a typed exclusion, never in the field/facts.
      (is (= :unread-new (:reason (get excluded "T-minted-after-run"))))
      (is (string? (:pin (get excluded "T-minted-after-run"))))
      (is (not (contains? (set (map :target (:field result)))
                          "T-minted-after-run")))
      (is (nil? (get-in result [:facts "T-minted-after-run"]))))))

(deftest registry-open-ticket-exclusion-reaches-selector-population
  (testing "the receiver's population (selection-world Q1) counts the ticket:
            open census N, enumerated N, missing 0"
    (let [outer-selection {:schema :wm/outer-task-selection-v1
                           :outer-field/admitted ["M-ready"]
                           :outer-field/excluded
                           [{:target "T-minted-after-run"
                             :reason :registry-open-not-in-giraffe-field
                             :status-line "OPEN"}]}
          world (selection-world/capture
                 {:controller-ranking []}
                 {"seat-1" {:type "zai"}}
                 nil
                 outer-selection
                 {:code-root "/nonexistent-hermetic"
                  :world-task-sets-fn (constantly
                                       {:missions ["M-ready"]
                                        :excursions []
                                        :tickets ["T-minted-after-run"]})
                  :world-pattern-manifest-fn (constantly {:file-count 0 :digest "x"})
                  :world-now-fn (constantly (java.time.Instant/parse
                                             "2026-10-31T00:00:00Z"))})
          q1 (:q1-diagnostic world)]
      (is (= 2 (:open-task-count q1)))
      (is (= 2 (:enumerated-task-count q1)))
      (is (= 0 (get-in q1 [:open-tasks-missing-from-outer-population :count])))
      (is (empty? (get-in q1 [:open-tasks-missing-from-outer-population :ids]))))))

(deftest unknown-relation-is-not-guessed
  (let [dir (fixture-run!)
        focus-inputs {:windows [] :relations [] :facet-edges []}
        result (binding [observation/*resolve-var* actual-var]
                 (observation/outer-observation
                  {:run-dir dir :makeup makeup :focus-inputs focus-inputs
                   :decision-as-of "2026-10-28T12:00:00Z"}))]
    (is (= :unknown ((:relation result) (:focus result) "M-unresolvable")))
    (is (= :unknown (get-in result [:focus/source :status])))))

(deftest real-giraffe-run-shape
  ;; Retained row: M-futonzero-prelim-practice is :ready at rank 43,
  ;; source-order 14, with dependency record pin 225b1fa5... and 0 reach.
  (let [dir (io/file "/home/joe/code/storage/giraffe/20261101T224600.983Z-6f02dbc0")]
    (is (.isDirectory dir) "the retained Giraffe evidence must be present")
    (let [result (binding [observation/*resolve-var* actual-var]
                   (observation/giraffe-field dir makeup))
          targets (set (map :target (:field result)))
          excluded (some #(when (= "M-repl-wins-over-cli" (:target %)) %)
                         (:outer-field/excluded result))]
      (is (nil? excluded) "inactive is outside the enumerated open set")
      (is (not (contains? targets "M-repl-wins-over-cli")))
      (is (nil? (get-in result [:facts "M-repl-wins-over-cli"])))
      (is (contains? targets "M-futonzero-prelim-practice"))
      (is (some? (get-in result [:facts "M-futonzero-prelim-practice"]))))))

(deftest retained-relation-is-the-scoring-receipt
  (let [calls (atom [])
        receipt {:class :associated :reason :fixture-declared-relation :evidence {:pin "retained"}}
        fact {:status :ready :cone 2 :unblocks 2 :rank 3 :reach-source :default-reach}
        field {:field [{:target "M-ready" :reach 1/2 :reach-source :default-reach}]
               :facts {"M-ready" fact}}
        obs (with-redefs [observation/giraffe-field (fn [& _] field)
                          focus/decision-focus (fn [& _] {:info {:focus "WM"}})
                          focus/classify-target (fn [& args] (swap! calls conj args) receipt)]
              (observation/outer-observation {:run-dir "unused" :makeup makeup :decision-as-of "fixed"}))
        counts {:c {:focused 55 :related 35 :unrelated 5 :stop-the-line 5 :not-yet-evaluated 0}
                :state-counts {:ready {:doable 3 :blocked 1}}
                :gain-counts {:none 1 :one 2 :several 3}}
        r (select/select {:cascades [{:id :one :patterns ["only-what-can-start-now"]
                                      :steps [:ready-gate]}]} counts obs)]
    (is (= 1 (count @calls)))
    (is (identical? receipt (get-in obs [:facts "M-ready" :relation :receipt])))
    (is (= :associated ((:relation obs) "WM" "M-ready")))
    (is (= :associated (get-in r [:cascades 0 :relation :class])))
    (is (= (identity/sha256 (pr-str receipt)) (get-in r [:cascades 0 :relation :receipt-digest])))
    (is (= 3 (get-in r [:cascades 0 :pick-facts :rank])))
    (is (= :default-reach (get-in r [:cascades 0 :reach :source])))
    (is (= 1 (count @calls)))))

(def retained-pass-dir "/home/joe/code/storage/giraffe/20261101T044121.422Z-730407c5")
(def retained-pass-sha "a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91")

(deftest retained-open-field-is-exact-and-policy-independent
  (let [population-text (slurp "holes/labs/wm-contract/witnesses/witnesses-open-tasks-2026-11-01.edn")
        ids (:ids (edn/read-string population-text))
        pass-text (slurp (io/file retained-pass-dir "pass.edn"))
        _ (is (= retained-pass-sha (identity/sha256 pass-text)))
        _ (is (= "e82f6381fa3e056fff332cdb510662469e353fb120cbb7120d1eb2b46f9333d9"
                 (identity/sha256 (pr-str (vec (sort (map str ids)))))))
        ;; Registry snapshot INPUT: exact pinned open IDs, with available source
        ;; declarations. No network registry or live policy store is consulted.
        entries (fn [prefix status]
                  (mapv #(hash-map :id % :status-class status
                                   :path (str "/snapshot/" % ".md")
                                   :source {:path (str "/snapshot/" % ".md")
                                            :sha256 "pinned-snapshot-sha"})
                        (filter #(.startsWith ^String % prefix) ids)))
        authority {:loaded-missions {:missions (entries "M-" :open)}
                   :loaded-excursions {:excursions (entries "E-" :open)}
                   :loaded-tickets {:tickets (entries "T-" :live)}
                   :ticket-targets (vec (filter #(.startsWith ^String % "T-") ids))
                   :excursion-targets (vec (filter #(.startsWith ^String % "E-") ids))}
        verifier (fn [_] (fn [dir]
                          {:ok? (= retained-pass-sha (identity/sha256 (slurp (io/file dir "pass.edn"))))}))
        build (fn [reach]
                (binding [observation/*resolve-var* verifier]
                  (with-redefs [satisfaction/all-satisfied-exclusion
                                (fn [& _] (throw (ex-info "Policy lookup forbidden" {})))]
                    (observation/giraffe-field retained-pass-dir {:default-reach reach} authority))))
        a (build 0) b (build 999)
        field (:field-facts a) admitted (:admitted field) excluded (:excluded field)]
    (is (= a b) "makeup default reach is not read")
    (is (= 626 (count (:enumerated field))))
    (is (= 609 (count admitted)))
    (is (= 17 (count excluded)))
    (is (= (set ids) (set (concat (keys admitted) (map :target excluded)))))
    (is (= #{:unread-new} (set (map :reason excluded))))
    (is (every? #(observation/task-exclusion-reasons (:reason %)) excluded))
    (is (every? (comp seq :pin) excluded))
    (is (contains? admitted "M-futonzero-prelim-practice")
        "ac5bc477 excluded this open task for satisfied inner policies")
    (is (not (contains? (set (map :target excluded)) "E-kimi-task-187")))
    (is (every? (fn [[_ fact]] (and (:pin fact)
                                     (<= 0 (get-in fact [:reach :value]) 1)
                                     (= :prior (get-in fact [:reach :kind]))
                                     (= (:pin fact) (get-in fact [:reach :inputs :pin])))) admitted))
    (is (= 1/2 (get-in admitted ["M-interim-director" :reach :value])))
    (is (= retained-pass-sha (:giraffe/pass-sha256 field)))
    (is (true? (:default-reach-ignored a)))
    (let [kit (state/load-makeup)
          selected (select/select (assoc kit :cascades [{:id :ready-only
                                                         :steps [:ready-gate :by-cone :by-unblocks]}])
                                  (state/init-state kit)
                                  (assoc a :focus "WM" :relation (fn [_ _] :associated)))]
      (is (= field (:field-facts selected)))
      (is (= 609 (count (:outer-score/admitted selected))))
      (is (= :giraffe-status-readiness-prior-v1 (get-in selected [:cascades 0 :reach :source])))
      (is (= 3/4 (get-in selected [:selection :selected :reach]))))))

(deftest task-reach-is-a-pinned-prior-not-run-history
  (doseq [[status expected] [[:ready 3/4] [:undetermined 1/2] [:not-ready 1/4]]]
    (let [row {:status status :dependency-reading {:record-pin {:sha256 "row-pin"}}}
          reach (observation/task-reach row)]
      (is (= expected (:value reach)))
      (is (= reach (observation/task-reach (assoc row :prior-run :never-attempted :policy :ignored))))
      (is (= {:status status :pin "row-pin"} (:inputs reach)))))
  (is (= :status-unavailable (:reason (observation/task-reach {:status :unknown}))))
  (is (= :row-pin-unavailable (:reason (observation/task-reach {:status :ready})))))

(deftest partition-and-exclusion-contract-refuse-invalid-input
  (doseq [row [{:target "M-x" :reason :open :pin "p"}
               {:target "M-x" :reason :all-policies-already-satisfied :pin "p"}
               {:target "M-x" :reason :unread-new :pin nil}]]
    (is (thrown? clojure.lang.ExceptionInfo (observation/validate-exclusion! row))))
  (doseq [[admitted excluded] [[[] []] [["M-x" "M-x"] []]
                               [["M-x"] [{:target "M-x" :reason :unread-new :pin "p"}]]
                               [["M-extra"] []]]]
    (is (thrown? clojure.lang.ExceptionInfo
                 (observation/validate-field! ["M-x"] admitted excluded)))))

(deftest registry-status-is-not-a-source-exclusion-reason
  (let [authority (-> fixture-authority
                      (assoc-in [:loaded-missions :missions 0 :status-class] :active)
                      (assoc-in [:loaded-missions :missions 0 :path] nil)
                      (assoc-in [:loaded-missions :missions 0 :source] {:path nil :sha256 nil})
                      (assoc-in [:loaded-missions :missions 0 :mission/unselectable-reason]
                                :pathless-foreign-entity)
                      (assoc-in [:loaded-missions :missions 1 :status-class] :identify)
                      (assoc-in [:loaded-missions :missions 2 :status-class] :inactive))
        result (binding [observation/*resolve-var* actual-var]
                 (observation/giraffe-field (fixture-run!) makeup authority))
        exclusion (first (:outer-field/excluded result))]
    (is (= ["M-cycle"] (:enumerated-open-tasks result)))
    (is (= ["M-cycle"] (mapv :target (:field result))))
    (is (nil? exclusion))
    (is (= [{:id "M-ready" :status-class :active
             :mission/unselectable-reason :pathless-foreign-entity}
            {:id "M-waiting" :status-class :inactive}]
           (:registry/non-open result)))))


(deftest retained-selector-roster-is-provenance-not-an-independent-census
  (let [authority (-> fixture-authority-with-tickets
                      (assoc :registry-source :substrate)
                      (update-in [:loaded-missions :missions] conj
                                 {:id "M-gated" :status-class :open :operator-gated? true}))
        field (binding [observation/*resolve-var* actual-var]
                (observation/giraffe-field (fixture-run!) makeup authority))
        roster (get-in field [:field-facts :open-tasks])
        groups (mapv roster [:missions :excursions :tickets])
        record {:outer/selection {:field-facts (:field-facts field)}}
        snap {:open-missions #{"M-gated"} :open-excursions #{} :open-tickets #{}}
        projected (run-facts/selection-facts-for-record record "pinned-record" snap {})
        old (run-facts/selection-facts-for-record
             (update-in record [:outer/selection :field-facts] dissoc :open-tasks)
             "pinned-record" snap {})]
    (is (= :substrate (:source roster)))
    (is (= (identity/sha256 (pr-str groups)) (:registry-pin roster)))
    (is (= (set (:enumerated-open-tasks field)) (set (mapcat identity groups))))
    (is (every? #(= % (vec (sort %))) groups))
    (is (not (some #{"M-gated"} (:missions roster))))
    (is (= ["M-gated"] (get-in projected [:facts "openMissions"])))
    (is (= [] (get-in projected [:facts "openTickets"])))
    (is (= (set (mapcat identity groups)) (set (get-in projected [:facts "enumeratedTasks"]))))
    (is (= (:pins snap) (get-in projected [:sources "openMissions"])))
    (is (= ["M-gated"] (get-in old [:facts "openMissions"])))
    (is (= (run-facts/facts-for-record record "pinned-record" snap)
           (run-facts/facts-for-record
            (update-in record [:outer/selection :field-facts] dissoc :open-tasks)
            "pinned-record" snap)))))

(defn completion-fixture-field [surfaces]
  (let [entries (mapv (fn [i]
                        {:id (str "M-completion-" i) :status-class :open :status-line "OPEN"
                         :path (str "/fixture/completion-" i ".md")
                         :source {:path (str "/fixture/completion-" i ".md")
                                  :sha256 "fixture-sha"}})
                      (range (count surfaces)))
        rows (mapv (fn [entry surface]
                     (let [row (cond-> {:id (keyword (:id entry)) :status :ready}
                                 surface (assoc :completion surface))]
                       (assoc row :dependency-reading
                              {:record-pin {:sha256 (identity/sha256 (pr-str row))}})))
                   entries surfaces)
        result ((actual-var 'futon3c.giraffe.run/write-run!)
                {:root (.getPath (temp-dir)) :run-id "completion-fixture"
                 :git-sha "fixture"
                 :adapter-result {:input {:as-of "2026-11-04" :items [] :edges []}
                                  :rejected []}
                 :pass-output {:as-of "2026-11-04" :order rows}})
        authority (assoc fixture-authority :loaded-missions {:missions entries})]
    (binding [observation/*resolve-var* actual-var]
      (observation/giraffe-field (:dir result) makeup authority))))

(deftest undefined-completion-v2-is-excluded-before-selection
  (let [undefined {:schema :wm/completion-surface-v2
                   :completion-class :completion-definition-not-found}
        defined {:schema :wm/completion-surface-v2 :completion-class :criteria-locators}
        result (completion-fixture-field [undefined defined])
        exclusion (first (:outer-field/excluded result))]
    (is (= ["M-completion-1"] (mapv :target (:field result))))
    (is (= 1 (count (:outer-field/excluded result))))
    (is (= "M-completion-0" (:target exclusion)))
    (is (= :completion-definition-not-found (:reason exclusion)))
    (is (= {:completion undefined :switch-mode :exclude
            :switch-sha256 (identity/sha256 (slurp (io/resource "wm/outer-admission.edn")))}
           (:evidence exclusion)))
    (is (= exclusion (observation/validate-exclusion! exclusion)))
    (is (observation/validate-field! (:enumerated-open-tasks result)
                                    (mapv :target (:field result))
                                    (:outer-field/excluded result)))))

(deftest completion-derivation-mode-refuses-unbuilt
  (let [config (io/file (temp-dir) "outer-admission.edn")]
    (spit config (pr-str {:completion-undefined {:mode :admit-with-derivation}}))
    (binding [observation/*admission-config-path* config]
      (is (= :completion-derivation-not-built
             (try (completion-fixture-field [nil])
                  :unexpected-admission
                  (catch clojure.lang.ExceptionInfo e (:kind (ex-data e)))))))))

(deftest missing-completion-remains-admitted
  (let [result (completion-fixture-field [nil])]
    (is (= ["M-completion-0"] (mapv :target (:field result))))
    (is (empty? (:outer-field/excluded result)))))

(deftest v1-completion-remains-admitted
  (let [result (completion-fixture-field
                [{:schema :wm/completion-surface-v1
                  :completion-class :completion-definition-not-found}])]
    (is (= ["M-completion-0"] (mapv :target (:field result))))
    (is (empty? (:outer-field/excluded result)))))
(deftest admitted-mission-shas-come-from-the-field-authority-snapshot
  (let [extra {:id "M-unread" :path "/fixture/M-unread.md"
               :source {:path "/fixture/M-unread.md" :sha256 "excluded-sha"}
               :status-class :open :status-line "OPEN"}
        authority (update-in fixture-authority [:loaded-missions :missions] conj extra)
        result (binding [observation/*resolve-var* actual-var]
                 (observation/giraffe-field (fixture-run!) makeup authority))]
    (is (= "fixture-sha"
           (get-in result [:mission-source-sha256-by-target "M-ready"])))
    (is (not (contains? (:mission-source-sha256-by-target result) "M-unread")))))
