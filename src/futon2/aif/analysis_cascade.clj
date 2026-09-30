(ns futon2.aif.analysis-cascade
  "Pure conversion from one validated 象 turn analysis to arranged cascades.

  The analysis already supplies the topology. Fragment order supplies
  precedence. Roles qualify each consecutive edge: context, condition,
  dependency, and goal enable what follows; rationale justifies the preceding
  node; contrast contrasts with it; otherwise the edge is plain precedence.
  The edge retains both fragments and their complete role vectors, so this
  qualification never discards 象's reading.

  MODE controls co-application on one fragment. In :alternatives mode (the
  default), refs on one fragment fork readings. In :overlap mode, they are
  nodes in one reading joined by :overlap edges because they explain shared
  state in the same exact span. A recorded rejection forks either mode: the
  rejected pattern never becomes a node, but its id/reason/query remain on
  that variant. Thus every node is a validator-stamped pattern_ref, never a
  retrieval hit that 象 rejected."
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

(defn- node [fragment-index fragment ref]
  {:pattern (:id ref)
   :source-sha256 (:source_sha256 ref)
   :pattern-rationale (:rationale ref)
   :fragment-index fragment-index
   :sentence-id (:sentence-id fragment)
   :fragment {:start (:start fragment) :end (:end fragment)
              :text (:text fragment) :intent (:intent fragment)
              :target (:target fragment)
              :rationale (:rationale fragment)}
   :roles (vec (:relations fragment))})

(defn- fragment-choices [mode fragment-index fragment]
  (let [refs (vec (filter validated-ref? (:pattern_refs fragment)))
        rejections (vec (:pattern_rejections fragment))
        readings (if (seq rejections) (cons nil rejections) [nil])
        node-groups (case mode
                      :overlap [(mapv #(node fragment-index fragment %) refs)]
                      :alternatives (mapv #(vector (node fragment-index fragment %)) refs))]
    (vec
     (for [nodes node-groups
           :when (seq nodes)
           rejection readings]
       {:nodes nodes
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

(defn- role-edge [left right]
  {:from (:pattern left)
   :to (:pattern right)
   :kind (edge-kind {:node left} {:node right})
   :from-fragment (:fragment-index left)
   :to-fragment (:fragment-index right)
   :from-roles (:roles left)
   :to-roles (:roles right)})

(defn- overlap-edges [nodes]
  (vec (for [i (range (count nodes))
             j (range (inc i) (count nodes))
             :let [left (nth nodes i) right (nth nodes j)]]
         {:from (:pattern left) :to (:pattern right) :kind :overlap
          :from-fragment (:fragment-index left)
          :to-fragment (:fragment-index right)
          :from-roles (:roles left) :to-roles (:roles right)})))

(defn- arrange [reading]
  (let [nodes (vec (mapcat :nodes reading))
        within (mapcat #(overlap-edges (:nodes %)) reading)
        between (mapcat (fn [left right]
                          (for [a (:nodes left) b (:nodes right)] (role-edge a b)))
                        reading (rest reading))]
    {:nodes nodes
     :edges (vec (concat within between))
     :precedence (mapv :pattern nodes)
     :alternatives (vec (keep :rejected-alternative reading))}))

(defn analysis->cascades
  "Return every cascade supported by ANALYSIS and counted conversion failures.
   MODE is :alternatives (default) or :overlap. Zero validated refs is a
   failure count, not a successful typed absence."
  ([analysis] (analysis->cascades analysis {:mode :alternatives}))
  ([analysis {:keys [mode] :or {mode :alternatives}}]
   (when-not (#{:alternatives :overlap} mode)
     (throw (ex-info "Unknown analysis-cascade mode" {:mode mode})))
   (let [fragments (fragment-records analysis)
         groups (mapv (fn [i fragment] (fragment-choices mode i fragment))
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
      :mode mode
      :cascades cascades
      :failures failures
      :failure-count (count failures)})))
