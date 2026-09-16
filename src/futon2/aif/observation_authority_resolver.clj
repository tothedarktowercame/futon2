(ns futon2.aif.observation-authority-resolver
  "Resolve externally commissioned observation references against a pinned index
  and an admitted-evidence manifest. This interface does not commission authority,
  acquire observations, or establish the authenticity of its caller's commission."
  (:require [clojure.string :as str]
            [futon2.aif.categorical-state-observation :as observation]
            [futon2.aif.evidence-manifest :as manifest])
  (:import (java.time Instant)))

(def index-schema :wm/observation-authority-index-v1)
(def reference-kinds #{:evidence :observer :review})
(def occurrence-keys [:run/id :cohort/id :attempt/id :checkpoint/ref])
(def time-keys [:action/started-at :action/completed-at :evidence/cutoff-at
                :disposition/recorded-at])

(defn- refuse! [reason path]
  (throw (ex-info "Observation authority resolution refused"
                  {:refusal reason :path path})))

(defn- demand! [pred reason path]
  (when-not pred (refuse! reason path)))

(defn- identity? [x]
  (and (or (string? x) (keyword? x) (symbol? x))
       (not (str/blank? (name x)))))

(defn- instant! [x path]
  (try (Instant/parse x)
       (catch Exception _ (refuse! :invalid-timestamp path))))

(defn- pointer! [p path]
  (demand! (and (map? p) (= #{:path :sha256} (set (keys p)))
                (string? (:path p)) (not (str/blank? (:path p)))
                (string? (:sha256 p)) (re-matches #"[0-9a-f]{64}" (:sha256 p)))
           :missing-source-pointer path)
  p)

(defn- expected! [expected]
  (demand! (map? expected) :expected-context-missing [:expected])
  (demand! (identity? (get-in expected [:subject :entity/id]))
           :missing-identity [:expected :subject :entity/id])
  (doseq [k occurrence-keys]
    (demand! (identity? (get-in expected [:point k])) :missing-identity [:expected :point k]))
  (doseq [k time-keys] (instant! (get-in expected [:point k]) [:expected :point k]))
  (demand! (#{:test :production} (:authority/scope expected))
           :authority-scope-missing [:expected :authority/scope])
  (doseq [k [:config/id :revision]]
    (demand! (identity? (get-in expected [:authority/provenance k]))
             :authority-provenance-missing [:expected :authority/provenance k])))

(defn- entry! [entry admitted cutoff]
  (let [{:keys [kind ref source]} entry]
    (demand! (and (map? entry)
                  (= (if (= :evidence kind)
                       #{:kind :ref :source :evidence/id}
                       #{:kind :ref :source})
                     (set (keys entry))))
             :authority-entry-invalid [:entries])
    (demand! (reference-kinds kind) :unsupported-authority-kind [:entries :kind])
    (demand! (identity? ref) :invalid-authority-reference [:entries :ref])
    (pointer! source [:entries :source])
    (when (= :evidence kind)
      (let [e (get admitted (:evidence/id entry))]
        (demand! e :evidence-not-admitted [:entries :evidence/id])
        (demand! (= source {:path (:source-path e) :sha256 (:sha256 e)})
                 :evidence-admission-mismatch [:entries :source])
        (demand! (not (.isAfter (instant! (:admitted-at e) [:entries :admitted-at]) cutoff))
                 :evidence-admitted-after-cutoff [:entries :admitted-at])))
    entry))

(defn build-resolver!
  "Build validator options from EXTERNAL inputs, never from an observation.

  Inputs: :index-source {:path :sha256}, :manifest (validated again here),
  :expected (the validator's full context), :commission/ref (required external
  commission identity), and optional :io-opts for isolated read controls.

  The pinned index is {:schema index-schema :index/id ID :expected EXPECTED
  :manifest-sha256 DIGEST :entries [...]}. Each entry is
  {:kind :observer|:review|:evidence :ref REF :source {:path PATH :sha256 HASH}};
  evidence entries additionally carry :evidence/id matching an admitted entry.
  The index binds the otherwise subject-free manifest to the exact context.

  Returns {:resolver (fn [kind ref] ...) :expected ... :io-opts ... :identities ...}.
  Pass this map unchanged as validate-observation!'s second argument. The closure
  captures only validated external inputs; candidates supply opaque references.
  Record bytes, roles and review subjects remain the existing validator's checks.
  A commission reference is retained provenance, NOT proof of institutional
  authorization. The commissioning caller must establish that independently."
  [{:keys [index-source manifest expected io-opts] :as inputs}]
  (demand! (and (map? inputs)
                (every? #{:index-source :manifest :expected :commission/ref :io-opts}
                        (keys inputs)))
           :resolver-input-invalid [:inputs])
  (demand! (identity? (:commission/ref inputs)) :commission-required [:commission/ref])
  (expected! expected)
  (pointer! index-source [:index-source])
  (let [admitted (manifest/validate-manifest manifest)
        index (observation/read-pinned-form! index-source io-opts)]
    (demand! (and (map? index)
                  (= #{:schema :index/id :expected :manifest-sha256 :entries} (set (keys index)))
                  (= index-schema (:schema index)) (identity? (:index/id index))
                  (vector? (:entries index)))
             :authority-index-invalid [:index])
    (demand! (= expected (:expected index)) :authority-context-mismatch [:index :expected])
    (demand! (= (:manifest-sha256 admitted) (:manifest-sha256 index))
             :authority-manifest-mismatch [:index :manifest-sha256])
    (let [by-id (into {} (map (juxt :evidence/id identity)) (:entries admitted))
          cutoff (instant! (get-in expected [:point :evidence/cutoff-at])
                           [:expected :point :evidence/cutoff-at])
          entries (mapv #(entry! % by-id cutoff) (:entries index))
          groups (group-by (juxt :kind :ref) entries)]
      (demand! (every? #(= 1 (count %)) (vals groups))
               :ambiguous-authority-binding [:index :entries])
      {:resolver (fn [kind ref]
                   (demand! (reference-kinds kind) :unsupported-authority-kind [:authority :kind])
                   (demand! (identity? ref) :invalid-authority-reference [:authority :ref])
                   (let [entry (first (get groups [kind ref]))]
                     (demand! entry :authority-not-found [:authority kind ref])
                     (:source entry)))
       :expected expected
       :io-opts io-opts
       :identities {:commission/ref (:commission/ref inputs)
                    :index/id (:index/id index) :index-source index-source
                    :manifest-sha256 (:manifest-sha256 admitted)
                    :subject (:subject expected) :point (:point expected)
                    :authority/scope (:authority/scope expected)
                    :authority/provenance (:authority/provenance expected)}})))
