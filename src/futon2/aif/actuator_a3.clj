(ns futon2.aif.actuator-a3
  "Thin, content-free A3 actuator.

  Extracts a build package from a reviewed fold-turn deposit, dispatches the
  package to a builder, and verifies returned evidence refs before any dial is
  counted. The executor does not interpret domain content and does not mutate
  substrate."
  (:require [babashka.http-client :as http]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.fold-escrow :as esc]
            [futon2.aif.mission-registry :as missions])
  (:import [java.time Instant]))

(def default-log-file "/home/joe/code/futon2/logs/actuator-a3.log")
(def default-agency-send "/home/joe/code/futon3c/scripts/agency_send.py")
(def default-drawbridge-url "http://localhost:6768/eval")

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

(defn extract-build-package
  [deposit]
  (let [ts (typespec deposit)
        ss (structure-spec deposit)
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
       :policy-holes (:policy-holes ss)})))

(defn build-prompt [package]
  (str "A3 BUILDER TASK — content-free fold blueprint execution.\n\n"
       "Build this reviewed fold-turn blueprint. Use handoffs as needed. "
       "Write artifacts to the substrate endpoints the typespec names and advance "
       "the mission doc only when backed by real artifact evidence.\n\n"
       "Return, for each claimed closed hole, an :evidence-ref that the executor "
       "can independently rerun. No evidence-ref means the closure will be rejected.\n\n"
       "BUILD PACKAGE EDN:\n"
       (pr-str package)
       "\n\nAllowed evidence-ref shapes:\n"
       "  {:kind :file-exists :path \"/abs/path\"}\n"
       "  {:kind :file-contains :path \"/abs/path\" :pattern \"literal text\"}\n"
       "  {:kind :drawbridge :form \"(read-only-clojure-form)\"}\n"))

(defn dispatch!
  [{:keys [package builder reviewer agency-send]
    :or {builder "zai-3"
         reviewer "claude-4"
         agency-send default-agency-send}}]
  (let [cmd [agency-send "--to" builder "--kind" "bell" "--from" reviewer
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

(defn drawbridge-resolves?
  ([ref] (drawbridge-resolves? ref default-drawbridge-url))
  ([{:keys [form]} drawbridge-url]
   (when-not (and (string? form) (not (str/blank? form)))
     false)
   (let [resp (http/post drawbridge-url
                         {:headers {"Content-Type" "text/plain"}
                          :body form
                          :throw false
                          :timeout 10000})]
     (and (= 200 (:status resp))
          (let [body (try (edn/read-string (:body resp))
                          (catch Throwable _ (:body resp)))]
            (if (map? body)
              (not (false? (:ok body)))
              (some? body)))))))

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
  "Verify builder-returned closures and apply the witness-gated dial rule.
  Dial counts only when at least one closure has a resolved witness AND the
  independently re-parsed open-hole-count decreased."
  [{:keys [closures before-open-hole-count after-open-hole-count] :as result} opts]
  (let [verified (mapv #(verify-closure % opts) (or closures []))
        accepted (filterv :closure/accepted? verified)
        dial-moved? (and (seq accepted)
                         (number? before-open-hole-count)
                         (number? after-open-hole-count)
                         (< (long after-open-hole-count)
                            (long before-open-hole-count)))]
    (assoc result
           :closures verified
           :accepted-closures accepted
           :rejected-closures (filterv (complement :closure/accepted?) verified)
           :dial-moved? (boolean dial-moved?)
           :dial-counted-closures (if dial-moved? (count accepted) 0))))

(defn mission-open-hole-count [mission-id]
  (:open-hole-count (missions/mission-status mission-id)))

(defn log! [log-file record]
  (.mkdirs (.getParentFile (io/file log-file)))
  (spit log-file (str (pr-str (assoc record :at (str (Instant/now)))) "\n")
        :append true))

(defn render-package [package]
  (pr-str (select-keys package
                       [:fold-turn/id :mission :mission-key :typespec
                        :structure-spec :policy-holes :ok? :missing])))

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
