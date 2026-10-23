(ns futon2.aif.wm.family-selection
  "Score target policy families jointly through the existing cascade scorer
  and selector. Failed families remain counted evidence and never contribute a
  placeholder policy. Habit E is attached by the selector from the enactment
  fold under each policy's structural reading-policy key (cascade-habit-store
  policy-view); a ranked entry carries no E of its own before that."
  (:require [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.mission-hole-wants :as mission-wants]
            [futon2.aif.mission-registry :as mission-registry]
            [futon2.aif.wm.family-canonical-g :as canonical-g]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.policy :as policy]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.occurrence-square-registry :as square-registry]
            [futon2.aif.reading-interpretation-debt :as debt]
            [futon2.aif.wm.selection-cascade-identity :as cascade-identity]
            [futon2.aif.target-policy-family :as target-family]
            [futon2.aif.target-reading-registry :as reading-registry]))

(identity/register! *ns* *file*)

(def kernel-version
  "The co-application transition kernel the ranked reading policies carry,
  and the kernel the decision's precision identity binds."
  :co-application-frontier-theta-v1)

(defn- annotate-failure [target failure]
  (assoc failure :target-id (or (:target-id failure) target)))

(defn- reading-action
  "The scored policy's action, carrying its history key and the exact
  interpretation it owes, both bound to its identity."
  [policy score]
  (let [action (:candidate score)
        action (assoc action :policy-key (prefix-admission/reading-policy-key action))]
    (assoc action :interpretation-debt
           (debt/debt action (:fit score) (:reading-pin policy)))))

(defn- scored->ranked [policy score rank]
  (let [entry (first (:scorer-result score))
        computed-f (:computed-f score)
        g-terms {:risk (:risk score)
                 :ambiguity (:ambiguity score)
                 :expected-information-gain (:information-gain score)
                 :combination :risk-plus-ambiguity-minus-information-gain
                 :units :nats}
        certificate (-> (:certificate entry)
                        (assoc :f computed-f)
                        (assoc :class-emission (:class-emission score))
                        (assoc :g-terms (merge (get-in entry [:certificate :g-terms])
                                              g-terms)))]
    (cond-> (-> entry
        (assoc :action (reading-action policy score)
               :reading-pin (:reading-pin policy)
               :cascade-id (:policy-id policy)
               :policy-id (:policy-id policy)
               :policy-kind (:kind policy)
               :target-id (:target policy)
               :rank rank
               :controller-score (:g score)
               :G-efe (:g score)
               :G-cascade (:g score)
               :f (:f score)
               :f-status (:f-status score)
               :computed-f computed-f
               :class-emission (:class-emission score)
               :habit 1
               :habit-provenance {:source :pending-selector-attachment}
               :transition-kernel kernel-version
               :certificate certificate))
      (= :canonical-coapply (:g-law score))
      (assoc :g-law :canonical-coapply :legacy-g (:legacy-g score)
             :horizon-choice (:horizon-choice score)))))

(defn constructed-key
  "The structural key of a family policy as constructed: the scored
  candidate's, else (SCORE nil) the key of its arranged candidate; a policy
  whose arrangement cannot be compiled is named by target and policy id."
  [policy score]
  (or (some-> (:candidate score) prefix-admission/reading-policy-key)
      (try (prefix-admission/reading-policy-key
            (shape-g/arranged->candidate (:target policy) (:policy-id policy)
                                         (:cascade policy)))
           (catch Exception _ nil))
      [:wm/reading-policy-unkeyed (str (:target policy)) (:policy-id policy)]))

(defn- policy-summary [policy score]
  {:policy-id (:policy-id policy)
   ;; recorded apart from the action, so enactment can check the action's
   ;; debt carrier against what the decision saw
   :interpretation-owed (get-in score [:fit :interpretation-owed])
   :interpretation-debt-digest (:digest (:interpretation-debt (reading-action policy score)))
   :kind (:kind policy)
   :F (:f score)
   :G (:g score)
   :g-terms {:risk (:risk score)
             :ambiguity (:ambiguity score)
             :information-gain (:information-gain score)
             :combination :risk-plus-ambiguity-minus-information-gain}
   :kernel kernel-version
   :habit {:status :attached-by-selector
           :key (get-in score [:candidate :id])}})

(defn- failed-field-family [target failure]
  {:status :failed :target-id target :policies []
   :reported-count 0 :distinct-count 0
   :failures [(assoc failure :target-id target)] :failure-count 1})

(defn- family-coverage-kind [family]
  (let [kinds (set (map :kind (:failures family)))]
    (cond
      (kinds :graph-path-not-supplied) :graph-refused
      (kinds :graph-pin-mismatch) :graph-refused
      (kinds :graph-pin-missing) :graph-refused
      (kinds :graph-unreadable) :graph-refused
      (kinds :graph-without-pattern-ids) :graph-refused
      (kinds :graph-endpoint-outside-pattern-ids) :graph-refused
      (kinds :target-source-path-absent) :source-path-absent
      (kinds :target-source-row-absent) :source-path-absent
      (kinds :target-item-line-absent) :source-path-absent
      (kinds :target-source-conflict) :source-path-absent
      (kinds :target-source-kind-unsupported) :source-kind-unsupported
      (kinds :target-head-template-only) :head-template-only
      (kinds :target-source-unreadable) :source-unreadable
      (kinds :stale-target-reading) :stale
      (kinds :no-current-target-reading) :absent
      :else :current)))

(defn- field-coverage [families]
  (let [counts (frequencies (map family-coverage-kind families))]
    {:targets (count families)
     :current (get counts :current 0)
     :stale (get counts :stale 0)
     :absent (get counts :absent 0)
     :source-path-absent (get counts :source-path-absent 0)
     :source-unreadable (get counts :source-unreadable 0)
     :source-kind-unsupported (get counts :source-kind-unsupported 0)
     :head-template-only (get counts :head-template-only 0)
     :graph-refused (get counts :graph-refused 0)}))

(defn families-for-field
  "Form one policy-family result per target source row, in field order.
  The pinned graph is loaded once before any source or reading lookup; with
  no graph path every target fails :graph-path-not-supplied."
  [{:keys [target-sources reading-root graph-path retraction
           occurrence-square-root pattern-texts pattern-library-root
           require-occurrence-squares?]}]
  (let [loaded (if (some? graph-path)
                 (graph-pin/load-pinned graph-path)
                 {:status :refused :kind :graph-path-not-supplied})
        graph-ok? (= :loaded (:status loaded))
        graph-summary (if graph-ok?
                        (select-keys loaded [:status :pin])
                        (dissoc loaded :graph :pin))
        graph-failure (dissoc loaded :status :graph :pin)
        families
        (mapv
         (fn [{:keys [target-id source-kind source-path source-absent]}]
           (cond
             (not graph-ok?)
             (failed-field-family target-id graph-failure)

             source-absent
             (failed-field-family target-id {:kind source-absent})

             (not (contains? #{nil :head} source-kind))
             (failed-field-family target-id {:kind :target-source-kind-unsupported
                                             :source-kind source-kind})

             :else
             (try
               (let [digest (reading-registry/excerpt-digest source-path)]
                 (if (map? digest)
                   (failed-field-family target-id (dissoc digest :status))
                   (let [reading (reading-registry/current-reading
                                  (or reading-root reading-registry/default-root) target-id digest)
                         squares (when-not (= :absent (:status reading))
                                   (square-registry/current
                                    (or occurrence-square-root square-registry/default-root) target-id
                                    (:analysis-digest reading)
                                    (or pattern-texts
                                        (square-registry/library-resolver
                                         (or pattern-library-root
                                             square-registry/default-library-root)))))]
                     (if (and require-occurrence-squares?
                              (= :absent (:status squares)))
                       (failed-field-family
                        target-id (merge (dissoc squares :status)
                                         {:excerpt-digest (:excerpt-digest reading)
                                          :analysis-digest (:analysis-digest reading)}))
                       (target-family/policy-family
                        {:reading reading :graph (:graph loaded)
                         :occurrence-squares (or squares {:entries {}})
                         :retraction retraction})))))
               (catch Exception e
                 (failed-field-family
                  target-id {:kind :target-source-unreadable
                             :source-path source-path
                             :message (ex-message e)})))))
         target-sources)]
    {:graph graph-summary
     :families families
     :coverage (field-coverage families)}))

(defn full-eligible-target-field
  "Construct the one complete, source-pinned pre-choice policy field.
  TASKS is the already-filtered ordinary outer frontier. This pure boundary
  performs no authoring or dispatch; it only delegates to families-for-field."
  [tasks {:keys [target-source-declarations target-reading-root pattern-graph-path occurrence-square-root
                 occurrence-square-pattern-texts pattern-library-root
                 require-occurrence-squares? target-policy-retraction] :as config}]
  (let [declarations
        (or target-source-declarations
            (mapv (fn [{:keys [id source]}]
                (if-let [path (:path source)]
                  {:target-id id :source-kind :head :source-path path
                   :source-pin source}
                  {:target-id id :source-kind :head :source-path nil
                   :source-absent :target-source-path-absent}))
                  tasks))
        field (families-for-field
               {:target-sources declarations
                :reading-root target-reading-root
                :graph-path pattern-graph-path
                :occurrence-square-root occurrence-square-root
                :pattern-texts occurrence-square-pattern-texts
                :pattern-library-root pattern-library-root
                :require-occurrence-squares? require-occurrence-squares?
                :retraction target-policy-retraction})
        config-view (select-keys config
                                 [:target-reading-root :pattern-graph-path
                                  :occurrence-square-root :pattern-library-root
                                  :require-occurrence-squares?
                                  :target-policy-retraction])
        body {:schema :wm/pre-choice-policy-field-v1
              :configuration config-view
              :target-sources declarations
              :graph (:graph field)
              :coverage (:coverage field)
              :families (:families field)}]
    (assoc body :source-pin
           {:path "wm://pre-choice-policy-field-v1"
            :sha256 (identity/sha256 (.getBytes (pr-str body) "UTF-8"))})))

(defn field-census
  "The recordable census of FIELD (families-for-field's result): the graph pin
  or its refusal, the coverage counts, and per target its status, counts,
  stable policy ids and kinds, every failure and the family provenance.
  Policy bodies stay in FIELD; nothing here is scored or selected."
  [field]
  {:schema :wm/target-policy-field-census-v1
   :graph (:graph field)
   :coverage (:coverage field)
   :families (mapv (fn [family]
                     (cond-> (select-keys family [:target-id :status :reported-count
                                                  :distinct-count :failures
                                                  :failure-count])
                       true (assoc :policies (mapv #(select-keys % [:policy-id :kind])
                                                   (:policies family)))
                       (:provenance family)
                       (assoc :provenance (:provenance family))))
                   (:families field))})

(defn score-family-field
  "Pure scoring stage.  It preserves one row per target and every typed
  construction/scoring failure; it never selects or certifies."
  ([families] (score-family-field families nil))
  ([families target-classes]
  (mapv
   (fn [family]
     (let [target (:target-id family)
           target-class (get target-classes target)
           ready? (= :computed (:status family))
           reading-pin (select-keys (:provenance family)
                                    [:analysis-digest :excerpt-digest])
           attempted (if ready?
                       (mapv (fn [p]
                               (let [prepared (cond-> (assoc p :reading-pin reading-pin
                                                               :require-target-class? (some? target-classes))
                                                target-class (assoc :target-class target-class))]
                                 [prepared (shape-g/score-policy prepared)]))
                             (:policies family)) [])
           scored (filterv #(= :computed (:status (second %))) attempted)
           score-failures
           (mapv (fn [[p score]]
                   (annotate-failure
                    target (merge {:kind (or (:kind score) :policy-scoring-refused)
                                   :policy-id (:policy-id p)}
                                  (select-keys score [:cycle :reason :units :roots
                                                      :bound :limit]))))
                 (remove #(= :computed (:status (second %))) attempted))
           failures (into (mapv #(annotate-failure target %) (:failures family))
                          score-failures)]
       {:target-id target :distinct-count (:distinct-count family)
        :constructed-keys (mapv (fn [[p score]] (constructed-key p score)) attempted)
        :scored scored
        :policies (mapv (fn [[p score]] (policy-summary p score)) scored)
        :failures failures :failure-count (count failures)}))
   families)))

(defn construct-ranked
  "Construct occurrence-grain ranked carriers without selecting."
  [rows]
  (let [scored (vec (mapcat :scored rows))]
    {:scored scored
     :ranked (mapv (fn [rank [p score]] (scored->ranked p score rank))
                   (range 1 (inc (count scored))) scored)}))

(def sealed-menu-body-keys
  [:schema :field-source-pin :family-census :frontier :focus-receipt :class-census
   :entries :refusals :failures :canonical-g-shadow])

(defn- class-census [target-classes]
  {:by-class (frequencies (keep :class (vals target-classes)))
   :unknown-by-reason (frequencies (keep (fn [[_ c]]
                                          (when (= :unknown (:class c)) (:reason c)))
                                        target-classes))})

(defn measured-frontier [field]
  (let [families (:families field)
        assessed (filterv #(= :computed (:status %)) families)
        absent (filterv #(some (fn [f] (contains? #{:no-current-target-reading
                                                   :stale-target-reading} (:kind f)))
                               (:failures %)) families)]
    {:eligible (count families) :assessed (count assessed) :reading-absent (count absent)
     :eligible-ids (mapv :target-id families)
     :assessed-ids (mapv :target-id assessed)
     :reading-absent-ids (mapv :target-id absent)}))

(defn sealed-menu-pin
  "Canonical seal of the exact pre-choice body. Public so every consumer
  verifies the producer's serialization rather than reproducing it."
  [carrier]
  {:path "wm://sealed-outer-policy-menu-v1"
   :sha256 (identity/sha256
            (.getBytes (pr-str (select-keys carrier sealed-menu-body-keys)) "UTF-8"))})

(defn verify-sealed-menu
  [carrier]
  (let [ids (mapv :policy-id (:entries carrier))
        digests (mapv :cascade-digest (:entries carrier))]
    (cond
      (not= :wm/sealed-outer-policy-menu-v1 (:schema carrier)) :sealed-menu-schema
      (not= :sealed (:status carrier)) :sealed-menu-status
      (not= (sealed-menu-pin carrier) (:menu-pin carrier)) :menu-pin-mismatch
      (not= (count ids) (count (distinct ids))) :duplicate-policy-id
      (not= (count digests) (count (distinct digests))) :duplicate-cascade-digest
      :else nil)))

(defn canonical-shadow
  "Diagnostic only: rescore the exact pre-choice rows without replacing them."
  [rows]
  (let [result (canonical-g/rescore rows)]
    {:role :diagnostic-only
     :consumed-by-selection false
     :comparison (:comparison result)
     :entries (mapv (fn [[p s]]
                      (assoc (select-keys s [:g :risk :ambiguity :information-gain :horizon])
                             :target-id (:target p) :policy-id (:policy-id p)
                             :rank (get-in s [:scorer-result 0 :rank])))
                    (mapcat :scored (:rows result)))
     :refusals (vec (filter #(= :canonical-g (:stage %))
                            (mapcat :failures (:rows result))))
     :refusal (:refusal result)}))

(defn- declared-mission-wants [target declarations]
  (let [rows (filterv #(= target (:target-id %)) declarations)
        row (first rows)
        pin (:source-pin row)
        path (:source-path row)
        source (when (and (= 1 (count rows)) (string? path)
                          (string? (:sha256 pin)))
                 (mission-wants/mission-source mission-registry/default-code-root
                                               {:id target :path path}))
        reason (cond
                 (not= 1 (count rows)) :target-source-pin-unavailable
                 (not (and (string? path) (string? (:sha256 pin)))) :target-source-pin-unavailable
                 (nil? source) :mission-wants-unavailable
                 (not= (:sha256 pin) (get-in source [:source :sha256])) :target-source-sha-mismatch)]
    (if reason
      {:declared-wants {:status :unavailable :reason reason :target-source-pin pin
                        :observed-source (:source source)}}
      {:want (:want source)
       :observation-locators (into {} (map (fn [[token locator]] [[target token] locator]))
                                   (:locators source))
       :declared-wants {:status :available :source (:source source)
                        :universe (:universe source)
                        :target-source-pin pin :holes (:holes source)
                        :criteria (:criteria source) :unlocated (:unlocated source)}})))

(defn- attach-declared-wants
  "Read each target once before sealing. Only a matching mission byte pin
  authorizes wants; scorer tokens and downstream rereads are not sources."
  [ranked field]
  (let [by-target (into {} (map (fn [target]
                                [target (declared-mission-wants target (:target-sources field))]))
                        (distinct (map #(get-in % [:action :target]) ranked)))]
    (mapv (fn [entry]
            (update entry :action
                    #(merge (dissoc % :want :observation-locators :declared-wants)
                            (get by-target (:target %))))) ranked)))

(defn sealed-pre-choice-menu
  "Atomically form and canonical-bind a complete pre-choice policy menu.
  FIELD is the pinned full-field carrier.  JOIN supplies only existing
  occurrence readings and explicitly authored executable additions."
  [field join]
  (let [families (:families field)
        target-classes (:target-classes join)
        failures (vec (mapcat #(map (partial annotate-failure (:target-id %))
                                    (:failures %)) families))
        rows (score-family-field (filterv #(= :computed (:status %)) families)
                                     target-classes)
            {:keys [ranked]} (construct-ranked rows)
            bound (cascade-identity/bind-ranked (attach-declared-wants ranked field)
                                                (or (:readings join) (constantly {}))
                                                (:authored join))
            refusals (vec (:refusals bound))
            entries (vec (:ranked bound))
            body {:schema :wm/sealed-outer-policy-menu-v1
                  :field-source-pin (:source-pin field)
                  :family-census (field-census field)
                  :frontier (measured-frontier field)
                  :focus-receipt (:focus-receipt join)
                  :class-census (class-census target-classes)
                  :entries entries :refusals refusals
                  :failures (vec (concat failures (mapcat :failures rows) refusals))}
            body (cond-> body
                   (= :canonical-coapply (:g-law join))
                   (assoc :canonical-g-shadow (canonical-shadow rows)))]
        (cond
          (and (empty? entries) (seq refusals))
          (assoc body :status :refused :reason :canonical-binding-refused
                 :entries [])

          (empty? entries)
          (assoc body :status :refused :reason :no-canonically-bound-policy)

          :else
          (assoc body :status :sealed :menu-pin (sealed-menu-pin body)))))

(defn select-over-families
  "Score every policy in each computed family, then call
  policy/select-action-cascades once over their union. Failed families add
  counted failures and no candidates. With no computed policies the result is
  a typed abstention and the selector is not called on an empty action set.
  OPTS :attach-f, when given, receives the ranked union and returns it with
  each entry's F carrier (:f-prefix) attached, before the one selector call;
  it is removed from the options the selector sees.
  OPTS :certify, when given, receives the ranked union and the scored
  [policy score] pairs after F is attached and before the selector. It
  returns {:status :certified :beta b :receipt r} (the beta the selector
  uses, and what to record) or a typed refusal {:status :refused …}; a
  refusal returns {:status :refused} without calling the selector.
  OPTS :exclude, when given, receives the certified ranked union, the
  selector's options and the certification, after certification and before
  the selector. It returns {:carrier c :ranked kept} (the menu the selector
  sees, recorded with C) or adds :refusal when nothing is left, which
  returns {:status :repeat-refused} without calling the selector."
  [families {:keys [attach-f certify exclude] :as opts}]
  (let [coverage-failures (vec (mapcat :failures
                                       (remove #(= :computed (:status %)) families)))
        missing-targets (vec (map :target-id
                                  (remove #(= :computed (:status %)) families)))
        rows (score-family-field families)
        canonical (when (= :canonical-coapply (:g-law opts)) (canonical-g/rescore rows))
        rows (if canonical
               (mapv (fn [row] (assoc row :policies (mapv (fn [[p s]] (policy-summary p s)) (:scored row))))
                     (:rows canonical)) rows)
        {:keys [scored ranked]} (construct-ranked rows)
        ranked (if canonical
                 (let [ordered (sort-by :G-efe ranked)
                       ranks (zipmap (distinct (map :G-efe ordered)) (iterate inc 1))]
                   (mapv #(assoc % :rank (get ranks (:G-efe %))) ordered)) ranked)
        ;; Packet B pre-selector join: bind each ranked entry to the
        ;; canonical :wm/executable-cascade-v1 identity.  Entries whose
        ;; canonical construction or validation refuses are removed with a
        ;; typed refusal; the selector never sees an unbound or substituted
        ;; chain.  Absent the option, selection is unchanged.
        bound (when-let [join (:bind-cascade-identity opts)]
                (cascade-identity/bind-ranked ranked
                                               (or (:readings join)
                                                   (constantly {}))
                                               (:authored join)))
        ranked (if bound (:ranked bound) ranked)
        ranked (if attach-f (attach-f ranked) ranked)
        certified (when (and certify (seq ranked)) (certify ranked scored))
        opts (cond-> (dissoc opts :attach-f :certify :exclude
                             :bind-cascade-identity :g-law)
               (= :certified (:status certified)) (assoc :beta (:beta certified)))
        excluded (when (and exclude (= :certified (:status certified)))
                   (exclude ranked opts certified))
        menu (if excluded (:ranked excluded) ranked)
        failures (vec (concat (mapcat :failures rows)
                              (map #(assoc % :stage (:stage % :pre-selector))
                                   (:refusals bound))))
        ;; Requirements Q8: every family policy's structural key at each
        ;; stage, by target; admitted only when certification passed
        stages (into {}
                     (map (fn [row]
                            [(:target-id row)
                             (cond-> {:constructed (vec (:constructed-keys row))
                                      :scored (mapv (fn [[_ score]]
                                                      (prefix-admission/reading-policy-key
                                                       (:candidate score)))
                                                    (:scored row))}
                               (= :certified (:status certified))
                               (assoc :admitted
                                      (vec (keep #(when (= (:target-id row) (:target-id %))
                                                    (get-in % [:action :policy-key]))
                                                 ranked))))]))
                     rows)
        summaries (mapv (fn [row]
                          (let [row (dissoc row :scored :constructed-keys)]
                            (if bound
                              (update row :policies
                                      (fn [ps]
                                        (mapv (fn [s]
                                                (cond-> s
                                                  (get (:digests bound) (:policy-id s))
                                                  (assoc :cascade-digest
                                                         (get (:digests bound)
                                                              (:policy-id s)))
                                                  (some #(= (:policy-id %) (:policy-id s))
                                                        (:refusals bound))
                                                  (assoc :cascade-refused true)))
                                              ps)))
                              row)))
                        rows)]
    (cond->
     (cond
      (:refusal canonical)
      {:status :abstained :decision {:status :abstained :kind :canonical-g-refused
                                     :refusal (:refusal canonical)}
       :ranked [] :target-policy-families summaries :failures failures
       :failure-count (count failures)}

      (seq missing-targets)
      {:status :abstained
       :decision {:status :abstained
                  :kind :incomplete-policy-menu-coverage
                  :missing-targets missing-targets
                  :absent-terms (vec (distinct (map :kind coverage-failures)))}
       :ranked []
       :target-policy-families summaries
       :failures failures
       :failure-count (count failures)}

      (:refusal excluded)
      {:status :repeat-refused
       :refusal (:refusal excluded)
       :certification certified
       :ranked ranked
       :target-policy-families summaries
       :failures failures
       :failure-count (count failures)}

      (= :refused (:status certified))
      {:status :refused
       :refusal certified
       :ranked ranked
       :target-policy-families summaries
       :failures failures
       :failure-count (count failures)}

      (seq ranked)
      (let [decision (policy/select-action-cascades menu opts)
            selected-bound-entry
            (when bound
              (let [d (get-in decision [:action :cascade-digest])]
                (first (filter #(= d (:cascade-digest %)) menu))))
            ;; The post-selection seam must consume the exact canonical
            ;; record that was compared.  Rebuilding it later from a reduced
            ;; action/receipt map creates a different (correctly rejected)
            ;; identity even when the policy itself is unchanged.
            decision (cond-> decision
                       selected-bound-entry
                       (assoc-in [:selection-certificate
                                  :selected-canonical-cascade]
                                 (:canonical-cascade selected-bound-entry)))
            identity-check (when bound
                             (cascade-identity/verify-selected-identity
                              decision menu))]
        (if (and bound (not= :verified (:status identity-check)))
          {:status :refused
           :refusal {:kind :cascade-identity-mismatch
                     :stage :post-selector-identity-check
                     :identity-check identity-check}
           :certification certified
           :q6 (:carrier excluded)
           :policy-stages stages
           :decision decision
           :ranked menu
           :target-policy-families summaries
           :failures failures
           :failure-count (count failures)}
          {:status :selected
           :certification certified
           :q6 (:carrier excluded)
           :policy-stages stages
           :cascade-identity identity-check
           :decision decision
           :ranked menu
           :target-policy-families summaries
           :failures failures
           :failure-count (count failures)}))

      :else
      {:status :abstained
       :decision {:status :abstained :kind :no-computed-policy-family}
       :ranked []
       :target-policy-families summaries
       :failures failures
       :failure-count (count failures)})
     canonical (assoc :canonical-g (:comparison canonical)))))
