(ns futon2.aif.open-cascade-relatedness-sut
  "Controlled, non-production composition of canonical retrieval, externally
   judged open-cascade search, and the real revision producer."
  (:require [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.cascade-revision :as revision]
            [futon2.aif.cascade-revision-producer :as producer]
            [futon2.aif.cascade-sources :as cascade-sources]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.open-cascade-pattern-search :as search]
            [futon2.aif.pattern-registry :as registry]))

(def schema :wm/open-cascade-relatedness-sut-v1)

(defn- refuse [kind & [data]]
  {:schema schema :status :refused :kind kind :detail data})

(defn- carrier [id content]
  (let [x {:id id :content content}]
    (assoc x :digest (evidence/value-digest x))))

(defn- member [candidate revision]
  {:id (cascade-sources/canonical-pattern-id (:id candidate)
                                             (:pattern-path candidate) :retrieval-id)
   :source-pin {:path (:pattern-path candidate)
                :revision revision
                :sha256 (:pattern-sha256 candidate)}})

(defn- repository [members revision]
  (let [r {:identity "canonical-context-retrieval"
           :version revision :members members}]
    (assoc r :digest (search/repository-digest r))))

(defn- dispatch-contract [selected applied]
  {:schema :wm/revision-selection-dispatch-v1
   :verdict (if (= selected (:selected-action applied)) :match :typed-divergence)
   :selected-action-sha256 (identity/digest selected)
   :dispatched-action-sha256 (identity/digest (:selected-action applied))})

(defn run-controlled!
  "Run the controlled seam. Canonical retrieval only proves a bounded top-k
   domain today; :complete therefore refuses rather than claiming global
   absence. INTERPRETATION-RESPONSE is the separate interpreter's authored
   reading. JUDGMENTS and AUTHORITY-RESULTS are the independent review job.
   No Agency or serving entrypoint is called."
  [{:keys [entries top-k scope limitation retrieval-revision judgments
           authority-results search-implementation query blocker prior-cascade
           interpretation-response revision-context sources code-root apply-revision-fn]}]
  (if-let [early (cond
                   (not= :controlled-sut (:mode revision-context))
                   (refuse :production-judgment-authority-unconfigured)
                   (= :complete scope) (refuse :canonical-retrieval-completeness-unavailable)
                   (not= :bounded scope) (refuse :domain-scope-invalid))]
    early
    (try
      (let [candidates (registry/aggregate-pattern-candidates entries :top-k top-k)
            members (mapv #(member % retrieval-revision) candidates)
            ids (mapv :id members)
            captured (into {} (map (fn [{:keys [source-pin]}]
                                     [[(:path source-pin) (:revision source-pin)]
                                      (java.nio.file.Files/readAllBytes
                                       (.toPath (io/file (:path source-pin))))]) members))
            admissible (set (map :pattern (filter #(= :admissible (:verdict %)) judgments)))
            chosen (first (filter admissible ids))
            result (if chosen {:kind :chosen-existing :pattern chosen}
                       {:kind :no-admissible-match})
            receipt (search/seal
                     {:schema search/schema :implementation search/implementation
                      :implementation-digest (search/implementation-digest)
                      :repository (repository members retrieval-revision)
                      :domain {:scope :bounded :members ids :limitation limitation}
                      :query (or query (carrier "controlled-query" {:entries (count entries)}))
                      :blocker blocker :prior-cascade prior-cascade
                      :search-implementation search-implementation :judgments judgments
                      :priority ids :result result :repository-global-absence nil})
            validated (search/validate! receipt {:captured-sources captured
                                                 :authority-results authority-results})]
        (if-not chosen
          {:schema schema :status :no-match :scope :bounded
           :limitation limitation :search validated}
          (if-not (= chosen (:pattern interpretation-response))
            (refuse :interpreter-chosen-pattern-mismatch
                    {:chosen chosen :interpreted (:pattern interpretation-response)})
            (let [proposal ((producer/make-proposals-fn
                             {:sources-fn (fn [_ _] sources)
                              :request-fn (fn [{:keys [target want]}]
                                            {:schema :wm/want-interpretation-request-v1
                                             :target target :want {:token want}})
                              :code-root code-root})
                            (assoc revision-context :ask!
                                   (constantly (str "```edn\n" (pr-str interpretation-response)
                                                    "\n```"))))]
              (if-not (= :admitted (:status proposal))
                (refuse :revision-admission-mismatch proposal)
                (let [ctx revision-context
                      revised (revision/revise
                               {:original (:original ctx)
                                :head-context (revision/head-seed (:original ctx))
                                :whole-context (dissoc (:whole-mission ctx) :content)
                                :blocker (:blocker ctx) :proposals (:candidates proposal)})
                      applied ((or apply-revision-fn revision/apply-to-construction)
                               (:construction ctx) revised)
                      dispatch (dispatch-contract (:revised revised) applied)]
                  (if (and (= :revised (:status revised)) (= :match (:verdict dispatch)))
                    {:schema schema :status :ready-for-external-dispatch
                     :scope :bounded :limitation limitation :chosen-pattern chosen
                     :search validated :proposal proposal :revision revised :dispatch dispatch}
                    (refuse :revision-dispatch-mismatch
                            {:revision revised :dispatch dispatch}))))))))
      (catch clojure.lang.ExceptionInfo e
        (refuse (or (:open-cascade-pattern-search/refusal (ex-data e))
                    :controlled-composition-failed) (ex-data e)))
      (catch Exception e
        (refuse :controlled-composition-failed
                {:class (.getName (class e)) :message (.getMessage e)})))))
