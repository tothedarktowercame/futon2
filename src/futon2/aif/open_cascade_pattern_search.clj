(ns futon2.aif.open-cascade-pattern-search
  "Pure validator/adapter for externally produced open-cascade search receipts.
  Deliberately unwired: it does not search, rank, judge, or dispatch."
  (:require [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]))

(def schema :wm/open-cascade-pattern-search-v1)
(def implementation {:id :futon2/open-cascade-pattern-search-validator
                     :version 1})

(defn- refuse! [reason path & [data]]
  (throw (ex-info "Open-cascade pattern search receipt refused"
                  (merge {:open-cascade-pattern-search/refusal reason :path path} data))))
(defn- require! [x reason path & [data]]
  (when-not x (refuse! reason path data)))
(defn- digest? [x] (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))
(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- unique? [xs] (= (count xs) (count (set xs))))
(defn- receipt-digest [r] (evidence/value-digest (dissoc r :receipt-digest)))

(defn repository-digest
  "Digest of the repository pin and ordered member/source-pin carrier."
  [repository]
  (evidence/value-digest (dissoc repository :digest)))

(defn implementation-digest [] (evidence/value-digest implementation))

(defn seal [receipt]
  (assoc receipt :receipt-digest (receipt-digest receipt)))

(defn- source-pin! [pin captured-sources path]
  (require! (map? pin) :source-pin-invalid path)
  (doseq [k [:path :revision]]
    (require! (nonblank? (get pin k)) :source-pin-invalid (conj path k)))
  (require! (digest? (:sha256 pin)) :source-pin-invalid (conj path :sha256))
  (let [bytes (get captured-sources [(:path pin) (:revision pin)])]
    (require! (bytes? bytes) :source-revision-unresolved path)
    (require! (= (:sha256 pin) (evidence/sha256 bytes)) :source-pin-stale path)))

(defn- identity! [x path]
  (require! (map? x) :identity-invalid path)
  (require! (nonblank? (:id x)) :identity-invalid (conj path :id))
  (require! (map? (:content x)) :identity-content-required (conj path :content))
  (require! (digest? (:digest x)) :identity-invalid (conj path :digest))
  (require! (= (:digest x) (evidence/value-digest (dissoc x :digest)))
            :identity-digest-mismatch (conj path :digest)))

(defn agency-result-digest [job]
  (evidence/value-digest (dissoc job :result-digest)))

(defn- authority! [authority authority-results path]
  (require! (map? authority) :authority-invalid path)
  (let [job (get authority-results (:job-id authority))]
    (require! (map? job) :authority-job-unresolved path)
    (require! (= "done" (:state job)) :authority-job-not-terminal path)
    (require! (= (:id authority) (:agent-id job)) :authority-agent-mismatch path)
    (require! (= (:job-id authority) (:job-id job)) :authority-job-mismatch path)
    (require! (= (:result-digest authority) (:result-digest job))
              :authority-result-mismatch path)
    (require! (= (:result-digest job) (agency-result-digest job))
              :authority-result-digest-mismatch path)
    ;; Trust boundary: an immutable caller-supplied Agency snapshot, not a signature.
    job))

(defn- legacy-receipt! [pattern source-pin receipt path]
  ;; This is the existing find-receipt carrier: structured antecedent plus an
  ;; authored-text/edge citation. A score or naked boolean cannot project.
  (require! (map? receipt) :legacy-receipt-unprojectable path)
  (require! (true? (:if receipt)) :legacy-receipt-unprojectable (conj path :if))
  (require! (= :structured-antecedent (:route receipt))
            :legacy-receipt-self-certifying (conj path :route))
  (require! (map? (:warrant receipt)) :legacy-receipt-unprojectable (conj path :warrant))
  (let [citation (:citation receipt)]
    (require! (map? citation) :legacy-receipt-unprojectable (conj path :citation))
    (case (:kind citation)
      :pattern-text
      (do (require! (= (:path source-pin) (:path citation))
                    :legacy-receipt-source-mismatch (conj path :citation :path))
          (require! (= (:sha256 source-pin) (:sha256 citation))
                    :legacy-receipt-source-mismatch (conj path :citation :sha256))
          (require! (and (vector? (:lines citation)) (= 2 (count (:lines citation)))
                         (every? pos-int? (:lines citation)) (nonblank? (:quote citation)))
                    :legacy-receipt-unprojectable (conj path :citation)))
      :authored-edges
      (require! (vector? (:tail citation)) :legacy-receipt-unprojectable
                (conj path :citation :tail))
      (refuse! :legacy-receipt-unprojectable (conj path :citation :kind))))
  {:pattern pattern :receipt receipt})

(defn validate!
  "Validate and adapt an externally supplied receipt. Returns the receipt with
  :projection containing only evidence-backed F11 legacy receipts."
  [r {:keys [captured-sources authority-results]}]
  (require! (map? captured-sources) :captured-sources-required [:captured-sources])
  (require! (map? authority-results) :authority-results-required [:authority-results])
  (require! (= schema (:schema r)) :schema-invalid [:schema])
  (require! (= implementation (:implementation r)) :implementation-invalid [:implementation])
  (require! (= (implementation-digest) (:implementation-digest r))
            :implementation-digest-mismatch [:implementation-digest])
  (require! (= (receipt-digest r) (:receipt-digest r))
            :receipt-digest-mismatch [:receipt-digest])
  (let [repo (:repository r) members (:members repo) domain (:domain r)
        domain-members (:members domain) judgments (:judgments r)
        member-by-id (into {} (map (juxt :id identity)) members)]
    (require! (and (map? repo) (nonblank? (:identity repo)) (nonblank? (:version repo))
                   (vector? members)) :repository-invalid [:repository])
    (require! (unique? (map :id members)) :repository-members-duplicate [:repository :members])
    (doseq [[i member] (map-indexed vector members)]
      (require! (keyword? (:id member)) :pattern-id-invalid [:repository :members i :id])
      (source-pin! (:source-pin member) captured-sources [:repository :members i :source-pin]))
    (require! (= (repository-digest repo) (:digest repo))
              :repository-digest-mismatch [:repository :digest])
    (require! (and (map? domain) (vector? domain-members) (unique? domain-members))
              :domain-invalid [:domain])
    (case (:scope domain)
      :complete (require! (= (mapv :id members) domain-members)
                          :complete-domain-not-repository [:domain :members])
      :bounded (do (require! (nonblank? (:limitation domain))
                             :bounded-limitation-required [:domain :limitation])
                   (require! (every? member-by-id domain-members)
                             :bounded-domain-outside-repository [:domain :members]))
      (refuse! :domain-scope-invalid [:domain :scope]))
    (doseq [k [:query :blocker :prior-cascade]] (identity! (get r k) [k]))
    (identity! (:search-implementation r) [:search-implementation])
    (require! (and (vector? judgments)
                   (= domain-members (mapv :pattern judgments)))
              :judgment-coverage-or-order-mismatch [:judgments])
    (require! (unique? (map :pattern judgments)) :judgment-duplicate [:judgments])
    (let [projected
          (into {}
                (map-indexed
                 (fn [i j]
                   (let [path [:judgments i] pin (get-in member-by-id [(:pattern j) :source-pin])]
                     (authority! (:authority j) authority-results (conj path :authority))
                     (require! (not= (get-in j [:authority :id])
                                     (get-in r [:search-implementation :id]))
                               :judgment-self-authority (conj path :authority))
                     (require! (and (map? (:evidence j)) (seq (:evidence j)))
                               :judgment-evidence-required (conj path :evidence))
                     (require! (= pin (get-in j [:evidence :source-pin]))
                               :judgment-source-mismatch (conj path :evidence :source-pin))
                     (case (:verdict j)
                       :admissible [(:pattern j)
                                    (legacy-receipt! (:pattern j) pin
                                                     (get-in j [:evidence :legacy-receipt])
                                                     (conj path :evidence :legacy-receipt))]
                       :rejected (do (require! (nonblank? (get-in j [:evidence :reason]))
                                              :rejection-reason-required (conj path :evidence :reason))
                                     nil)
                       (refuse! :judgment-verdict-invalid (conj path :verdict)))))
                 judgments))
          priority (:priority r)
          admissible (filterv #(= :admissible (:verdict %)) judgments)
          first-admissible (first (filter (set (map :pattern admissible)) priority))
          result (:result r)]
      (require! (and (vector? priority) (unique? priority) (= (set priority) (set domain-members)))
                :priority-invalid [:priority])
      (case (:kind result)
        :chosen-existing
        (do (require! (= first-admissible (:pattern result))
                      :chosen-not-first-admissible [:result :pattern])
            (require! (contains? projected (:pattern result))
                      :chosen-not-admissible [:result :pattern]))
        :no-admissible-match
        (require! (empty? admissible) :no-match-has-admissible-member [:result])
        (refuse! :result-kind-invalid [:result :kind]))
      (when (= :bounded (:scope domain))
        (require! (nil? (:repository-global-absence r))
                  :bounded-global-absence-forbidden [:repository-global-absence]))
      (when (and (= :complete (:scope domain)) (= :no-admissible-match (:kind result)))
        (require! (= :no-pattern-addresses-this-tension (:repository-global-absence r))
                  :complete-no-match-absence-required [:repository-global-absence]))
      (assoc r :projection {:legacy-receipts projected
                            :repository-global-absence (:repository-global-absence r)}))))
