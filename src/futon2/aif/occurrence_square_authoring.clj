(ns futon2.aif.occurrence-square-authoring
  "Pre-G authoring of situation maps for every occurrence in one retained
  target reading. The machine constructs, validates and publishes squares."
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.occurrence-square-registry :as registry]
            [futon2.aif.target-reading-registry :as readings]
            [futon2.aif.three-halves-square :as square]))

(def schema :wm/occurrence-square-authoring-v1)

(defn- refs [reading]
  (vec
   (mapcat
    (fn [[fragment-index fragment]]
      (for [ref (:pattern_refs fragment)
            :when (= "candidate" (:status ref))]
        {:occurrence-id [(:id ref) fragment-index]
         :pattern (:id ref)
         :pattern-sha256 (:source_sha256 ref)
         :fragment (select-keys fragment [:start :end :text :intent :target
                                          :rationale :relations])}))
    (map-indexed vector
                 (mapcat :fragments (get-in reading [:analysis :sentences]))))))

(defn selected-need [judgement]
  (let [families (get-in judgement [:decision :selection-certificate
                                     :target-policy-field :families])
        refusal (first
                 (for [family families
                       failure (:failures family)
                       :when (= :occurrence-squares-not-authored (:kind failure))]
                   {:target (:target-id family)
                    :excerpt-digest (:excerpt-digest failure)
                    :analysis-digest (:analysis-digest failure)}))]
    refusal))

(defn prompt [{:keys [target analysis-digest occurrences]}]
  (str "Author the four situation mappings for every cited pattern occurrence. "
       "G evaluates complete cascade policies; these mappings are upstream "
       "construction evidence, not atomic actions. For each occurrence return "
       "exactly I1, I2, G and B, each with a concise situation element and an "
       "evidence source explaining where that situation is observed. Do not "
       "invent offsets, hashes, theories, maps or policy identities; the machine "
       "constructs and validates those. Reply with exactly one JSON object: "
       "{schema:'wm/occurrence-square-maps-v1',target:'...',analysis_digest:'...',"
       "entries:[{occurrence_id:[pattern,fragmentIndex],maps:{I1:{element,source},"
       "I2:{element,source},G:{element,source},B:{element,source}}}]}.\n\n"
       (json/generate-string {:target target :analysis_digest analysis-digest
                              :occurrences occurrences} {:pretty true})))

(defn- keywordize-roles [maps]
  (into {} (map (fn [[k v]] [(keyword (name k)) v])) maps))

(defn- decode [raw]
  (handoff/response-json-object raw))

(defn repair!
  [{:keys [answer-fn author target-reading-root occurrence-square-root
           pattern-library-root]} judgement]
  (when-let [{:keys [target excerpt-digest analysis-digest] :as need}
             (selected-need judgement)]
    (let [reading (readings/current-reading
                   (or target-reading-root readings/default-root)
                   target excerpt-digest)]
      (cond
        (not= :current-candidate (:status reading))
        {:schema schema :status :error :kind :occurrence-square-reading-unavailable
         :need need}

        (not (fn? answer-fn))
        {:schema schema :status :error :kind :designated-author-unavailable
         :author author :need need}

        :else
        (let [resolver (registry/library-resolver
                        (or pattern-library-root registry/default-library-root))
              classified (mapv (fn [{:keys [pattern] :as occurrence}]
                                 (let [parts (square/pattern-parts (resolver pattern))
                                       missing (vec (for [part ["IF" "HOWEVER" "BECAUSE" "THEN"]
                                                          :when (str/blank? (get parts part))]
                                                      (keyword part)))]
                                   (assoc occurrence :pattern-parts parts
                                          :missing-parts missing
                                          :void? (boolean (seq missing)))))
                               (refs reading))
              occurrences (vec (remove :void? classified))
              voids (into {} (for [{:keys [occurrence-id pattern missing-parts]} classified
                                   :when (seq missing-parts)]
                               [occurrence-id {:occurrence-id occurrence-id
                                               :pattern pattern
                                               :missing-parts missing-parts
                                               :reason :pattern-part-missing}]))
              request {:schema :wm/occurrence-square-authoring-request-v1
                       :kind :occurrence-square-authoring :target target
                       :analysis-digest analysis-digest :occurrences occurrences
                       :author author}
              exchange (handoff/exchange {:contact answer-fn :request request
                                           :decode decode})
              answer (:value exchange)
              entries (:entries answer)
              by-id (into {} (map (juxt (comp vec :occurrence_id) identity)) entries)
              expected (set (map :occurrence-id occurrences))]
          (cond
            (not= :validated (:status exchange))
            {:schema schema :status :error :kind :occurrence-square-answer-invalid
             :answer (select-keys exchange [:reason :job-id :message])}

            (or (not= "wm/occurrence-square-maps-v1" (some-> (:schema answer) name))
                (not= target (:target answer))
                (not= analysis-digest (:analysis_digest answer))
                (not= expected (set (keys by-id))))
            {:schema schema :status :error :kind :occurrence-square-answer-unbound
             :expected expected :received (set (keys by-id))}

            :else
            (let [built
                  (into {}
                        (for [{:keys [occurrence-id pattern pattern-sha256]} occurrences
                              :let [text (resolver pattern)
                                    path (str "futon3/library/" pattern ".flexiarg")
                                    maps (keywordize-roles (:maps (get by-id occurrence-id)))
                                    sq (square/build-from-situation-maps
                                        pattern text {:path path :sha256 pattern-sha256} maps)]]
                          [occurrence-id {:occurrence-id occurrence-id
                                          :pattern pattern
                                          :pattern-sha256 pattern-sha256
                                          :square sq}]))
                  bad (first (for [[oid entry] built
                                   :let [v (square/validate (:square entry))]
                                   :when (not= :valid (:status v))]
                               {:occurrence-id oid :validation v}))]
              (if bad
                {:schema schema :status :error :kind :occurrence-square-construction-invalid
                 :detail bad}
                (let [published
                      (registry/publish!
                       (or occurrence-square-root registry/default-root)
                       {:target-id target
                        :excerpt-digest (:excerpt-digest reading)
                        :analysis-digest analysis-digest
                        :author author :entries built :voids voids}
                       resolver)]
                  (if (= registry/schema (:schema published))
                    {:schema schema :status :published :published true
                     :target target :analysis-digest analysis-digest
                     :count (count built) :digest (:digest published)
                     :job-id (:job-id exchange)}
                    {:schema schema :status :error
                     :kind :occurrence-square-publication-invalid
                     :detail published}))))))))))
