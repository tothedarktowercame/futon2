(ns futon2.aif.flight-driver
  "One command for a flight: print the plan, and only with --run fly it.

    clojure -M -m futon2.aif.flight-driver M-futon-seams --seat kimi-6 \\
      --repo futon3c --path holes/missions/M-futon-seams.md \\
      --lifecycle-path holes/labs/M-futon-seams/lifecycle.edn [--max-clicks 4] [--run]

  Without --run nothing is sent and nothing is written: the plan is the
  record Joe authorizes against. With --run, ONE flight: the ask step before
  each click (D11, answering seat named here), clicks as ordinary clicks
  through POST /api/alpha/wm/click (budget and cast-seat preflight apply),
  and a flight record written under the store, whose path is printed with
  every request, answer job, publication and click."
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runner :as runner]
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
        (str/starts-with? x "--") (let [[v & rest] more]
                                    (when-not v (throw (ex-info (str x " needs a value") {:arg x})))
                                    (recur rest (assoc out (keyword (subs x 2)) v)))
        :else (recur more (assoc out :target x)))
      out)))

(defn- check-args! [{:keys [target seat repo path]}]
  (doseq [[k v] {:target target :seat seat :repo repo :path path}]
    (when (str/blank? v) (throw (ex-info (str "missing " (name k)) {:missing k}))))
  (when (refused-seats seat)
    (throw (ex-info (str seat " may not answer flight requests (claude-1's delegate)")
                    {:refused-seat seat}))))

(defn- flight-for [{:keys [target repo path lifecycle-repo lifecycle-path read-text code-root observe id store]}]
  (flight/start {:target target :chosen-because {:kind :requested :by "flight-driver"}}
                (cond-> {:kind :a-exits :repo repo :path path :store (or store wi/default-store)}
                  lifecycle-path (assoc :lifecycle {:repo (or lifecycle-repo repo) :path lifecycle-path})
                  read-text (assoc :read-text read-text)
                  code-root (assoc :code-root code-root)
                  observe (assoc :observe observe))
                {:id id}))

(defn plan
  "The flight the driver would fly, as data. OPTS: parsed args plus
  :sources (the tick's declared sources) and, for tests, :read-text,
  :observe and :id."
  [{:keys [target seat store max-clicks sources id] :as opts}]
  (let [store (or store wi/default-store)
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
     {:criteria (when (get-in src [:readings-needed :criteria?])
                  {:sections-read (get-in src [:readings-needed :sections-read])})
      :locators (vec (for [t (get-in src [:readings-needed :locators])
                           :let [c (get-in src [:criteria-by-token t])]]
                       {:want t :line (:line c) :criterion (first (str/split-lines (str (:stated c))))}))}
     :open-wants (vec (remove #(true? (get universe %)) (:wants cw)))
     :constraints (mapv #(select-keys % [:want :requires :phase :through :line :by])
                        (get-in src [:constraints :requires]))
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
     :run? false}))

(defn run-flight!
  "Fly the planned flight once. Returns {:flight … :record-path …}."
  [{:keys [target seat store max-clicks sources] :as opts} planned]
  (let [store (or store wi/default-store)
        f (flight-for (assoc opts :id (:flight-id planned)))
        answer (fr/agency-answer-fn {:seat seat :caller "wm-flight" :opts (runner/config {})})
        flown (flight/run! f {:read-fn (fr/read-fn {:store store :answer-fn answer})
                              :ask-fn (fr/ask-fn {:store store :answer-fn answer})
                              :click-fn (fr/http-click-fn {:caller "wm-flight"})
                              :observe-fn (fr/observe-fn)
                              :sources-fn (constantly sources)
                              :max-clicks (or max-clicks 4)})
        path (io/file store "flights" (str (:flight/id flown) ".edn"))]
    (.mkdirs (.getParentFile path))
    (spit path (with-out-str (pp/pprint {:plan planned :flight flown})))
    {:flight flown :record-path (.getCanonicalPath path)
     :readings (vec (for [a (:readings flown) q (:asked a)] (select-keys q [:kind :want :request-id :seat :job-id :outcome])))
     :requests (vec (for [a (:asks flown) q (:asked a)] (select-keys q [:want :request-id :seat :job-id :outcome])))
     :published-store (str store "/" target ".edn")
     :clicks (mapv #(select-keys % [:click-id :advanced :open-after :abstention]) (:clicks flown))
     :status (:status flown) :closure-scope (:closure-scope flown) :needs (:needs flown)}))

(defn main*
  "The driver without process exit: returns {:plan … :ran …}. :ran is nil
  unless ARGS carry --run."
  [args & [{:keys [load-sources] :or {load-sources #(cs/with-context-fn (cs/load-declared cs/default-dir))}}]]
  (let [opts (parse-args args)
        _ (check-args! opts)
        opts (cond-> opts (:max-clicks opts) (update :max-clicks parse-long))
        opts (assoc opts :sources (load-sources))
        planned (plan opts)]
    {:plan planned
     :ran (when (:run? opts) (run-flight! opts (assoc planned :run? true)))}))

(defn -main [& args]
  (let [{:keys [plan ran]} (main* args)]
    (pp/pprint plan)
    (if ran
      (pp/pprint ran)
      (println "\nPlan only. Nothing sent, nothing written. Add --run to fly this flight once."))
    (shutdown-agents)))
