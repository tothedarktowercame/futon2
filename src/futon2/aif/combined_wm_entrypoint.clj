(ns futon2.aif.combined-wm-entrypoint
  "One fail-closed process boundary for scheduled and direct combined WM runs.

  This namespace owns no runner semantics: live modes delegate exactly once to
  full-loop-runtime/run-opportunity!.  The historical judgement scan remains a
  deliberately named diagnostic mode."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.full-loop-runtime :as runtime]))

(def schema :wm/combined-entrypoint-config-v1)
(def pin-pattern #"[0-9a-f]{64}")

(defn- pin? [x]
  (and (map? x) (string? (:path x)) (not (str/blank? (:path x)))
       (string? (:sha256 x)) (re-matches pin-pattern (:sha256 x))))

(defn config-digest
  "Digest the authoritative config body (the self pin is excluded)."
  [config]
  (identity/digest (dissoc config :config-pin)))

(defn seal-config [config path]
  (assoc config :config-pin {:path path :sha256 (config-digest config)}))

(defn- refusal [reason absent]
  {:schema :wm/combined-entrypoint-result-v1
   :status :refused :reason reason :absent-terms (vec absent) :exit-code 2})

(defn preflight
  "Total preflight.  No authority, cast, provider, or click budget is inferred."
  [requested-mode config]
  (try
    (let [live? (#{:scheduled :on-demand} requested-mode)
          expected-trigger ({:scheduled :wallclock-cron
                             :on-demand :duree-click-on-demand}
                            requested-mode)
          casts (select-keys config [:author :reviewer :repair-reviewer])
          provider-path (:outer-policy-evidence-provider-path config)
          absent (cond-> []
                   (not= schema (:schema config)) (conj :config/schema)
                   (not= requested-mode (:mode config)) (conj :scheduling/mode)
                   (and live? (not= expected-trigger (:trigger config)))
                   (conj :scheduling/trigger)
                   (not (pin? (:config-pin config))) (conj :config/pin)
                   (and (pin? (:config-pin config))
                        (not= (config-digest config)
                              (get-in config [:config-pin :sha256])))
                   (conj :config/content-pin)
                   (and live? (not (pin? (:opportunity-authority config))))
                   (conj :opportunity/authority)
                   (and live? (not= 0 (:ordinary-click-budget config)))
                   (conj :ordinary-click-budget/explicit-zero)
                   (and live? (some #(str/blank? (get casts %))
                                    [:author :reviewer :repair-reviewer]))
                   (conj :casts/explicit)
                   (and live? (= (:author casts) (:reviewer casts)))
                   (conj :casts/separation)
                   (and live? (str/blank? provider-path))
                   (conj :outer-policy-evidence-provider/path)
                   (and live? (not (str/blank? provider-path))
                        (not (.isFile (io/file provider-path))))
                   (conj :outer-policy-evidence-provider/readable))]
      (if (seq absent)
        (refusal :combined-entrypoint-preflight-failed absent)
        {:status :ready :mode requested-mode :trigger (:trigger config)}))
    (catch Throwable t
      (assoc (refusal :combined-entrypoint-preflight-exception [:config/valid-edn])
             :detail (ex-message t)))))

(defn load-config [path]
  (try
    (if (str/blank? path)
      (refusal :combined-entrypoint-config-required [:config/path])
      (let [file (io/file path)]
        (if-not (.isFile file)
          (refusal :combined-entrypoint-config-unreadable [:config/path])
          {:status :loaded :config (edn/read-string (slurp file))})))
    (catch Throwable t
      (assoc (refusal :combined-entrypoint-config-invalid [:config/edn])
             :detail (ex-message t)))))

(defn execute!
  "Execute one configured opportunity. Dependencies are injectable so unit
  tests do no network/process work. Returns process semantics; never exits."
  ([requested-mode config] (execute! requested-mode config {}))
  ([requested-mode config {:keys [runtime-fn diagnostic-fn identity-fn]
                           :or {runtime-fn runtime/run-opportunity!
                                identity-fn #(str (java.util.UUID/randomUUID))}}]
   (let [checked (preflight requested-mode config)]
     (if-not (= :ready (:status checked))
       checked
       (if (= :diagnostic requested-mode)
         (let [f (or diagnostic-fn
                     (requiring-resolve 'wm-scheduled-run/run-judgement-only!))
               result (f (or (:diagnostic-days config) 14))]
           {:schema :wm/combined-entrypoint-result-v1 :status :diagnostic-complete
            :mode requested-mode :result result :exit-code 0})
         (let [run-id (identity-fn)
               binding-body {:schema :wm/combined-run-binding-v1
                             :run-id run-id :opportunity-id run-id
                             :mode requested-mode :trigger (:trigger config)
                             :config-pin (:config-pin config)
                             :opportunity-authority (:opportunity-authority config)
                             :ordinary-click-budget 0}
               run-binding (assoc binding-body :sha256 (identity/digest binding-body))
               opts (merge (select-keys config
                                        [:author :reviewer :repair-reviewer
                                         :outer-policy-evidence-provider-path
                                         :run-record-dir])
                           {:run-id run-id :opportunity-id run-id
                            :trigger (:trigger config)
                            :combined-entrypoint-mode requested-mode
                            :combined-run-binding run-binding
                            :click-budget/state (atom 0)})
               result (runtime-fn opts)
               successful? (contains? #{:closed :cohort-complete
                                        :grounded-change :complete}
                                      (:outcome result))]
           {:schema :wm/combined-entrypoint-result-v1
            :status (if successful? :complete :runtime-incomplete)
            :mode requested-mode :trigger (:trigger config)
            :run-id run-id :opportunity-id run-id
            :run-binding run-binding :runtime-result result
            :exit-code (if successful? 0 1)}))))))

(defn run-cli!
  "CLI logic without System/exit. Config path is explicit or supplied by the
  deployment environment; an absent path is a typed refusal."
  [args env]
  (let [[mode-name explicit-path & extra] args
        mode (some-> mode-name keyword)
        path (or explicit-path (get env "FUTON_WM_RUN_CONFIG"))]
    (cond
      (seq extra) (refusal :combined-entrypoint-invalid-arguments [:arguments])
      (not (#{:scheduled :on-demand :diagnostic} mode))
      (refusal :combined-entrypoint-mode-required [:scheduling/mode])
      :else (let [loaded (load-config path)]
              (if (= :loaded (:status loaded))
                (execute! mode (:config loaded))
                loaded)))))

(defn -main [& args]
  (let [result (run-cli! args (System/getenv))]
    (pp/pprint result)
    (shutdown-agents)
    (System/exit (int (:exit-code result 2)))))
