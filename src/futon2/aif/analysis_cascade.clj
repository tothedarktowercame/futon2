(ns futon2.aif.analysis-cascade
  "Pure conversion from one validated 象 turn analysis to arranged cascades.

  The analysis already supplies the topology. Fragment order supplies
  precedence. Roles qualify each consecutive edge: context, condition,
  dependency, and goal enable what follows; rationale justifies the preceding
  node; contrast contrasts with it; otherwise the edge is plain precedence.
  The edge retains both fragments and their complete role vectors, so this
  qualification never discards 象's reading.

  Pattern refs on one fragment are alternatives because they explain the same
  span. A recorded rejection also forks an alternative reading: the rejected
  pattern never becomes a node, but its id/reason/query remain on that variant.
  Thus every node is a validator-stamped pattern_ref, never a retrieval hit
  that 象 rejected."
  (:require [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def structural-roles
  #{"context" "condition" "contrast" "action" "rationale" "goal" "dependency"})

(defn- validated-ref? [ref]
  (and (= "candidate" (:status ref))
       (string? (:id ref)) (seq (:id ref))
       (string? (:rationale ref)) (seq (:rationale ref))
       (string? (:source_sha256 ref)) (re-matches #"[0-9a-f]{64}" (:source_sha256 ref))))

(defn- fragment-records [analysis]
  (->> (:sentences analysis)
       (mapcat (fn [sentence]
                 (map #(assoc % :sentence-id (:id sentence)) (:fragments sentence))))
       (sort-by (juxt :start :end :sentence-id))
       vec))

(defn- alternatives [fragment-index fragment]
  (let [refs (filter validated-ref? (:pattern_refs fragment))
        rejections (vec (:pattern_rejections fragment))
        readings (cons nil rejections)]
    (vec
     (for [ref refs
           rejection (if (seq rejections) readings [nil])]
       {:node {:pattern (:id ref)
               :source-sha256 (:source_sha256 ref)
               :pattern-rationale (:rationale ref)
               :fragment-index fragment-index
               :sentence-id (:sentence-id fragment)
               :fragment {:start (:start fragment) :end (:end fragment)
                          :text (:text fragment) :intent (:intent fragment)
                          :target (:target fragment)
                          :rationale (:rationale fragment)}
               :roles (vec (:relations fragment))}
        :rejected-alternative (when rejection
                                (select-keys rejection [:id :reason :query]))}))))

(defn- cartesian [groups]
  (reduce (fn [rows choices]
            (vec (for [row rows choice choices] (conj row choice))))
          [[]] groups))

(defn- edge-kind [left right]
  (let [l (set (get-in left [:node :roles]))
        r (set (get-in right [:node :roles]))]
    (cond
      (r "rationale") :justified-by
      (r "contrast") :contrasts-with
      (l "dependency") :dependency-enables
      (l "condition") :condition-enables
      (l "context") :context-enables
      (l "goal") :goal-precedes
      :else :precedes)))

(defn- arrange [reading]
  (let [nodes (mapv :node reading)
        edges (mapv (fn [left right]
                      {:from (get-in left [:node :pattern])
                       :to (get-in right [:node :pattern])
                       :kind (edge-kind left right)
                       :from-fragment (get-in left [:node :fragment-index])
                       :to-fragment (get-in right [:node :fragment-index])
                       :from-roles (get-in left [:node :roles])
                       :to-roles (get-in right [:node :roles])})
                    reading (rest reading))]
    {:nodes nodes
     :edges edges
     :precedence (mapv :pattern nodes)
     :alternatives (vec (keep :rejected-alternative reading))}))

(defn analysis->cascades
  "Return every cascade supported by ANALYSIS and counted conversion failures.
   Zero validated refs is a failure count, not a successful typed absence."
  [analysis]
  (let [fragments (fragment-records analysis)
        groups (mapv (fn [i fragment] (alternatives i fragment))
                     (range) fragments)
        supported (vec (filter seq groups))
        failures (vec (keep-indexed
                       (fn [i choices]
                         (when (empty? choices)
                           {:kind :fragment-without-validated-pattern-ref
                            :fragment-index i}))
                       groups))
        cascades (if (seq supported)
                   (->> (cartesian supported) (map arrange) distinct vec)
                   [])]
    {:status (if (seq cascades) :constructed :failed)
     :cascades cascades
     :failures failures
     :failure-count (count failures)}))
