(ns futon2.aif.cascade-revision-producer
  "Production producer for blocker-responsive provisional cascades. Retrieval
   remains unjudged until the existing want-interpretation validator constructs
   and admits an executable candidate."
  (:require [clojure.java.io :as io]
            [futon2.aif.cascade-policy :as cascade-policy]
            [futon2.aif.cascade-problems :as cascade-problems]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.want-interpretation :as want-interpretation]
            [futon2.aif.wm.cascade-decision :as cascade-decision]
            [futon2.aif.wm.construction-inputs :as wm-inputs])
  (:import [java.nio.file Files StandardOpenOption]
           [java.util UUID]))

(load-identity/register! *ns* *file*)

(def receipt-schema :wm/cascade-revision-proposal-production-v1)

(defn- problem-row [judgment target]
  (first (filter #(= target (:target %))
                 (get-in judgment [:cascade-problems :problems]))))

(defn- construction-port
  "The constructor seam with explicit bounds. No judgment on the production
   path records :construction-parameters (only the flight PLAN carries that
   key, and it is not the judgment), so the bounds come from
   wm.construction-inputs with their recorded authority rather than from a
   key nothing writes."
  []
  {:construct construction/construct
   :budget (:value (wm-inputs/construction-budget nil))
   :move-cost (:value wm-inputs/construction-move-cost)
   :evaluate-g (fn [problem candidate]
                 (cascade-decision/constructed-candidate-g problem candidate))})

(defn sources-from-judgment
  "Reconstitute the selected target's already-admitted construction inputs.
   Functions are installed by this composition seam, never persisted in the
   judgment."
  [judgment target]
  (when-let [row (problem-row judgment target)]
    (let [p (:cascade-problem row)
          budget (get-in judgment [:construction-parameters :budget :value])
          move-cost (get-in judgment [:construction-parameters :move-cost :value])
          port (construction-port)]
      (when (number? move-cost)
        {:universes {target (:facts p)}
         :wants {target (:want p)}
         :locators {target (:locators p)}
         :interpretations
         {target {:patterns (:interpretations p)
                  :receipts (:interpretation-receipts row)}}
         :horizon-steps (:horizon-steps p)
         :beta-by-context {:revision {:beta (:beta p)}}
         :context-of (constantly :revision)
         :pattern-feedback (when-let [feedback (:pattern-feedback p)]
                             {target feedback})
         :construction (assoc port :budget (or budget (:budget port))
                              :move-cost move-cost)}))))

(defn- interpretation-of
  "One precedence entry of a selected action, translated to the
   constructor's grammar. Reading-route candidates carry guards as
   conjunction clauses ({:present #{tok} :absent #{}}); the constructor
   reads :needs/:forbids. The two grammars say the same thing about which
   tokens a pattern requires, so both translate; neither is guessed when a
   guard is absent."
  [entry]
  (let [g (:guard entry)
        guard (cond
                (and (map? g) (seq (:clauses g)))
                {:needs (into #{} (mapcat :present) (:clauses g))
                 :forbids (into #{} (mapcat :absent) (:clauses g))}

                (map? g)
                {:needs (set (:needs g)) :forbids (set (:forbids g))}

                :else {:needs #{} :forbids #{}})]
    {:guard guard :produces (set (:produces entry))}))

(defn sources-from-candidate
  "Reconstitute the revision construction view from the ORIGINAL selected
   action itself, when the judgment's interpreted assembly has no row for
   the target. On the production reading-policy route it never has one —
   the tick's trace records :cascade-problems {:problems []} because the
   interpreted-construction route is retired — and no judgment writer
   records :construction-parameters, so both legs sources-from-judgment
   needs are absent and every production revision attempt refused
   :revision-construction-inputs-unavailable (measured 2026-10-31, run
   2026-10-31-0be4fc7d-70db-4e96-8beb-ea24a1867896, click
   wm-click-f56a06af: the answer was corrected by the revision round and
   the cascade never was).

   The snapshot is the post-construction state the blocker interrupted:
   every token the original precedence produces or guards on is
   established, the target's wants are not (the run stopped at review, not
   at a satisfied want). β is the judgment's declared beta for the target —
   no declaration refuses, no default is invented."
  [judgment original]
  (when (and (map? original)
             (= :cascade-candidate (:kind original))
             (string? (:target original)))
    (let [target (:target original)
          entries (filter map? (:precedence original))
          patterns (into {}
                         (map (fn [e] [(or (:id e) (:pattern-id e))
                                       (interpretation-of e)]))
                         entries)
          established (set (concat (mapcat :produces (vals patterns))
                                   (mapcat #(-> % :guard :needs) (vals patterns))))
          wants (vec (:want original))
          facts (merge (zipmap established (repeat true))
                       (zipmap wants (repeat false)))
          beta (get-in judgment [:cascade-problems :declared-betas target])]
      (when (and (seq wants) (number? beta))
        {:universes {target facts}
         :wants {target wants}
         ;; the candidate's locators are keyed by the same target-qualified
         ;; tokens its guards and produces use, and R5's rate coverage keys
         ;; them the same way, so they pass through unmodified
         :locators {target (:observation-locators original)}
         :interpretations {target {:patterns patterns
                                   :receipts (or (:interpretation-receipts original) {})}}
         :horizon-steps (inc (max (count patterns) 1))
         :beta-by-context {:revision {:beta beta}}
         :context-of (constantly :revision)
         :construction (construction-port)}))))

(defn- write-query-source! [root whole-mission blocker]
  (let [dir (io/file root (str "request-" (UUID/randomUUID)))
        file (io/file dir "whole-mission-and-blocker.md")
        marker (str "WM MID-RUN BLOCKER " (pr-str blocker))
        content (str (:content whole-mission) "\n\n" marker "\n")]
    (.mkdirs dir)
    (Files/write (.toPath file) (.getBytes content "UTF-8")
                 (into-array StandardOpenOption
                             [StandardOpenOption/CREATE_NEW StandardOpenOption/WRITE]))
    {:root dir :file file :marker marker
     :sha256 (evidence/sha256 (.getBytes content "UTF-8"))}))

(defn- default-request!
  [{:keys [target want facts patterns whole-mission blocker evidence-root]}]
  (let [{:keys [root file marker sha256]}
        (write-query-source! evidence-root whole-mission blocker)]
    (assoc
     (want-interpretation/request!
      {:target target :want want
       :criterion {:kind :mid-run-blocker :phase :revision :stated marker}
       :facts facts :patterns patterns}
      root
      {:resolve-fn (fn [_] {:id target :path (.getCanonicalPath file)})
       ;; The query source is itself a retained snapshot, outside a repository.
       :revision-fn (constantly (str "whole+blocker:" (:sha256 whole-mission)))})
     :revision-query-source
     {:path (.getCanonicalPath file) :sha256 sha256
      :whole-mission {:path (:path whole-mission) :sha256 (:sha256 whole-mission)}
      :blocker-sha256 (evidence/value-digest blocker)})))

(defn- trial-sources [sources target validated]
  (let [[id interpretation] (first (:interpretation validated))]
    (-> sources
        (assoc-in [:interpretations target :patterns id] interpretation)
        (assoc-in [:interpretations target :receipts id] (:receipt validated))
        (update :candidates dissoc target))))

(defn- executable-action [sources target validated]
  (let [trial (trial-sources sources target validated)
        assembled (cascade-problems/assemble {:targets [target] :sources trial})
        row (first (:problems assembled))
        admitted ((var-get #'cascade-decision/admit-cascade-problem) row)
        precedence (get-in validated [:candidate :precedence])
        candidate (first (filter #(= precedence (:precedence %))
                                 (get-in admitted [:problem :constructed-candidates])))
        p (get-in admitted [:problem :cascade-problem])
        qualify (fn [token] [target token])
        patterns (into {}
                       (map (fn [[id {:keys [guard produces]}]]
                              [id (-> (cascade-policy/token-interpretation
                                       id {:guard {:needs (set (map qualify (:needs guard)))
                                                   :forbids (set (map qualify (:forbids guard)))}
                                           :produces (set (map qualify produces))})
                                      (assoc :target target))]))
                       (:interpretations p))]
    (when candidate
      {:kind :cascade-candidate :id (:candidate-id candidate) :target target
       :precedence (mapv patterns (:precedence candidate))
       :observation-locators
       (into {} (map (fn [[token locator]] [(qualify token) locator])) (:locators p))
       :construction-receipt (:construction-receipt candidate)
       :interpretation-receipts (:interpretation-receipts (get admitted :problem))})))

(defn make-proposals-fn
  "Create the production port. ASK! is supplied at invocation by the runner;
   it dispatches through the existing Agency job boundary and returns reply
   text. Raw retrieval/proposal forms never reach validation or construction."
  [{:keys [judgment-fn sources-fn request-fn code-root]
    :or {sources-fn sources-from-judgment
         request-fn default-request!
         code-root "/home/joe/code"}}]
  (fn [{:keys [target blocker whole-mission ask! evidence-root original]}]
    (let [judgment (when judgment-fn (judgment-fn))
          sources (or (sources-fn judgment target)
                      ;; The production reading-policy judgment carries no
                      ;; interpreted problem row; the original selected
                      ;; action is then the only real construction input.
                      (sources-from-candidate judgment original))
          wants (get-in sources [:wants target])
          facts (get-in sources [:universes target])
          want (first (remove #(true? (get facts %)) wants))]
      (cond
        (not (fn? ask!))
        {:status :refused :kind :revision-agent-port-unavailable}

        (not (map? sources))
        {:status :refused :kind :revision-construction-inputs-unavailable}

        (nil? want)
        {:status :refused :kind :no-residual-want-for-revision}

        :else
        (try
          (let [request (request-fn {:target target :want want
                                     :facts (get-in sources [:universes target])
                                     :patterns (get-in sources [:interpretations target :patterns])
                                     :whole-mission whole-mission :blocker blocker
                                     :evidence-root evidence-root})
                reply (ask! (str (want-interpretation/prompt request)
                                 "\nThis is a blocker-driven revision. Read the pinned whole-mission source in "
                                 evidence-root "; do not treat retrieval hits as interpretations."))
                parsed (want-interpretation/parse-reply reply)]
            (if-not (:response parsed)
              {:status :refused
               :kind (if (:decline parsed)
                       :revision-interpreter-declined
                       :raw-or-uninterpreted-revision-response)
               :detail (or (:decline parsed) (:unparseable-response parsed))}
              (let [validated
                    (want-interpretation/validate-response
                     request (:response parsed)
                     {:code-root code-root :sources sources :constraints []
                      :admit #'cascade-decision/admit-cascade-problem})
                    action (when (= :valid (:status validated))
                             (executable-action sources target validated))]
                (if-not action
                  {:status :refused :kind :revision-interpretation-not-admitted
                   :validation (select-keys validated [:status :reasons :decline])}
                  {:status :admitted
                   :candidates [action]
                   :receipt {:schema receipt-schema
                             :target target
                             :whole-mission-sha256 (:sha256 whole-mission)
                             :blocker-sha256 (evidence/value-digest blocker)
                             :request-sha256 (evidence/value-digest request)
                             :revision-query-source (:revision-query-source request)
                             :pattern (first (keys (:interpretation validated)))
                             :pattern-source (get-in validated [:receipt :source])
                             :library-search
                             {:status :recorded
                              :snapshot {:query-source (:revision-query-source request)
                                         :retrieval-sha256
                                         (when (:retrieval request)
                                           (evidence/value-digest (:retrieval request)))}
                              :query-sha256 (evidence/value-digest
                                             (select-keys request [:target :want :context :retrieval]))
                              :considered
                              (let [selected (first (keys (:interpretation validated)))
                                    hits (mapcat :candidates (get-in request [:retrieval :runs]))]
                                (if (seq hits)
                                  (mapv (fn [hit]
                                          (let [id (or (:pattern hit) (:id hit))]
                                            {:pattern id
                                             :disposition (if (= (str id) (str selected))
                                                            :selected :rejected)
                                             :reason (if (= (str id) (str selected))
                                                       :admitted-reading
                                                       :not-selected-by-interpreter)})) hits)
                                  [{:status :absent
                                    :reason :retriever-returned-no-retained-candidates}]))}
                             :admission
                             {:status :admitted
                              :authority :want-interpretation-validate-response
                              :construction-receipt-sha256
                              (evidence/value-digest (:construction-receipt action))}
                             :construction :machine-constructed}}))))
          (catch Exception e
            {:status :refused :kind :revision-proposal-production-failed
             :exception-class (.getName (class e)) :message (.getMessage e)
             :data (select-keys (ex-data e)
                                [:interpretation/refusal :interpretation-evidence/refusal
                                 :construction/refusal :constructor/refusal
                                 :refusal :stopped-at])}))))))
