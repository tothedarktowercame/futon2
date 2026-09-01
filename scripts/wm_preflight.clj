(ns wm-preflight
  "G4 — readiness preflight for a War Machine end-to-end run.

   Answers: if the loop ran over mission M right now, would it actually
   deliberate AND act — or silently no-op? It surfaces the toggles + deposit
   state that otherwise fail *silently* (selection gain holds, no error), which is exactly
   how a fresh-mission run stalls invisibly.

   Usage:
     clojure -M:wm-preflight                 # global arm/toggle state + deposit coverage
     clojure -M:wm-preflight M-foo M-bar ...  # per-mission READY / NOT-READY verdict

   Reads code-default arm values (a standalone JVM has no bindings, so these are
   the defaults a scheduled tick starts from) and the FUTON_WM_* env escape
   hatches. It does NOT mutate anything."
  (:require [futon2.aif.belief :as belief]
            [futon2.aif.fold-realized :as fr]
            [futon2.aif.actuator-a6 :as a6]
            [futon2.aif.close-loop :as cl]
            [futon2.aif.fold-escrow :as esc]
            [cheshire.core :as json]
            [clojure.java.shell :as shell]
            [clojure.string :as str])
  (:import [java.time Instant]
           [java.security MessageDigest]
           [java.math BigInteger]))

(defn- onoff [b] (if b "ON " "off"))

(defn- stem [x] (-> (str x) (str/replace #".*/" "") str/lower-case))

(defn- deposit-for?
  "Mirror of enact/deposit-for-mission: case-insensitive id-stem substring match."
  [missions target]
  (let [s (stem target)]
    (boolean (some #(let [m (stem %)] (or (str/includes? m s) (str/includes? s m)))
                   missions))))

(defn parse-cli [args]
  (loop [xs args fence-id nil evidence nil missions []]
    (if (empty? xs)
      {:writer-fence-id fence-id :writer-fence-evidence evidence :missions missions}
      (case (first xs)
        "--writer-fence"
        (if-let [id (second xs)]
          (recur (nnext xs) id evidence missions)
          (throw (ex-info "--writer-fence requires an id" {})))
        "--writer-fence-evidence"
        (if-let [path (second xs)]
          (recur (nnext xs) fence-id path missions)
          (throw (ex-info "--writer-fence-evidence requires a path" {})))
        (recur (rest xs) fence-id evidence (conj missions (first xs)))))))

(defn- sha256 [bytes]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") bytes))))

(def fence-evidence-checker
  "/home/joe/code/futon2/checks/writer_fence_evidence.py")

(def ^:dynamic *run-fence-evidence*
  (fn [fence-id attestation-path]
    (shell/sh "python3" fence-evidence-checker
              "--fence-id" fence-id "--attestations" attestation-path)))

(defn verify-fence-evidence [fence-id path]
  (cond
    (and (nil? fence-id) (nil? path))
    {:verified? false :status :absent :reason :not-declared}

    (or (nil? fence-id) (nil? path))
    {:verified? false :status :invalid :reason :id-and-evidence-required-together}

    :else
    (try
      (let [bytes (java.nio.file.Files/readAllBytes (java.nio.file.Paths/get path (make-array String 0)))
            receipt (json/parse-string (String. bytes "UTF-8") true)
            attested (get-in receipt [:classification :attested])
            attestation (:value attested)
            expires (some-> attestation :expires-at Instant/parse)
            temp (java.io.File/createTempFile "wm-preflight-fence-attestation-" ".json")]
        (try
          (spit temp (json/generate-string attestation))
          (let [live (*run-fence-evidence* fence-id (.getAbsolutePath temp))
                live-receipt (try (json/parse-string (:out live) true)
                                  (catch Throwable _ nil))
                problems (cond-> []
                           (not= "FENCE-VERIFIABLE" (:verdict receipt)) (conj :prior-verdict-not-verifiable)
                           (not= fence-id (:fence-id receipt)) (conj :receipt-fence-id-mismatch)
                           (not= fence-id (get-in receipt [:classification :fence-id])) (conj :classification-fence-id-mismatch)
                           (not= "complete" (:status attested)) (conj :attestation-incomplete)
                           (not= fence-id (:fence-id attestation)) (conj :attestation-fence-id-mismatch)
                           (nil? (:observation-interval receipt)) (conj :observation-interval-absent)
                           (or (nil? expires) (.isBefore expires (Instant/now))) (conj :attestation-expired)
                           (not= 0 (:exit live)) (conj :live-fence-check-failed)
                           (not= "FENCE-VERIFIABLE" (:verdict live-receipt)) (conj :live-fence-not-verifiable)
                           (not= fence-id (:fence-id live-receipt)) (conj :live-fence-id-mismatch)
                           (nil? (:observation-interval live-receipt)) (conj :live-observation-interval-absent))]
            {:verified? (empty? problems)
             :status (if (empty? problems) :observed-held :unverified)
             :id fence-id :path path :receipt-sha256 (sha256 bytes)
             :live-receipt-sha256 (some-> (:out live) .getBytes sha256)
             :live-verdict (:verdict live-receipt)
             :live-observation-interval (:observation-interval live-receipt)
             :problems problems})
          (finally (.delete temp))))
      (catch Throwable t
        {:verified? false :status :unavailable :id fence-id :path path
         :reason :evidence-unreadable :detail (.getMessage t)}))))

(defn readiness-claim [started-at finished-at fence]
  {:claim :event-free
   :interval {:started-at (str started-at) :finished-at (str finished-at)}
   :writer-fence (dissoc fence :verified?)
   :event-free? (if (:verified? fence) true :unverified)
   :distinguishable-cause? (:verified? fence)})

(defn readiness-label [ready? fence]
  (cond
    (not ready?) "NOT-READY ✗"
    (:verified? fence) (str "READY (FENCE-VERIFIED " (:id fence) ") ✓")
    :else "READY-CONTENT-ONLY (event-free unverified) ⚠"))

(defn -main [& missions]
  (let [{:keys [writer-fence-id writer-fence-evidence missions]} (parse-cli missions)
        fence (verify-fence-evidence writer-fence-id writer-fence-evidence)
        started-at (Instant/now)]
  (println "══ WM readiness preflight (G4) ══")
  (println "claim: readiness-over-observation-interval"
           "writer-fence:" (if (:verified? fence)
                              (str "OBSERVED-HELD " writer-fence-id)
                              (str "UNVERIFIED " (pr-str (dissoc fence :verified?)))))

  (println "\n── Deliberation arms (Tier-1, world-inert; change thinking) ──")
  (doseq [[label v] [["pattern model-uncertainty bonus" a6/*pattern-grain-model-uncertainty?*]
                     ["r3d-multichannel?   (R3d 8-channel)" belief/*r3d-multichannel?*]
                     ["risk-mode           (R5a)"          (or (System/getenv "FUTON_WM_RISK_MODE") ":kl (default)")]
                     ["ambiguity-mode      (R5b)"          (or (System/getenv "FUTON_WM_AMBIGUITY_MODE") ":gaussian-entropy (default)")]]]
    (println (format "  %-38s %s" label (if (boolean? v) (onoff v) v))))

  (println "\n── Acting arms (Tier-2; move real dials on a run) ──")
  (let [live-wire   fr/*live-wire?*
        selection-gain-feed fr/*selection-gain-grounded-feed?*
        escrow      cl/*escrow-replay?*
        classical   cl/*classical-fold-score?*
        env-wire    (System/getenv "FUTON_WM_LIVE_WIRE")
        enact-armed (and live-wire escrow (not classical))]
    (doseq [[label v] [["live-wire?          (R16 enactment)"    live-wire]
                       ["selection-gain grounded feed (R14)" selection-gain-feed]
                       ["escrow-replay?      (fold replay seam)"  escrow]
                       ["classical-fold-score?  (must be OFF)"       classical]]]
      (println (format "  %-38s %s" label (onoff v))))
    (when env-wire (println (format "  %-38s %s" "[env] FUTON_WM_LIVE_WIRE" env-wire)))
    (println (format "  → enactment ARMED (live-wire ∧ escrow-replay ∧ ¬classical): %s"
                     (if enact-armed "YES" "NO")))

    (println "\n── Deposits (the #1 silent-stall gate) ──")
    (let [{:keys [deposits]} (try (esc/load-deposits)
                                  (catch Throwable t {:deposits ::err :err (.getMessage t)}))]
      (if (= deposits ::err)
        (println "  ✗ escrow unreadable — escrow replay disabled; every mission NOT-READY")
        (let [dmissions (mapv :mission deposits)]
          (println (format "  %d deposit(s) loaded + sha-gated." (count deposits)))
          (if (seq missions)
            (do
              (println "\n── Per-mission verdict ──")
              (doseq [m missions]
                (let [has (deposit-for? dmissions m)
                      ready (and enact-armed has)]
                  (println (format "  %-32s deposit:%-4s → %s"
                                   m (if has "yes" "NONE")
                                   (readiness-label ready fence)))
                  (when-not has
                    (println "      ↳ no deposit → fold abstains, ΔG nil, γ holds (silent no-op — the G1 gap)"))
                  (when-not enact-armed
                    (println "      ↳ acting arms not aligned (see Tier-2 above)")))))
            (do
              (println "  missions WITH a deposit (runnable without new authoring):")
              (doseq [m (sort (distinct dmissions))] (println "    •" m))
              (println "\n  (pass mission ids as args for a per-mission readiness verdict)"))))))
    (println "\nobservation:" (pr-str (readiness-claim started-at (Instant/now)
                                                       fence)))
    (when (and (or writer-fence-id writer-fence-evidence) (not (:verified? fence)))
      (System/exit 1)))))
