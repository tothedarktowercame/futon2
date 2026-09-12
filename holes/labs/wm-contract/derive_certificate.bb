#!/usr/bin/env bb
;; Mechanical runtime-certificate-v1 emitter. Route classification is delegated
;; to src/futon2/aif/run4_route_conformance.clj, the same reusable core cited by
;; u49_route_transcribe.bb; do not copy its disposition rules here.

(load-file "src/futon2/aif/run4_route_conformance.clj")

(require '[babashka.fs :as fs]
         '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[futon2.aif.run4-route-conformance :as route])

(def required-scope
  #{:route-conformance :deliverable-chain :selection-enaction
    :realised-equation-bindings :mandatory-negative-scope
    :not-r1-r17 :not-r2-r17 :not-fundamentals-inhabitants
    :not-above-witnessed-rung :not-task-success :not-mission-closure})

(defn sha256-bytes [bs]
  (let [d (.digest (java.security.MessageDigest/getInstance "SHA-256") bs)]
    (apply str (map #(format "%02x" (bit-and % 0xff)) d))))

(defn sha256-file [path] (sha256-bytes (fs/read-all-bytes path)))

(defn canonical [x]
  (cond
    (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                   (map (fn [[k v]] [k (canonical v)])) x)
    (set? x) (mapv canonical (sort-by pr-str x))
    (sequential? x) (mapv canonical x)
    :else x))

(defn value-sha [x]
  (sha256-bytes (.getBytes (pr-str (canonical x)) "UTF-8")))

(defn refusal [class why & [data]]
  {:certificate/refused true :refusal/class class :why why
   :refusal/data (or data {})})

(defn- fail! [class why & [data]]
  (throw (ex-info why {:refusal/class class :refusal/data (or data {})})))

(defn read-edn-file [path]
  (try (edn/read-string (slurp path))
       (catch Throwable t
         (fail! :record-unreadable (str "cannot read " path ": " (.getMessage t))
                {:path path}))))

(defn read-json-file [path]
  (try (json/parse-string (slurp path) true)
       (catch Throwable t
         (fail! :contract-unreadable (str "cannot read " path ": " (.getMessage t))
                {:path path}))))

(defn- checkpoint-source [path record]
  {:role (:checkpoint/type record) :path (str path) :sha256 (sha256-file path)
   :schema (:event/schema-version record) :recorded-at (:recorded-at record)})

(defn- validate-checkpoints! [paths records expected-hashes]
  (when-not (= 7 (count records))
    (fail! :checkpoint-chain-incomplete "certificate requires checkpoints 001 through 007"
           {:observed (count records)}))
  (let [seqs (mapv :event/sequence records)
        types (mapv :checkpoint/type records)
        expected-types [:time-step :selection :construction :dispatch :build
                        :adjudication :closed]
        identities (set (map (juxt :cohort/id :attempt/id) records))]
    (when-not (= (vec (range 1 8)) seqs)
      (fail! :checkpoint-sequence-invalid "checkpoint sequence is removed or reordered"
             {:observed seqs}))
    (when-not (= expected-types types)
      (fail! :checkpoint-type-invalid "checkpoint types do not match 001 through 007"
             {:observed types}))
    (when-not (= 1 (count identities))
      (fail! :checkpoint-identity-mismatch "checkpoints do not share cohort and attempt"
             {:observed identities}))
    (doseq [path paths :let [expected (get expected-hashes (str path))] :when expected]
      (when-not (= expected (sha256-file path))
        (fail! :source-digest-mismatch "checkpoint bytes do not match expected digest"
               {:path (str path) :expected expected :observed (sha256-file path)})))))

(defn- validate-checkpoint-times! [records]
  (when-not (apply <= (map #(compare %1 %2) (map :recorded-at records)
                           (rest (map :recorded-at records))))
    (fail! :checkpoint-time-invalid "checkpoint timestamps are not monotone")))

(defn- construction-judgment [records]
  (get-in (nth records 2) [:payload :judgment]))

(defn- require-green! [label result]
  (when-not (and (map? result) (true? (:ok result)) (empty? (:findings result)))
    (fail! :validator-result-invalid (str (name label) " validator is absent or not green")
           {:validator label :observed result})))

(defn- validate-deliverable! [judgment]
  (let [fold-output (:fold-output judgment)
        wiring (:wiring judgment)
        shape (:shape-validation judgment)
        correspondence (:correspondence-validation judgment)]
    (when (or (nil? wiring) (nil? fold-output))
      (fail! :deliverable-chain-incomplete "construction wiring/fold-output is nil"))
    (when (or (nil? (:cascade judgment))
              (nil? (:fold/execution fold-output))
              (nil? (:fold/authority fold-output)))
      (fail! :deliverable-chain-incomplete
             "cascade, fold execution, or fold authority is absent"))
    (when-not (= wiring (:wiring fold-output))
      (fail! :deliverable-digest-mismatch "judgment wiring differs from fold-output wiring"))
    (require-green! :shape shape)
    (require-green! :correspondence correspondence)
    {:fold-output fold-output :wiring wiring :shape shape :correspondence correspondence}))

(defn- validate-selection! [selection]
  (case (:verdict selection)
    :match (when-not (= (:selected selection) (:enacted selection))
             (fail! :selection-enaction-invalid "match verdict carries unequal actions"))
    :typed-divergence (when-not (and (keyword? (:class selection))
                                     (seq (str (:grounds selection)))
                                     (:selected selection) (:enacted selection))
                        (fail! :divergence-grounds-missing
                               "typed divergence requires class, grounds and both actions"))
    (fail! :selection-enaction-invalid "selection/enaction verdict absent or untyped"
           {:observed selection})))

(defn- reassemble-route! [route-record control-map]
  (let [raw (or (:route route-record) (:wm/route route-record))
        modern? (and (vector? raw) (every? #(contains? % :fromNode) raw))
        nodes (if modern?
                (vec (cons (str (:fromNode (first raw))) (map #(str (:toNode %)) raw)))
                (mapv (comp str :node) raw))
        hops (mapv vec (partition 2 1 nodes))]
    (when (empty? nodes) (fail! :route-empty "run route is empty"))
    (when (and modern?
               (not (every? true? (map (fn [[a b]]
                                         (= (:toNode a) (:fromNode b)))
                                       (partition 2 1 raw)))))
      (fail! :route-discontinuous "route edge carrier is discontinuous"))
    (when-not (route/conforms-routes? control-map [nodes])
      (fail! :route-nonconformant "route contains an unmapped or refuted hop"
             {:nodes nodes :hops hops}))
    {:routes [nodes]
     :hops (mapv (fn [[[from to] raw]]
                   {:from from :to to :via (:via raw)
                    :at (or (:at_ raw) (:at raw))})
                 (map vector hops (if modern? raw (rest raw))))}))

(defn- declarations [holes bundle]
  (let [expand (fn [contract]
                 (map #(-> %
                           (assoc :contract-id (:contract-id contract))
                           (update :source merge (:source contract)))
                      (:declarations contract)))]
    (vec (concat (expand holes) (mapcat expand (:contracts bundle))))))

(defn- declaration-matches [decls lean-name]
  (filterv #(or (= lean-name (:name %))
                (str/ends-with? (:name %) (str "." lean-name))) decls))

(defn- equation-bindings! [registry holes bundle tick-equations manifest-sha
                           declaration-revisions]
  (let [rows (into {} (map (juxt :id identity)) (:equations registry))
        decls (declarations holes bundle)]
    (mapv
     (fn [{:keys [registry-row source-sha256 pointer]}]
       (let [row (get rows registry-row)]
         (when-not row (fail! :registry-row-missing "tick equation is absent from registry"
                              {:registry-row registry-row}))
         (when-not (or (= true (:realised row))
                       (and (keyword? (:status row))
                            (str/starts-with? (name (:status row)) "realised")))
           (fail! :registry-row-not-realised "tick equation is not marked realised"
                  {:registry-row registry-row :status (:status row)}))
         (let [matches (declaration-matches decls (:lean row))]
           (when-not (= 1 (count matches))
             (fail! (if (empty? matches) :declaration-missing :declaration-ambiguous)
                    "realised registry row must resolve to one checked declaration"
                    {:registry-row registry-row :lean (:lean row)
                     :matches (mapv :name matches)}))
           (let [d (first matches)]
             (when-not (= (get declaration-revisions registry-row)
                          (get-in d [:source :git-sha]))
               (fail! :declaration-revision-mismatch
                      "checked declaration is not at the certificate revision"
                      {:registry-row registry-row
                       :expected (get declaration-revisions registry-row)
                       :observed (get-in d [:source :git-sha])}))
             {:registry-row registry-row :node (:node row)
              :realisation (or (:status row) (:realised row))
              :tick-path-evidence {:source-sha256 source-sha256 :pointer pointer}
              :declaration {:full-name (:name d) :kind (:kind d)
                            :contract-id (:contract-id d)
                            :module (get-in d [:source :module])
                            :source-git-sha (get-in d [:source :git-sha])
                            :source-sha256 (get-in d [:source :sha256])}
              :contract-manifest-sha256 manifest-sha}))))
     tick-equations)))

(defn derive-certificate
  "Pure relative to the supplied immutable file bytes. Returns a certificate or
   a typed refusal; never writes output and never consults a live service."
  [{:keys [checkpoint-paths route-record-path control-map-path registry-path
           holes-contract-path machine-contracts-path contract-manifest-path
           expected-source-hashes tick-equations declaration-revisions
           checker scope]}]
  (try
    (let [_ (doseq [[path expected] expected-source-hashes]
              (when-not (= expected (sha256-file path))
                (fail! :source-digest-mismatch "source bytes do not match expected digest"
                       {:path path :expected expected :observed (sha256-file path)})))
          records (mapv read-edn-file checkpoint-paths)
          _ (validate-checkpoints! checkpoint-paths records expected-source-hashes)
          judgment (construction-judgment records)
          selection-judgment (get-in (nth records 1) [:payload :judgment])
          _ (when-not (map? selection-judgment)
              (fail! :deliverable-chain-incomplete "G/selection record is absent"))
          {:keys [fold-output wiring shape correspondence]} (validate-deliverable! judgment)
          _ (validate-checkpoint-times! records)
          selection (:selection-enaction judgment)
          _ (validate-selection! selection)
          close (last records)
          _ (when (= :guardrail-refusal (get-in close [:payload :judgment :outcome]))
              (fail! :terminal-refusal "guardrail-refusal is not a completed deliverable"))
          scope-set (set scope)
          _ (when-not (= required-scope scope-set)
              (fail! :mandatory-scope-missing "certificate scope is not the mandatory exact set"
                     {:missing (vec (sort (remove scope-set required-scope)))
                      :extra (vec (sort (remove required-scope scope-set)))}))
          route-record (read-edn-file route-record-path)
          control-map (read-edn-file control-map-path)
          route-data (reassemble-route! route-record control-map)
          registry (read-edn-file registry-path)
          holes (read-json-file holes-contract-path)
          bundle (read-json-file machine-contracts-path)
          manifest-sha (sha256-file contract-manifest-path)
          binds (equation-bindings! registry holes bundle tick-equations manifest-sha
                                    declaration-revisions)
          source-sha (sha256-file (nth checkpoint-paths 2))]
      {:certificate/schema :wm/runtime-certificate-v1
       :run {:id (:run/id route-record) :series-id (:cohort/id (first records))
             :attempt-id (:attempt/id (first records))
             :started-at (:recorded-at (first records)) :closed-at (:recorded-at close)
             :outcome (get-in close [:payload :judgment :outcome])}
       :sources (mapv checkpoint-source checkpoint-paths records)
       :route (merge {:trace-record-sha256 (sha256-file route-record-path)
                      :drawn-wiring {:path control-map-path
                                     :sha256 (sha256-file control-map-path)}} route-data)
       :deliverable-chain
       {:g-record {:source-sha256 (sha256-file (nth checkpoint-paths 1))
                   :pointer [:payload :judgment]
                   :sha256 (value-sha selection-judgment)}
        :cascade {:source-sha256 source-sha :pointer [:payload :judgment :cascade]
                  :sha256 (value-sha (:cascade judgment))}
        :wiring {:source-sha256 source-sha :pointer [:payload :judgment :wiring]
                 :sha256 (value-sha wiring)}
        :fold-output {:source-sha256 source-sha :pointer [:payload :judgment :fold-output]
                      :sha256 (value-sha fold-output)}
        :fold-execution {:source-sha256 source-sha
                         :pointer [:payload :judgment :fold-output :fold/execution]}
        :fold-authority {:source-sha256 source-sha
                         :pointer [:payload :judgment :fold-output :fold/authority]}
        :shape-validation (assoc shape :input-sha256 (value-sha fold-output))
        :correspondence-validation
        (assoc correspondence :cascade-sha256 (value-sha (:cascade judgment))
               :fold-output-sha256 (value-sha fold-output))
        :selection-enaction (assoc selection :source-sha256 source-sha
                                   :pointer [:payload :judgment :selection-enaction])}
       :equation-bindings binds
       :checker checker
       :scope {:checked (vec (sort scope-set))
               :lean-attestation :suspended-vocabulary}})
    (catch clojure.lang.ExceptionInfo e
      (refusal (:refusal/class (ex-data e)) (.getMessage e)
               (:refusal/data (ex-data e))))
    (catch Throwable t
      (refusal :unexpected-input-error (.getMessage t)
               {:exception-class (.getName (class t))}))))

(defn emit! [result edn-path json-path]
  (when (:certificate/refused result)
    (fail! (:refusal/class result) (:why result) (:refusal/data result)))
  (spit edn-path (str (pr-str (canonical result)) "\n"))
  (spit json-path (str (json/generate-string (canonical result)) "\n"))
  result)

(when (= *file* (System/getProperty "babashka.file"))
  (let [[config-path edn-out json-out] *command-line-args*
        result (derive-certificate (read-edn-file config-path))]
    (if (:certificate/refused result)
      (do (println (pr-str result)) (System/exit 2))
      (do (emit! result edn-out json-out)
          (println (pr-str {:ok true :edn edn-out :json json-out
                            :certificate-sha256 (sha256-file edn-out)}))))))
