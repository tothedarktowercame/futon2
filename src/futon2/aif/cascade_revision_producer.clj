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
            [futon2.aif.wm.cascade-decision :as cascade-decision])
  (:import [java.nio.file Files StandardOpenOption]
           [java.util UUID]))

(load-identity/register! *ns* *file*)

(def receipt-schema :wm/cascade-revision-proposal-production-v1)

(defn- problem-row [judgment target]
  (first (filter #(= target (:target %))
                 (get-in judgment [:cascade-problems :problems]))))

(defn sources-from-judgment
  "Reconstitute the selected target's already-admitted construction inputs.
   Functions are installed by this composition seam, never persisted in the
   judgment."
  [judgment target]
  (when-let [row (problem-row judgment target)]
    (let [p (:cascade-problem row)
          budget (get-in judgment [:construction-parameters :budget :value])
          move-cost (get-in judgment [:construction-parameters :move-cost :value])]
      (when (and (map? budget) (number? move-cost))
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
         :construction
         {:construct construction/construct
          :budget budget
          :move-cost move-cost
          :evaluate-g (fn [problem candidate]
                        (cascade-decision/constructed-candidate-g problem candidate))}}))))

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
  (fn [{:keys [target blocker whole-mission ask! evidence-root]}]
    (let [judgment (when judgment-fn (judgment-fn))
          sources (sources-fn judgment target)
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
                                 :construction/refusal])}))))))
