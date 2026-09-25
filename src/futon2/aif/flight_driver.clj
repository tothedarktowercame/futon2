(ns futon2.aif.flight-driver
  "One command for a flight: print the plan, and only with --run fly it.

    clojure -M -m futon2.aif.flight-driver M-futon-seams --seat kimi-6 \\
      --repo futon3c --path holes/missions/M-futon-seams.md \\
      --lifecycle-path holes/labs/M-futon-seams/lifecycle.edn [--max-clicks 4] [--run]

  The spike, the first flight of M-autoclock-in (M-wm-wiring), with every
  wired step given:

    clojure -M -m futon2.aif.flight-driver M-autoclock-in --seat <seat> \\
      --repo futon3c --path holes/missions/M-autoclock-in.md \\
      --checker /home/joe/code/futon3c/holes/labs/M-futon-seams/exemplar/proof2a_check.clj \\
      --bb bb --library-root /home/joe/code/futon3/library --max-clicks 1 [--run]

  Also --field-entry <edn> (the target field's entry for the target) and
  --cascades <dir> (the target's cascades, for the read step's served-by
  reading). Each flag not given is a typed absence on the plan's
  :resolved-steps. The store is wi/default-store, under futon2's data/: a
  --run writes the flight record, the readings, the requests and any
  enactment record there.

  Without --run nothing is sent and nothing is written: the plan is the
  record Joe authorizes against. With --run, ONE flight: the ask step before
  each click (D11, answering seat named here), clicks as ordinary clicks
  through POST /api/alpha/wm/click (budget and cast-seat preflight apply),
  and a flight record written under the store, whose path is printed with
  every request, answer job, publication and click."
  (:require [clojure.edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.mission-reading :as reading]
            [futon2.aif.want-interpretation :as wi]
            [futon2.report.war-machine :as wm])
  (:import [java.util UUID])
  (:gen-class))

(def refused-seats
  "Seats that must never answer a flight's requests: kimi-1 is claude-1's
  turn-analysis delegate."
  #{"kimi-1"})

(defn parse-args [args]
  (loop [xs args out {}]
    (if-let [[x & more] (seq xs)]
      (cond
        (= "--run" x) (recur more (assoc out :run? true))
        (= "--read" x) (recur more (assoc out :read? true))
        (str/starts-with? x "--") (let [[v & rest] more]
                                    (when-not v (throw (ex-info (str x " needs a value") {:arg x})))
                                    (recur rest (assoc out (keyword (subs x 2)) v)))
        :else (recur more (assoc out :target x)))
      out)))

(defn resolve-target
  "Which target this flight flies, and how it was placed (M-wm-wiring step 7,
  the flight entry's read). :chosen-target, the outer cascade's choice (not
  written by anything yet), wins: {:target t :target-source :chosen
  :draw-seed seed-or-{:absent :no-draw-seed}}, and a --target given as well
  is recorded as {:hand-target-overridden t}, not dropped. Otherwise the
  --target: {:target t :target-source :hand-placed}. A :field-entry (the
  target field's entry for the target) is carried beside it as given:
  eligibility is the field's, not filtered here. Neither is the driver's
  missing-target refusal, the same ex-info check-args! has always thrown
  ({:missing :target}), never a nil target."
  [{:keys [target chosen-target draw-seed field-entry]}]
  (let [given? #(and (string? %) (not (str/blank? %)))]
    (cond-> (cond
              (given? chosen-target)
              (cond-> {:target chosen-target :target-source :chosen
                       :draw-seed (if (some? draw-seed) draw-seed {:absent :no-draw-seed})}
                (given? target) (assoc :hand-target-overridden target))
              (given? target)
              {:target target :target-source :hand-placed}
              :else
              (throw (ex-info "missing target" {:missing :target})))
      field-entry (assoc :field-entry field-entry))))

(defn- check-args! [{:keys [seat repo path] :as opts}]
  (resolve-target opts)
  (doseq [[k v] {:seat seat :repo repo :path path}]
    (when (str/blank? v) (throw (ex-info (str "missing " (name k)) {:missing k}))))
  (when (refused-seats seat)
    (throw (ex-info (str seat " may not answer flight requests (claude-1's delegate)")
                    {:refused-seat seat}))))

(defn- flight-for [{:keys [repo path lifecycle-repo lifecycle-path read-text code-root observe id store] :as opts}]
  (flight/start (let [r (resolve-target opts)]
                  (assoc r :chosen-because {:kind :requested :by "flight-driver"}))
                (cond-> {:kind :a-exits :repo repo :path path :store (or store wi/default-store)}
                  lifecycle-path (assoc :lifecycle {:repo (or lifecycle-repo repo) :path lifecycle-path})
                  read-text (assoc :read-text read-text)
                  code-root (assoc :code-root code-root)
                  observe (assoc :observe observe))
                {:id id}))

(defn resolved-steps
  "What a real run would use for each wired step (M-wm-wiring WM-DRIVER-I),
  from the parsed OPTS; a flag not given is a typed absence, never a default
  standing in for it."
  [{:keys [checker bb library-root field-entry cascades]}]
  {:checker (or checker {:absent :no-wc-checker-configured})
   :bb (or bb {:absent :not-given :runs "bb"})
   :library-root (or library-root {:absent :not-in-flight-opts})
   :field-entry (or field-entry {:absent :no-field-entry})
   :cascades (or cascades {:absent :no-cascades-dir})
   :quotes {:absent :no-quotes}
   :dispatch-step {:absent :no-dispatch-configured}
   :enact "flight-runner/enact-fn over the click's run record (observe-publication-fn inside it)"
   :wc "flight-runner/wc-verdict-fn with the checker and bb"})

(defn plan
  "The flight the driver would fly, as data. OPTS: parsed args plus
  :sources (the tick's declared sources) and, for tests, :read-text,
  :observe and :id."
  [{:keys [seat store max-clicks sources id] :as opts}]
  (let [target (:target (resolve-target opts))
        store (or store wi/default-store)
        id (or id (str "flight-" (subs (str (UUID/randomUUID)) 0 8)))
        f (flight-for (assoc opts :id id))
        cw (flight/click-wants f sources)
        src (:source cw)
        view (fr/target-view store f cw sources)
        universe (get-in view [:universes target])
        unproduced (wi/unproduced-wants (:wants cw) universe (get-in view [:interpretations target :patterns]))
        horizon (wm/resolve-cascade-horizon view [target])]
    {:flight-id id
     :requisition target
     :answering-seat seat
     :answer-path "Agency bell (mode work, requisition = target), poll to terminal, reply grammar :wm/want-interpretation-response-v1"
     :want-source (select-keys src [:kind :repo :path :lifecycle :text-read?])
     :wants {:in-view (vec (for [t (:wants cw)]
                             (let [c (get-in src [:criteria-by-token t])]
                               {:token t :phase (first (str/split (str (:phase c)) #" "))
                                :met? (get universe t)})))
             :out-of-view (:out-of-view src)
             :unlocated (:unlocated src)}
     :criteria-from (:criteria-from src)
     :machine-located (:machine-located src)
     ;; D11 part 5: what the mission does not state, and the reading step
     ;; would compute before the first click instead of refusing
     :readings-it-would-request
     {:coverage (when (get-in src [:readings-needed :coverage?])
                  {:found (count (:criteria-by-token src))})
      :constraints (when (get-in src [:readings-needed :constraints?])
                     {:mission-sha (get-in src [:readings-needed :mission-sha])
                      :tokens-an-edge-may-join (count (:known-tokens src))})
      :criteria (when (get-in src [:readings-needed :criteria?])
                  {:sections-read (get-in src [:readings-needed :sections-read])})
      :locators (vec (for [t (get-in src [:readings-needed :locators])
                           :let [c (get-in src [:criteria-by-token t])]]
                       {:want t :line (:line c) :criterion (first (str/split-lines (str (:stated c))))}))}
     ;; questions an earlier criteria reading raised, and who they go to
     :owner-questions (reading/published-questions store target)
     :open-wants (vec (remove #(true? (get universe %)) (:wants cw)))
     :constraints (mapv #(select-keys % [:want :requires :phase :through :line :by :quote])
                        (get-in src [:constraints :requires]))
     :constraint-questions (get-in src [:constraint-questions])
     :coverage-questions (get-in src [:coverage-questions])
     :unlocated (mapv #(select-keys % [:token :line :reason :decline]) (:unlocated src))
     :requests-it-would-issue
     (vec (for [t unproduced
                :let [c (get-in src [:criteria-by-token t])]]
            {:want t :phase (first (str/split (str (:phase c)) #" ")) :line (:line c)
             :criterion (first (str/split-lines (str (:stated c))))}))
     :published-already (get-in view [:machine-interpretations target] [])
     :store store
     :horizon horizon
     :construction-parameters {:budget (wm/construction-budget sources) :move-cost wm/construction-move-cost}
     :clicks {:max (or max-clicks 4)
              :run-ids (str "<date>-" id "-click-<n>")
              :via "POST /api/alpha/wm/click with flight-edn: an ordinary click (budget consume + cast-seat preflight); runner/run-opportunity! in the serving JVM"}
     :needs-in-serving-jvm "futon3c b7340968 (flight-edn) and futon2 runner/war-machine at this checkout, reloaded from master"
     :placement (select-keys (resolve-target opts) [:target :target-source :draw-seed :hand-target-overridden])
     :resolved-steps (resolved-steps opts)
     :run? false}))

(defn run-flight!
  "Fly the planned flight once. Returns {:flight … :record-path …}."
  [{:keys [seat store max-clicks sources checker bb library-root cascades
           run-record-dir click-fn answer-fn dispatch-step!] :as opts} planned]
  (let [target (:target (resolve-target opts))
        store (or store wi/default-store)
        run-record-dir (or run-record-dir runner/default-run-record-dir)
        record-path (fn [click-id] (str (io/file run-record-dir (str "tick-run-record-" click-id ".edn"))))
        f (flight-for (assoc opts :id (:flight-id planned)))
        answer (or answer-fn
                   (fr/agency-answer-fn (cond-> {:seat seat :caller "wm-flight" :opts (runner/config {})}
                                          library-root (assoc :library-root library-root))))
        enact (fr/enact-fn (cond-> {:interpretations (fn [fl] (:patterns (wi/read-published store (:target fl))))
                                    :fetch-run-record (fn [click-id]
                                                        (let [p (io/file (record-path click-id))]
                                                          (when (.isFile p)
                                                            (clojure.edn/read-string {:default tagged-literal} (slurp p)))))
                                    :record-dir (str (io/file store "flights" "enactments"))}
                             dispatch-step! (assoc :dispatch-step! dispatch-step!)))
        wc (fr/wc-verdict-fn (cond-> {:click-record-path record-path}
                               checker (assoc :checker checker)
                               bb (assoc :bb bb)))
        notify! (fn [owner tgt prompt] (runner/dispatch! (runner/config {}) owner "wm-flight" tgt
                                                  (str "Requisition: " tgt " — War Machine questions for the mission owner\n\n" prompt)))
        flown (flight/run! f {:read-fn (fr/read-fn (cond-> {:store store :answer-fn answer
                                                            :notify! notify! :caller "joe"}
                                                     cascades (assoc :served-by-cascades {target cascades})))
                              :ask-fn (fr/ask-fn {:store store :answer-fn answer})
                              :click-fn (or click-fn (fr/http-click-fn {:caller "wm-flight" :run-record-dir run-record-dir}))
                              :enact-fn enact
                              :wc-fn wc
                              :observe-fn (fr/observe-fn)
                              :sources-fn (constantly sources)
                              :max-clicks (or max-clicks 4)})
        path (io/file store "flights" (str (:flight/id flown) ".edn"))]
    (.mkdirs (.getParentFile path))
    (spit path (with-out-str (pp/pprint {:plan planned :flight flown})))
    {:flight flown :record-path (.getCanonicalPath path)
     :enactments (:enactments flown)
     :readings (vec (for [a (:readings flown) q (:asked a)] (select-keys q [:kind :want :request-id :seat :job-id :outcome])))
     :requests (vec (for [a (:asks flown) q (:asked a)] (select-keys q [:want :request-id :seat :job-id :outcome])))
     :published-store (str store "/" target ".edn")
     :clicks (mapv #(select-keys % [:click-id :advanced :open-after :abstention]) (:clicks flown))
     :status (:status flown) :closure-scope (:closure-scope flown) :needs (:needs flown)
     :open-questions (:open-questions flown)}))

(defn read-only!
  "The read step alone (D11 part 5): ask the seat for the readings the plan
  lists, publish the valid ones, and return them with the plan as it stands
  afterwards. No interpretation request, no click."
  [{:keys [seat store sources] :as opts} planned]
  (let [store (or store wi/default-store)
        f (flight-for (assoc opts :id (:flight-id planned)))
        answer (fr/agency-answer-fn {:seat seat :caller "wm-flight" :opts (runner/config {})})
        notify! (fn [owner tgt prompt] (runner/dispatch! (runner/config {}) owner "wm-flight" tgt
                                                  (str "Requisition: " tgt " — War Machine questions for the mission owner\n\n" prompt)))
        read ((fr/read-fn {:store store :answer-fn answer :notify! notify! :caller "joe"}) f sources)
        published (wi/read-published store (:target (resolve-target opts)))]
    {:readings (:asked read)
     :needs (:needs read)
     :published-locators (into {} (for [[t {:keys [locator receipt]}] (:locators published)]
                                    [t {:locator locator :cue (:cue receipt) :reading (:reading receipt)
                                        :observed-at-validation (:observed-at-validation receipt)
                                        :answered-by (:answered-by receipt)}]))
     :plan-after (plan (assoc opts :id (:flight-id planned)))}))

(defn main*
  "The driver without process exit: returns {:plan … :ran …}. :ran is nil
  unless ARGS carry --run."
  [args & [{:keys [load-sources] :or {load-sources #(cs/with-context-fn (cs/load-declared cs/default-dir))}}]]
  (let [opts (parse-args args)
        _ (check-args! opts)
        opts (cond-> opts
               (:max-clicks opts) (update :max-clicks parse-long)
               (:field-entry opts) (update :field-entry clojure.edn/read-string))
        opts (assoc opts :sources (load-sources))
        planned (plan opts)]
    {:plan planned
     :read (when (and (:read? opts) (not (:run? opts))) (read-only! opts planned))
     :ran (when (:run? opts) (run-flight! opts (assoc planned :run? true)))}))

(defn -main [& args]
  (let [{:keys [plan ran read]} (main* args)]
    (pp/pprint plan)
    (cond
      ran (pp/pprint ran)
      read (do (println "\n;; --read: the read step only (no interpretation request, no click)")
               (pp/pprint read))
      :else
      (println "\nPlan only. Nothing sent, nothing written. Add --read to run the read step only, --run to fly this flight once."))
    (shutdown-agents)))
