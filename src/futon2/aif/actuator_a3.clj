(ns futon2.aif.actuator-a3
  "Thin, content-free A3 actuator.

  Extracts a build package from a reviewed fold-turn deposit, dispatches the
  package to a builder, and verifies returned evidence refs before any dial is
  counted. The executor does not interpret domain content and does not mutate
  substrate."
  (:require [babashka.http-client :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.fold-escrow :as esc]
            [futon2.aif.mission-registry :as missions])
  (:import [java.time Instant]))

(def default-log-file "/home/joe/code/futon2/logs/actuator-a3.log")
(def default-agency-send "/home/joe/code/futon3c/scripts/agency_send.py")
(def default-drawbridge-url "http://localhost:6768/eval")
(def default-admin-token-path "/home/joe/code/futon3c/.admintoken")

(def reviewed-endpoint-bindings
  {"futon5a-d/mission/learning-loop"
   [{:endpoint "CapabilityVocabulary"
     :kind :entity
     :type :capability}
    {:endpoint "CapabilityHypergraph"
     :kind :hyperedge
     :type :capability/*
     :endpoint-types [:capability :mission/doc]}]})

(defn- admin-token []
  (try (str/trim (slurp default-admin-token-path))
       (catch Throwable _ nil)))

(declare evidence-resolves?)

(def a3-corpus-ids
  ["ft-learning-loop-010"
   "ft-chipwitz-corps-001"
   "ft-futonzero-generative-011"
   "ft-pattern-mining-011"])

(defn mission-key [mission-id]
  (missions/mission-target-id mission-id))

(defn deposits-by-id
  ([] (deposits-by-id nil))
  ([deposit-dir]
   (let [{:keys [deposits rejected]} (if deposit-dir
                                       (esc/load-deposits deposit-dir)
                                       (esc/load-deposits))]
     (when (seq rejected)
       (throw (ex-info "actuator-a3: rejected deposits in corpus load"
                       {:rejected rejected})))
     (into {} (map (juxt :fold-turn/id identity) deposits)))))

(defn typespec
  "Content-free typespec extraction. Prefer the explicit arrow candidate, then
  older hole-from-sorry-psi records, then the terminal want-signature marker."
  [deposit]
  (cond
    (:arrow-candidate deposit)
    {:source :arrow-candidate
     :spec (:arrow-candidate deposit)}

    (:hole deposit)
    {:source :hole
     :spec (:hole deposit)}

    (seq (filter #(= :want-signature (:discharges %))
                 (get-in deposit [:wiring :terminals])))
    {:source :want-signature
     :spec {:format :want-signature
            :terminals (vec (filter #(= :want-signature (:discharges %))
                                    (get-in deposit [:wiring :terminals])))}}

    :else nil))

(defn structure-spec [deposit]
  (let [wiring (:wiring deposit)
        holes (vec (or (get-in deposit [:turn :answer :policy-holes]) []))]
    (when (and (map? wiring)
               (seq (:nodes wiring))
               (or (contains? wiring :hyperedges)
                   (contains? wiring :terminals)))
      {:wiring wiring
       :policy-holes holes})))

(defn endpoint-bindings
  "Endpoint bindings are authored by the deposit or reviewed grounding map, never
  by the builder result. These bindings determine the substrate proof queries."
  [deposit]
  (vec (or (:endpoint-bindings deposit)
           (get reviewed-endpoint-bindings (:mission deposit))
           (get reviewed-endpoint-bindings (:fold-turn/id deposit))
           [])))

(defn extract-build-package
  [deposit]
  (let [ts (typespec deposit)
        ss (structure-spec deposit)
        bindings (endpoint-bindings deposit)
        missing (cond-> []
                  (nil? ts) (conj {:field :typespec
                                   :reason :missing-typespec
                                   :class :spec-too-weak
                                   :detail "no :arrow-candidate, :hole, or want-signature terminal"})
                  (nil? ss) (conj {:field :structure-spec
                                   :reason :missing-structure
                                   :class :spec-too-weak
                                   :detail "no top-level :wiring with nodes and terminal/hyperedge shape"}))]
    (if (seq missing)
      {:ok? false
       :fold-turn/id (:fold-turn/id deposit)
       :mission (:mission deposit)
       :missing missing}
      {:ok? true
       :fold-turn/id (:fold-turn/id deposit)
       :mission (:mission deposit)
       :mission-key (mission-key (:mission deposit))
       :typespec ts
       :structure-spec ss
       :policy-holes (:policy-holes ss)
       :endpoint-bindings bindings})))

(defn proof-query
  "Derive the datalog query from an upstream endpoint binding. Builder claims do
  not participate in this derivation."
  [{:keys [kind type endpoint-types]}]
  (case kind
    :entity
    {:find '[e]
     :where [['e :entity/type type]]
     :limit 1}

    :hyperedge
    (let [endpoint-syms (mapv #(symbol (str "ep" %))
                              (range (count endpoint-types)))]
      {:find '[e]
       :where (vec (concat [['e :hx/type type]]
                           (mapcat (fn [ep-sym ep-type]
                                     [['e :hx/endpoints ep-sym]
                                      [ep-sym :entity/type ep-type]])
                                   endpoint-syms
                                   endpoint-types)))
       :limit 1})

    (throw (ex-info "unknown endpoint binding kind"
                    {:kind kind
                     :type type
                     :endpoint-types endpoint-types}))))

(defn proof-form [query]
  (pr-str `(do
             (require (quote xtdb.api))
             (xtdb.api/q (xtdb.api/db (:node @futon3c.dev/!f1-sys))
                         (quote ~query)))))

(defn endpoint-proof-ref [binding]
  {:kind :drawbridge
   :form (proof-form (proof-query binding))})

(defn endpoint-inhabitation
  ([binding] (endpoint-inhabitation binding {}))
  ([binding opts]
   (let [query (proof-query binding)
         ref {:kind :drawbridge :form (proof-form query)}
         inhabited? (evidence-resolves? ref opts)]
     (assoc binding
            :query query
            :evidence-ref ref
            :inhabited? inhabited?
            :reason (when-not inhabited? :not-inhabited)))))

(defn endpoint-snapshot
  ([bindings] (endpoint-snapshot bindings {}))
  ([bindings opts]
   (mapv #(endpoint-inhabitation % opts) bindings)))

(defn endpoint-dial
  [snapshot]
  (let [total (count snapshot)
        inhabited (count (filter :inhabited? snapshot))]
    {:inhabited inhabited
     :total total
     :discharged? (and (pos? total) (= inhabited total))}))

(defn endpoint-name-set [snapshot pred]
  (set (keep (fn [{:keys [endpoint] :as row}]
               (when (pred row) endpoint))
             snapshot)))

(defn endpoint-dial-review
  [{:keys [before after] :or {before [] after []}}]
  (let [before-inhabited (endpoint-name-set before :inhabited?)
        after-inhabited (endpoint-name-set after :inhabited?)
        newly-inhabited (vec (sort (set/difference after-inhabited
                                                   before-inhabited)))
        dial (endpoint-dial after)]
    (assoc dial
           :dial-moved? (boolean (seq newly-inhabited))
           :newly-inhabited newly-inhabited
           :endpoints after)))

(defn build-prompt [package]
  (str "A3 BUILDER TASK — content-free fold blueprint execution.\n\n"
       "Build this reviewed fold-turn blueprint. Use handoffs as needed.\n\n"
       "ACCEPTANCE DIAL: write the bound endpoints into substrate-2. The executor "
       "will derive Drawbridge xtdb.api/q proofs from :endpoint-bindings below; "
       "builder-supplied evidence refs or file-exists claims do not move this "
       "dial. Use the sanctioned Drawbridge write form: xtdb.api/submit-tx on "
       "(:node @futon3c.dev/!f1-sys), then xtdb.api/await-tx.\n\n"
       "Optional downstream bookkeeping: you may declare :closes-mission-hole "
       "with a resolving :evidence-ref, but do NOT edit the mission doc. Only the "
       "executor may close doc markers after the substrate endpoint dial moves.\n\n"
       "BUILD PACKAGE EDN:\n"
       (pr-str package)
       "\n\nOptional evidence-ref shapes for doc bookkeeping only:\n"
       "  {:kind :file-exists :path \"/abs/path\"}\n"
       "  {:kind :file-contains :path \"/abs/path\" :pattern \"literal text\"}\n"
       "  {:kind :drawbridge :form \"(read-only-clojure-form)\"}\n"))

(defn dispatch!
  [{:keys [package builder reviewer agency-send]
    :or {builder "zai-3"
         reviewer "claude-4"
         agency-send default-agency-send}}]
  (let [cmd ["python3" agency-send "--to" builder "--kind" "bell" "--from" reviewer
             "--mission" (:mission package)]
        pb (ProcessBuilder. ^java.util.List cmd)]
    (.directory pb (io/file "/home/joe/code"))
    (.redirectErrorStream pb true)
    (let [p (.start pb)]
      (spit (.getOutputStream p) (build-prompt package))
      (.close (.getOutputStream p))
      (let [out (slurp (.getInputStream p))
            exit (.waitFor p)]
        {:cmd cmd :exit exit :out out}))))

(defn file-exists-resolves? [{:keys [path]}]
  (and (string? path) (.exists (io/file path))))

(defn file-contains-resolves? [{:keys [path pattern]}]
  (and (file-exists-resolves? {:path path})
       (string? pattern)
       (str/includes? (slurp path) pattern)))

(defn result-truthy?
  "A witness query resolves only if its RESULT witnesses existence: reject
  nil/false/0/empty-collection/blank-string. A query that 'ran fine' but found
  nothing (count 0, empty seq) must NOT count as a resolved witness — else the
  dial can be gamed with a valid query that finds no artifact."
  [v]
  (cond
    (nil? v) false
    (false? v) false
    (number? v) (not (zero? v))
    (string? v) (not (str/blank? v))
    (coll? v) (boolean (seq v))
    :else true))

(defn drawbridge-resolves?
  ([ref] (drawbridge-resolves? ref default-drawbridge-url))
  ([{:keys [form]} drawbridge-url]
   (if-not (and (string? form) (not (str/blank? form)))
     false
     (let [tok (admin-token)
           resp (http/post drawbridge-url
                           {:headers (cond-> {"Content-Type" "text/plain"}
                                       tok (assoc "x-admin-token" tok))
                            :body form
                            :throw false
                            :timeout 10000})]
       (and (= 200 (:status resp))
            (let [body (try (edn/read-string (:body resp))
                            (catch Throwable _ (:body resp)))]
              (if (map? body)
                (and (not (false? (:ok body))) (result-truthy? (:value body)))
                (result-truthy? body))))))))

(defn evidence-resolves?
  ([evidence-ref] (evidence-resolves? evidence-ref {}))
  ([evidence-ref {:keys [resolver drawbridge-url]}]
   (cond
     (nil? evidence-ref) false
     (fn? resolver) (boolean (resolver evidence-ref))
     (= :file-exists (:kind evidence-ref)) (file-exists-resolves? evidence-ref)
     (= :file-contains (:kind evidence-ref)) (file-contains-resolves? evidence-ref)
     (= :drawbridge (:kind evidence-ref)) (drawbridge-resolves? evidence-ref
                                                               (or drawbridge-url
                                                                   default-drawbridge-url))
     :else false)))

(defn verify-closure
  [closure opts]
  (let [evidence-ref (:evidence-ref closure)
        resolved? (evidence-resolves? evidence-ref opts)]
    (assoc closure
           :witness/resolved? resolved?
           :closure/accepted? (boolean resolved?)
           :closure/reject-reason (cond
                                    (nil? evidence-ref) :missing-evidence-ref
                                    (not resolved?) :unresolved-evidence-ref
                                    :else nil))))

(defn verify-builder-result
  "Verify builder output and apply the endpoint-gated dial rule. Endpoint
  bindings are executor/fold supplied; builder evidence refs are optional
  bookkeeping and cannot move the endpoint dial."
  [{:keys [closures doc-advancement endpoint-bindings before-snapshot
           after-snapshot]
    :as result} opts]
  (let [verified (mapv #(verify-closure % opts) (or closures []))
        accepted (filterv :closure/accepted? verified)
        endpoint-mode? (or (seq endpoint-bindings) (seq before-snapshot)
                           (seq after-snapshot))]
    (if endpoint-mode?
      (let [after (or after-snapshot (endpoint-snapshot endpoint-bindings opts))
            review (endpoint-dial-review {:before (or before-snapshot [])
                                          :after after})]
        (assoc result
               :closures verified
               :accepted-closures accepted
               :rejected-closures (filterv (complement :closure/accepted?) verified)
               :endpoint-review review
               :dial-moved? (:dial-moved? review)
               :dial-counted-closures (:inhabited review)))
      (let [closed (vec (:holes-closed doc-advancement))
            dial-moved? (seq closed)]
        (assoc result
               :closures verified
               :accepted-closures accepted
               :rejected-closures (filterv (complement :closure/accepted?) verified)
               :dial-moved? (boolean dial-moved?)
               :dial-counted-closures (if dial-moved? (count closed) 0))))))

(defn doc-open-hole-count
  "Small file-local dial for executor-owned advancement. The live WM mission
  count still comes from mission-registry; this counts the concrete unchecked
  task markers the executor is allowed to close in a doc fixture or doc file."
  [text]
  (count (re-seq #"(?m)^\s*[-*]\s+\[\s\]\s+" text)))

(defn open-hole-lines
  [text]
  (->> (str/split-lines text)
       (filter #(re-find #"^\s*[-*]\s+\[\s\]\s+" %))
       vec))

(defn- close-marker-line [line marker]
  (when (and (str/includes? line marker)
             (re-find #"^\s*[-*]\s+\[\s\]\s+" line))
    (str/replace-first line #"\[\s\]" "[x]")))

(defn advance-mission-doc!
  "Executor-only doc advancement. A closure closes a mission-doc hole iff:
  1. its evidence-ref resolves via the existing witness gate, and
  2. :closes-mission-hole appears verbatim on an unchecked mission-doc line.

  The builder never gets to move this dial directly."
  [mission-doc-path closures opts]
  (let [before-text (slurp mission-doc-path)
        before-count (doc-open-hole-count before-text)]
    (loop [remaining closures
           text before-text
           closed []
           rejected []]
      (if-not (seq remaining)
        (do
          (spit mission-doc-path text)
          {:mission-doc-path mission-doc-path
           :before-open-hole-count before-count
           :after-open-hole-count (doc-open-hole-count text)
           :holes-closed (vec closed)
           :rejected (vec rejected)})
        (let [closure (first remaining)
              verified (verify-closure closure opts)
              marker (:closes-mission-hole closure)
              reject (fn [reason]
                       (assoc (select-keys closure [:hole-id :closes-mission-hole :evidence-ref])
                              :reason reason))]
          (cond
            (not (:witness/resolved? verified))
            (recur (rest remaining)
                   text
                   closed
                   (conj rejected (reject (:closure/reject-reason verified))))

            (not (and (string? marker) (not (str/blank? marker))))
            (recur (rest remaining)
                   text
                   closed
                   (conj rejected (reject :missing-closes-mission-hole)))

            (not (str/includes? text marker))
            (recur (rest remaining)
                   text
                   closed
                   (conj rejected (reject :hole-text-not-in-doc)))

            :else
            (let [lines (str/split-lines text)
                  idx (first (keep-indexed (fn [i line]
                                             (when (close-marker-line line marker)
                                               i))
                                           lines))]
              (if (nil? idx)
                (recur (rest remaining)
                       text
                       closed
                       (conj rejected (reject :hole-already-closed)))
                (let [new-line (close-marker-line (nth lines idx) marker)
                      new-text (str (str/join "\n" (assoc (vec lines) idx new-line))
                                    (when (str/ends-with? text "\n") "\n"))]
                  (recur (rest remaining)
                         new-text
                         (conj closed (assoc (select-keys closure [:hole-id :closes-mission-hole :evidence-ref])
                                             :line-before (nth lines idx)
                                             :line-after new-line))
                         rejected))))))))))

(defn next-feedback [rejected holes-remaining]
  (str "A3 review-partial retry input. The executor did not count rejected "
       "closures toward the dial. Return machine-checkable evidence only.\n\n"
       "Rejected closures EDN:\n"
       (pr-str (vec rejected))
       "\n\nRemaining unchecked mission-doc holes EDN:\n"
       (pr-str (vec holes-remaining))
       "\n\nFor each retry closure include :evidence-ref that resolves and "
       ":closes-mission-hole copied verbatim from one remaining unchecked line."))

(defn next-endpoint-feedback [endpoints]
  (let [remaining (filterv (complement :inhabited?) endpoints)]
    (str "A3 review-partial retry input. The executor did not count builder "
         "claims toward the dial. The remaining endpoints must be made "
         "inhabited in substrate-2 so the executor-derived Drawbridge queries "
         "return non-empty results.\n\n"
         "Remaining endpoint proofs EDN:\n"
         (pr-str (mapv #(select-keys % [:endpoint :kind :type :endpoint-types
                                        :query :reason])
                       remaining)))))

(defn review-partial
  [{:keys [mission-doc-path closures endpoint-bindings before after]} opts]
  (if (or (seq endpoint-bindings) (seq before) (seq after))
    (let [before-snapshot (or before [])
          after-snapshot (or after (endpoint-snapshot endpoint-bindings opts))
          dial (endpoint-dial-review {:before before-snapshot
                                      :after after-snapshot})
          resolved (filterv :inhabited? after-snapshot)
          rejected (mapv (fn [{:keys [reason] :as row}]
                           (assoc (select-keys row [:endpoint :kind :type
                                                    :endpoint-types :query])
                                  :reason (or reason :not-inhabited)))
                         (remove :inhabited? after-snapshot))
          report (assoc dial
                        :resolved resolved
                        :rejected rejected
                        :endpoints-remaining (count rejected))]
      (assoc report :next-feedback (next-endpoint-feedback after-snapshot)))
    (let [adv (advance-mission-doc! mission-doc-path closures opts)
          after-text (slurp mission-doc-path)
          holes-remaining (open-hole-lines after-text)
          report {:resolved (:holes-closed adv)
                  :rejected (:rejected adv)
                  :holes-closed (:holes-closed adv)
                  :holes-remaining holes-remaining
                  :before-open-hole-count (:before-open-hole-count adv)
                  :after-open-hole-count (:after-open-hole-count adv)
                  :dial-moved? (boolean (seq (:holes-closed adv)))}]
      (assoc report :next-feedback (next-feedback (:rejected report) holes-remaining)))))

(defn mission-open-hole-count [mission-id]
  (:open-hole-count (missions/mission-status mission-id)))

(defn log! [log-file record]
  (.mkdirs (.getParentFile (io/file log-file)))
  (spit log-file (str (pr-str (assoc record :at (str (Instant/now)))) "\n")
        :append true))

(defn render-package [package]
  (pr-str (select-keys package
                       [:fold-turn/id :mission :mission-key :typespec
                        :structure-spec :policy-holes :endpoint-bindings
                        :ok? :missing])))

(defn parse-args [args]
  (loop [args args
         opts {:deposit-ids []}]
    (if (empty? args)
      opts
      (let [[a b & more] args]
        (case a
          "--dry-run" (recur (rest args) (assoc opts :dry-run? true))
          "--all-corpus" (recur (rest args) (assoc opts :all-corpus? true))
          "--deposit" (recur more (update opts :deposit-ids conj b))
          "--deposit-dir" (recur more (assoc opts :deposit-dir b))
          "--builder" (recur more (assoc opts :builder b))
          "--from" (recur more (assoc opts :reviewer b))
          "--reviewer" (recur more (assoc opts :reviewer b))
          "--log-file" (recur more (assoc opts :log-file b))
          "--agency-send" (recur more (assoc opts :agency-send b))
          (throw (ex-info (str "unknown option: " a) {:arg a})))))))

(defn selected-deposit-ids [{:keys [deposit-ids all-corpus?]}]
  (cond
    (seq deposit-ids) deposit-ids
    all-corpus? a3-corpus-ids
    :else a3-corpus-ids))

(defn run-a3!
  [{:keys [dry-run? log-file] :or {log-file default-log-file} :as opts}]
  (let [by-id (deposits-by-id (:deposit-dir opts))
        packages (mapv (fn [id]
                         (if-let [d (get by-id id)]
                           (extract-build-package d)
                           {:ok? false
                            :fold-turn/id id
                            :missing [{:field :deposit
                                       :reason :missing-deposit
                                       :class :instruction-inadequate
                                       :detail "deposit id not found"}]}))
                       (selected-deposit-ids opts))]
    (if dry-run?
      {:mode :dry-run :packages packages}
      (let [dispatches (mapv (fn [pkg]
                               (if (:ok? pkg)
                                 (assoc (dispatch! (assoc opts :package pkg))
                                        :fold-turn/id (:fold-turn/id pkg)
                                        :mission (:mission pkg))
                                 {:fold-turn/id (:fold-turn/id pkg)
                                  :skipped true
                                  :missing (:missing pkg)}))
                             packages)
            record {:mode :dispatch :packages packages :dispatches dispatches}]
        (log! log-file record)
        record))))
