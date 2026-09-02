#!/usr/bin/env bb
(ns report-gate
  "U8 reporting gate: every report claim must be backed by a named query."
  (:require [babashka.curl :as curl]
            [clojure.edn :as edn]))

(def evidence-url "http://127.0.0.1:7073/api/alpha/evidence")

(defn join-key [entry]
  (let [body (:evidence/body entry)]
    [(:evidence/session-id entry) (:turn-id body) (:round body)
     (:pairing-key body)]))

(defn- query-result [claim backing-query verdict & [failure]]
  (cond-> {:claim claim :backing-query backing-query :verdict verdict}
    failure (assoc :failure failure)))

(defn- matching-decision [decisions claim]
  (some #(when (= (:join claim) (join-key %)) %) decisions))

(defn- matching-round [rounds claim]
  (some (fn [entry]
          (let [[session turn round _] (:join claim)
                body (:evidence/body entry)]
            (when (= [session turn round]
                     [(:evidence/session-id entry) (:turn-id body) (:round body)])
              entry)))
        rounds))

(defn- matching-clock [clocks claim]
  (some #(when (= (:join claim) (:join %)) %) clocks))

(defn adjudicate
  "Adjudicate typed REPORT claims against named query results in SOURCES.

  REPORT is a vector of claims with :claim/type and :value. Decision/R16
  claims also carry :join [session-id turn-id round pairing-key]. SOURCES has
  :decisions, :rounds, :clocks and :status entries, each with :query and typed
  results. Returns a per-claim verdict and a distinct typed failure vector."
  [report sources]
  (let [results
        (mapv
         (fn [{claim-type :claim/type :as claim}]
           (case claim-type
             :arm-chosen
             (let [entry (matching-decision (get-in sources [:decisions :records]) claim)]
               (if (= (:value claim) (get-in entry [:evidence/body :arm]))
                 (query-result claim (get-in sources [:decisions :query]) :backed)
                 (query-result claim (get-in sources [:decisions :query]) :failed
                               :u8/claim-unbacked)))

             :g-terms
             (let [entry (matching-decision (get-in sources [:decisions :records]) claim)]
               (if (= (:value claim) (get-in entry [:evidence/body :g-terms]))
                 (query-result claim (get-in sources [:decisions :query]) :backed)
                 (query-result claim (get-in sources [:decisions :query]) :failed
                               :u8/claim-unbacked)))

             :tool-calls
             (let [entry (matching-round (get-in sources [:rounds :records]) claim)]
               (if (= (:value claim) (get-in entry [:evidence/body :calls]))
                 (query-result claim (get-in sources [:rounds :query]) :backed)
                 (query-result claim (get-in sources [:rounds :query]) :failed
                               :u8/claim-unbacked)))

             :mission-attribution
             (let [decision (matching-decision
                             (get-in sources [:decisions :records]) claim)
                   clock (matching-clock (get-in sources [:clocks :records]) claim)
                   mission (:value claim)
                   recorded (get-in decision [:evidence/body :mission])]
               (cond
                 (and (= mission recorded) (= mission (:mission clock)))
                 (query-result claim (get-in sources [:clocks :query]) :backed)

                 ;; No recorded mission on the decision, or no clock witness
                 ;; joining to it: attribution is ABSENT from the record.
                 (or (nil? recorded) (nil? clock))
                 (query-result claim (get-in sources [:clocks :query]) :failed
                               :u8/decision-mission-attribution-absent)

                 ;; A mission IS recorded but disagrees with the claim (or the
                 ;; clock names a different mission): the claim is unbacked,
                 ;; not unattributable.
                 :else
                 (query-result claim (get-in sources [:clocks :query]) :failed
                               :u8/claim-unbacked)))

             :status
             (let [oracle (:status sources)
                   derived (:derived-status oracle)
                   evidence (:derived-from oracle)
                   ;; :derived-status comes from the NEWEST signal
                   ;; (z1_views.clj: derived = first all-signals, and
                   ;; :derived-from = take 10 all-signals in that order), so
                   ;; only the FIRST entry's source can vouch for it. `some`
                   ;; over the list would let any older commit signal launder
                   ;; a chat-derived status.
                   declared? (contains? #{:commit-subject
                                          :mission-sync-snapshot}
                                        (:source (first evidence)))]
               (cond
                 (not= (:value claim) derived)
                 (query-result claim (:query oracle) :failed :u8/claim-unbacked)

                 (not declared?)
                 (query-result claim (:query oracle) :failed
                               :u8/mission-status-signal-overbroad)

                 :else (query-result claim (:query oracle) :backed)))

             (query-result claim nil :failed :u8/claim-unbacked)))
         report)
        failures (->> results (keep :failure) distinct vec)]
    {:gate :u8/report-backed-by-typed-records
     :ok (empty? failures)
     :results results
     :failures failures}))

(defn- get-edn [url params]
  (-> (curl/get url {:query-params params :as :text :timeout 14000})
      :body edn/read-string))

(defn real-case
  "Read one current ZAIF decision and its typed companions from :7073.
  Read-only; the singular tag filter is deliberately not used."
  [mission]
  (let [search (get-edn (str evidence-url "/text-search")
                        {:tags "zaif" :limit 100})
        decision (some #(let [entry (:entry %)]
                          (when (= :zaif-arm-choice
                                   (get-in entry [:evidence/body :event]))
                            entry))
                       (:results search))
        body (:evidence/body decision)
        session-id (:evidence/session-id decision)
        session-query {:endpoint evidence-url
                       :params {:session-id session-id :limit 200}}
        session-result (get-edn evidence-url (:params session-query))
        rounds (filterv #(= :turn-round (get-in % [:evidence/body :event]))
                        (:entries session-result))
        round (some #(when (= [(:turn-id body) (:round body)]
                              [(get-in % [:evidence/body :turn-id])
                               (get-in % [:evidence/body :round])]) %)
                    rounds)
        status-var (or (resolve 'z1-views/mission-status)
                       (throw (ex-info "load z1_views.clj before real-case" {})))
        clock-var (or (resolve 'z1-views/mission-attributed)
                      (throw (ex-info "load z1_views.clj before real-case" {})))
        clocks (clock-var {:limit 50})
        status (status-var {:mission mission :limit 20})
        join (join-key decision)
        report [{:claim/type :arm-chosen :value (:arm body) :join join}
                {:claim/type :g-terms :value (:g-terms body) :join join}
                {:claim/type :tool-calls
                 :value (vec (get-in round [:evidence/body :calls])) :join join}
                {:claim/type :mission-attribution :value mission :join join}
                {:claim/type :status :value (:derived-status status)}]
        sources {:decisions
                 {:query {:endpoint (str evidence-url "/text-search")
                          :params {:tags "zaif" :limit 100}}
                  :records [decision]}
                 :rounds {:query session-query :records rounds}
                 :clocks
                 {:query (:query clocks)
                  ;; Z1 returns mission groups whose turns currently lack the
                  ;; decision join tuple. Preserve those real records as-is;
                  ;; adjudication must expose the absent join, not invent it.
                  :records (vec (for [group (:results clocks)
                                      turn (:turns group)]
                                  (assoc turn :mission (:mission group))))}
                 :status status}]
    {:report report :sources sources :decision decision :round round}))
