(ns futon2.aif.categorical-state-close-attachment
  "Offline exact join of a pinned close and a revalidated categorical annotation.

  The context resolver selects all source pointers. This module never trusts a
  caller-supplied qualified envelope, writes no close, and estimates no mass."
  (:require [clojure.string :as str]
            [futon2.aif.categorical-state-observation :as observation]))

(def context-schema :wm/close-annotation-context-v1)
(def joined-schema :wm/close-categorical-annotation-join-v1)

(defn- refuse! [reason path & [data]]
  (throw (ex-info (str "Close annotation attachment refused: " (name reason))
                  (merge {:refusal reason :path path} data))))

(defn- demand! [pred reason path & [data]]
  (when-not pred (refuse! reason path data)))

(defn- nonblank? [x]
  (and (or (string? x) (keyword? x) (symbol? x))
       (not (str/blank? (name x)))))

(defn- resolved-pointer! [resolver kind ref]
  (demand! (fn? resolver) :attachment-resolver-missing [:resolver])
  (let [pointer (resolver kind ref)]
    (demand! (map? pointer) :attachment-authority-not-found [kind ref])
    pointer))

(defn inspect-close
  "Read-only qualification census for one already parsed close. This reports
  absences only; it never derives an entity, run, annotation, or context."
  [close-record]
  (let [judgment (get-in close-record [:payload :judgment])
        reasons (cond-> []
                  (not= :closed (:checkpoint/type close-record))
                  (conj :not-a-close)
                  (not= :present (get-in judgment [:outcome-entity :status]))
                  (conj :outcome-entity-missing)
                  (nil? (get-in judgment [:entity-state-at-close :entity/id]))
                  (conj :entity-state-at-close-missing)
                  (nil? (get-in judgment [:entity-state-at-close :belief-source :run/id]))
                  (conj :run-id-missing)
                  (nil? (:categorical-state-observation judgment))
                  (conj :annotation-authority-missing)
                  (nil? (:categorical-state-context judgment))
                  (conj :context-authority-missing))]
    {:schema :wm/close-annotation-discovery-v1
     :status (if (seq reasons) :absent :present)
     :cohort/id (:cohort/id close-record)
     :attempt/id (:attempt/id close-record)
     :checkpoint/type (:checkpoint/type close-record)
     :recorded-at (:recorded-at close-record)
     :outcome (:outcome judgment)
     :reasons reasons}))

(defn- join-close! [close-record context]
  (let [point (:point context)
        entity (get-in context [:subject :entity/id])
        judgment (get-in close-record [:payload :judgment])]
    (demand! (= :closed (:checkpoint/type close-record)) :wrong-close [:checkpoint/type])
    (demand! (= (:cohort/id point) (:cohort/id close-record))
             :close-cohort-mismatch [:cohort/id])
    (demand! (= (:attempt/id point) (:attempt/id close-record))
             :close-attempt-mismatch [:attempt/id])
    (demand! (= (:event/sequence point) (:event/sequence close-record))
             :close-checkpoint-mismatch [:event/sequence])
    (demand! (= (:disposition/recorded-at point) (:recorded-at close-record))
             :close-time-mismatch [:recorded-at])
    (demand! (= :present (get-in judgment [:outcome-entity :status]))
             :close-entity-missing [:payload :judgment :outcome-entity])
    (demand! (= entity (get-in judgment [:outcome-entity :entity/id]))
             :close-entity-mismatch [:payload :judgment :outcome-entity :entity/id])
    (demand! (= entity (get-in judgment [:entity-state-at-close :entity/id]))
             :close-state-entity-mismatch [:payload :judgment :entity-state-at-close :entity/id])
    (demand! (= (:run/id point)
                (get-in judgment [:entity-state-at-close :belief-source :run/id]))
             :close-run-mismatch [:payload :judgment :entity-state-at-close :belief-source :run/id])
    {:disposition (:outcome judgment)
     :outcome-entity (:outcome-entity judgment)
     :entity-state-at-close (:entity-state-at-close judgment)}))

(defn attach-from-context!
  "Resolve a pinned context, then its pinned close and raw observation. Re-run
  the observation validator against the context-derived expected authority.
  A map resembling a qualified envelope is only an opaque resolver key."
  [context-ref {:keys [resolver io-opts] :as authority}]
  (let [context-pointer (resolved-pointer! resolver :context context-ref)
        context (observation/read-pinned-form! context-pointer io-opts)]
    (demand! (= context-schema (:schema context)) :invalid-attachment-context [:context])
    (demand! (nonblank? (:context/id context)) :invalid-attachment-context [:context :context/id])
    (demand! (= context-ref (:context/ref context)) :context-reference-mismatch [:context :context/ref])
    (let [close-ref (:close/ref context)
          annotation-ref (:annotation/ref context)
          close-pointer (resolved-pointer! resolver :close close-ref)
          annotation-pointer (resolved-pointer! resolver :annotation annotation-ref)]
      (demand! (= close-pointer (:close/source context))
               :close-source-mismatch [:context :close/source])
      (demand! (= annotation-pointer (:annotation/source context))
               :annotation-source-mismatch [:context :annotation/source])
      (let [close-record (observation/read-pinned-form! close-pointer io-opts)
            candidate (observation/read-pinned-form! annotation-pointer io-opts)
            expected (select-keys context [:subject :point :authority/scope
                                           :authority/provenance])
            qualified (observation/validate-observation!
                       candidate (assoc authority :expected expected))
            close-join (join-close! close-record context)]
        {:schema joined-schema
         :status :joined
         :context/ref context-ref
         :context/source context-pointer
         :context context
         :close/ref close-ref
         :close/source close-pointer
         :close-record close-record
         :annotation/ref annotation-ref
         :annotation/source annotation-pointer
         :validated-annotation qualified
         :entity/id (get-in context [:subject :entity/id])
         :point (:point context)
         :disposition (:disposition close-join)
         :acquisition {:method (:method qualified)
                       :rubric (get-in qualified [:limitations :rubric])
                       :retrospective? (get-in qualified [:limitations :retrospective?])
                       :missingness (get-in qualified [:limitations :missingness])
                       :selection (get-in qualified [:limitations :selection])
                       :authority/scope (:authority/scope qualified)
                       :authority/provenance (:authority/provenance qualified)}}))))

(defn attach-all!
  "Attach independently resolved contexts; duplicate or conflicting annotations
  at one authoritative point refuse rather than being voted or overwritten."
  [context-refs authority]
  (let [joined (mapv #(attach-from-context! % authority) context-refs)
        groups (group-by #(select-keys % [:entity/id :point]) joined)]
    (doseq [[point xs] groups]
      (when (< 1 (count xs))
        (let [states (set (map #(get-in % [:validated-annotation :observation
                                           :categorical-status :value]) xs))]
          (refuse! (if (< 1 (count states))
                     :conflicting-close-annotations :duplicate-close-annotation)
                   [:contexts] {:point point}))))
    joined))
