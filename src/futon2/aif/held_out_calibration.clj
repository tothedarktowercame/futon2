(ns futon2.aif.held-out-calibration
  "Two-layer calibration boundary for a preregistered held-out split.

  Layer one (futon2.aif.held-out-observations) decided WHICH runs the
  declared window holds out, with provenance. This namespace scores the
  frozen predictions those runs made against the outcomes their own
  declared locators observe, and publishes the passing disposition ONLY
  when the declared metrics fall inside the declared bounds.

  What this namespace must never do: edit the declaration, recompute a
  bound, substitute a metric, or drop a row it cannot score. A failing
  calibration is a real result about the machine's predictions, not a
  bug to code around."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [futon2.aif.held-out-split :as split]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.observation-checks :as checks]))

(load-identity/register! *ns* *file*)

(import (java.nio.file Files))

(def schema :wm/eig-held-out-calibration-v1)
(def disposition 'CALIBRATION-EVIDENCE-PASSING)

(def ^:dynamic *repo-root*
  "Checkout root that repo-relative record paths resolve against; see
   futon2.aif.held-out-observations for why the reader's cwd is not one."
  "/home/joe/code/futon2")

(defn- sha256-of [f]
  (let [bytes (Files/readAllBytes (.toPath (io/file f)))]
    (apply str (map #(format "%02x" %)
                    (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)))))

(defn- read-record
  "Parse an EDN record, tolerating tagged literals; nil when it will not
   parse at all."
  [file]
  (try
    (edn/read-string {:default (fn [_tag value] value)} (slurp file))
    (catch Exception _ nil)))

;; ---------------------------------------------------------------------------
;; Locating each held-out run's CLOSE record. The run record does not name
;; its close; the close names its run, so the locator is a scan over the
;; durable close ledger with a string prefilter before parsing.
;; ---------------------------------------------------------------------------

(defn- close-record-file
  "The durable close record whose :run/id is RUN-ID, or nil. CLOSE-ROOT is
   the directory holding the wm-full-loop-* cohort trees."
  [run-id close-root]
  (let [root (io/file close-root)
        root (if (.isAbsolute root) root (io/file *repo-root* close-root))
        needle (str ":run/id \"" run-id "\"")]
    (->> (file-seq root)
         (filter (fn [f] (and (.isFile f)
                              (str/ends-with? (.getName f) "closed.edn"))))
         (filter (fn [f] (str/includes? (slurp f) needle)))
         (sort-by (fn [f] (.getPath f)))
         first)))

(defn- repo-relative [f]
  (let [full (.getPath (io/file f))
        prefix (str *repo-root* "/")]
    (if (str/starts-with? full prefix) (subs full (count prefix)) full)))

(defn- token-outcome-comparison [close]
  (get-in close [:payload :judgment :route-attestation :token-outcome-comparison]))

;; ---------------------------------------------------------------------------
;; The realised outcome is OBSERVED, never asserted: the token's own
;; declared locator, read at the run's artifact sha -- the commit the run
;; actually authored -- through the mechanical observation checks. The
;; locator's own :sha is \"HEAD\"; evaluating at HEAD would let a LATER
;; click's edit decide an earlier run's outcome, so the artifact sha pins
;; the observation to what the run left behind.
;; ---------------------------------------------------------------------------

(defn- observe-realised
  "Returns {:realised true|false} or {:refused <refusal-map>}."
  [comparison token]
  (let [prediction (:prediction comparison)
        locator (or (get (:observation-locators prediction) token)
                    (get-in prediction [:action :observation-locators token]))
        artifact-sha (:artifact-sha comparison)]
    (cond
      (nil? locator) {:refused {:kind :no-observation-locator :token token}}
      (not (string? artifact-sha)) {:refused {:kind :no-artifact-sha :token token}}
      :else
      (let [result (checks/observe {token (assoc locator :sha artifact-sha)})]
        (if-let [refusal (get-in result [:refused token])]
          {:refused refusal}
          {:realised (contains? (:observed result) token)})))))

;; ---------------------------------------------------------------------------
;; Rows
;; ---------------------------------------------------------------------------

(defn- calibration-row
  "Build one calibration row from a valid observation row. The row is
   RETAINED with a typed :hygiene-reason whenever any link in its chain --
   run-record digest, close record, frozen prediction, realised
   observation -- fails; a retained-but-unscored row never contributes to
   the metrics."
  [obs-row close-root]
  (let [run-id (:run-id obs-row)
        run-file (let [f (io/file (get-in obs-row [:source :path] ""))]
                   (if (.isAbsolute f) f (io/file *repo-root* (get-in obs-row [:source :path]))))
        base {:run-id run-id :source (:source obs-row)}]
    (cond
      ;; provenance re-check: the observation row's tie to its run record
      ;; must still hold at calibration time
      (not (.exists run-file))
      (assoc base :hygiene-reason :source-missing)

      (not= (get-in obs-row [:source :sha256]) (sha256-of run-file))
      (assoc base :hygiene-reason :source-digest-mismatch)

      :else
      (let [close-file (close-record-file run-id close-root)]
        (if (nil? close-file)
          (assoc base :hygiene-reason :close-record-missing)
          (let [close (read-record close-file)
                close-src {:path (repo-relative close-file)
                           :sha256 (sha256-of close-file)}]
            (cond
              (nil? close)
              (assoc base :hygiene-reason :close-record-unreadable
                          :close-source close-src)

              (not= run-id (get-in close [:payload :judgment :occurrence :run/id]))
              (assoc base :hygiene-reason :close-run-id-mismatch
                          :close-source close-src)

              :else
              (let [comparison (token-outcome-comparison close)
                    prediction (:prediction comparison)
                    wanted (first (:wanted prediction))]
                (cond
                  (nil? comparison)
                  (assoc base :hygiene-reason :token-outcome-comparison-missing
                              :close-source close-src)

                  (not= :frozen (:status prediction))
                  (assoc base :hygiene-reason :prediction-not-frozen
                              :close-source close-src
                              :prediction-status (:status prediction))

                  (nil? wanted)
                  (assoc base :hygiene-reason :no-wanted-token
                              :close-source close-src)

                  :else
                  (let [token (:token wanted)
                        predicted (:predicted wanted)
                        realised (observe-realised comparison token)]
                    (if-let [refusal (:refused realised)]
                      (assoc base :hygiene-reason (:kind refusal)
                              :close-source close-src
                              :token token)
                      {:run-id run-id
                       :token token
                       :predicted predicted
                       :realised (:realised realised)
                       :prediction-rule (:prediction-rule prediction)
                       :artifact-sha (:artifact-sha comparison)
                       :source (:source obs-row)
                       :close-source close-src})))))))))))

(defn rows-from-observations
  "The held-out set is the first :next-n VALID rows of the observations
   packet, in recorded order -- the window the preregistration declared,
   no more. Valid rows beyond the window are named in the result's
   :excluded with reason :outside-declared-window: they were never held
   out, so scoring them would be evaluating on data the declaration did
   not reserve."
  ([declaration packet]
   (rows-from-observations declaration packet "data"))
  ([declaration packet close-root]
   (split/validate-v2 declaration)
   (let [n (get-in declaration [:window :next-n])
         valid (filterv #(= :valid (:hygiene %)) (:observations packet))
         in-window (take n valid)
         excluded (mapv (fn [row] {:run-id (:run-id row)
                                   :reason :outside-declared-window})
                        (drop n valid))
         rows (mapv #(calibration-row % close-root) in-window)]
     {:rows rows
      :excluded excluded
      :window {:next-n n
               :contributing (vec (map :run-id (remove :hygiene-reason rows)))}})))

;; ---------------------------------------------------------------------------
;; Metrics. Binary entropy H(p) = -p ln p - (1-p) ln (1-p), in nats (the
;; log-loss bound is ln 4, so natural log throughout). The declaration
;; names no prior, so the entropy-reduction reference is the
;; maximum-entropy binary prior p0 = 1/2:
;;
;;   predicted-entropy-reduction  H(p0) - H(p)   -- how far the frozen
;;     prediction sharpened the belief away from no information;
;;   realised-entropy-reduction   H(p) - H(y) = H(p)  -- the uncertainty
;;     the prediction still carried that observing the outcome removed
;;     (an observed binary outcome is degenerate, H = 0).
;;
;; Both are reported per the declaration; only the primary metrics gate
;; the disposition.
;; ---------------------------------------------------------------------------

(def ^:private epsilon 1.0e-15)

(defn- clamp [p] (min (- 1.0 epsilon) (max epsilon (double p))))

(defn- binary-entropy [p]
  (let [p (clamp p)]
    (- (+ (* p (Math/log p)) (* (- 1.0 p) (Math/log (- 1.0 p)))))))

(defn- row-log-loss [p y]
  (let [p (clamp p)]
    (- (+ (* y (Math/log p)) (* (- 1.0 y) (Math/log (- 1.0 p)))))))

(defn- row-brier [p y]
  (let [d (- (double p) (double y))] (* d d)))

(defn- mean [xs] (if (seq xs) (/ (reduce + xs) (double (count xs))) 0.0))

(defn calibrate
  "Pure. Score the contributing rows against the realised outcomes and
   compare to the bounds READ FROM THE DECLARATION. :passing requires a
   full window of contributing rows AND both primary metrics inside their
   bounds; the disposition symbol is attached only on :passing."
  [declaration {:keys [rows excluded]}]
  (split/validate-v2 declaration)
  (let [n (get-in declaration [:window :next-n])
        contributing (filterv #(not (:hygiene-reason %)) rows)
        scored (mapv (fn [row]
                       (let [p (double (:predicted row))
                             y (if (:realised row) 1.0 0.0)]
                         (assoc row :log-loss (row-log-loss p y)
                                    :brier (row-brier p y)
                                    :predicted-entropy-reduction
                                    (- (Math/log 2.0) (binary-entropy p))
                                    :realised-entropy-reduction
                                    (binary-entropy p))))
                     contributing)
        mean-log-loss (mean (map :log-loss scored))
        mean-brier (mean (map :brier scored))
        bounds (:passing-bounds declaration)
        log-loss-bound (get-in bounds [:mean-log-loss :lte])
        brier-bound (get-in bounds [:mean-brier :lte])
        enough? (>= (count contributing) n)
        log-loss-ok? (<= mean-log-loss log-loss-bound)
        brier-ok? (<= mean-brier brier-bound)
        failing-reasons (cond-> []
                          (not enough?) (conj :insufficient-contributing-rows)
                          (not log-loss-ok?) (conj :mean-log-loss-outside-bounds)
                          (not brier-ok?) (conj :mean-brier-outside-bounds))
        passing? (empty? failing-reasons)]
    (cond-> {:schema schema
             :ticket/id (:ticket/id declaration)
             :split {:schema (:schema declaration)
                     :registered-at (:registered-at declaration)
                     :starting-point (:starting-point declaration)}
             :status (if passing? :passing :failing)
             :failing-reasons failing-reasons
             :metrics {:mean-log-loss mean-log-loss
                       :mean-brier mean-brier
                       :mean-predicted-entropy-reduction
                       (mean (map :predicted-entropy-reduction scored))
                       :mean-realised-entropy-reduction
                       (mean (map :realised-entropy-reduction scored))}
             :bounds bounds
             :window {:next-n n
                      :contributing (mapv :run-id scored)
                      :excluded excluded}
             :rows (mapv (fn [row]
                           (if (:hygiene-reason row)
                             (select-keys row [:run-id :hygiene-reason :source
                                               :close-source :prediction-status])
                             row))
                         rows)
             :claims {:calibration-evidence-present? passing?
                      :restoration-accepted? false}}
      passing? (assoc :disposition disposition))))

(defn snapshot
  "Compute the calibration packet from the declaration and observations
   packet at the given paths."
  ([declaration-path observations-path]
   (snapshot declaration-path observations-path "data"))
  ([declaration-path observations-path close-root]
   (let [declaration (edn/read-string (slurp declaration-path))
         packet (edn/read-string (slurp observations-path))]
     (calibrate declaration
                (rows-from-observations declaration packet close-root)))))

;; ---------------------------------------------------------------------------
;; Rendering: the disposition head is SPLICED onto its own line, never
;; pattern-replaced into pprint output (futon2 96f166dc: the replace-based
;; version passed only by an accident of hash ordering). A failing packet
;; renders WITHOUT the head, so the C4 locator reads the token false.
;; ---------------------------------------------------------------------------

(defn render-packet
  "Render parseable EDN with a passing disposition at an exact C4 line
   head; see futon2.aif.held-out-observations/render-packet for why the
   head is spliced rather than replaced."
  [packet]
  (if-not (= disposition (:disposition packet))
    (with-out-str (pprint/pprint packet))
    (let [body (str/trimr (with-out-str (pprint/pprint (dissoc packet :disposition))))
          inner (str/trimr (subs body 0 (str/last-index-of body "}")))]
      (str inner "\n :disposition\n" disposition "\n}\n"))))

(defn write-snapshot!
  "Atomically materialize the calibration packet. A passing packet whose
   head the C4 predicate cannot observe is refused outright -- checked
   with the predicate itself, not a re-implementation."
  ([declaration-path observations-path output-path]
   (write-snapshot! declaration-path observations-path "data" output-path))
  ([declaration-path observations-path close-root output-path]
   (let [packet (snapshot declaration-path observations-path close-root)
         target (io/file output-path)
         tmp (io/file (.getParentFile target)
                      (str "." (.getName target) "." (java.util.UUID/randomUUID) ".tmp"))]
     (io/make-parents target)
     (when (and (= disposition (:disposition packet))
                (not (checks/decl-present? (render-packet packet) (str disposition))))
       (throw (ex-info "Refusing to write a passing calibration whose disposition head the C4 check cannot observe"
                       {:held-out/refusal :disposition-head-not-observable
                        :path output-path :decl (str disposition)})))
     (spit tmp (str ";; Regenerated from the preregistered split, the observations packet\n"
                    ";; and each held-out run's frozen prediction through\n"
                    ";; futon2.aif.held-out-calibration/write-snapshot!.\n"
                    ";; Do not hand-edit: every scored row names the close record it was\n"
                    ";; read from and its digest.\n"
                    (render-packet packet)))
     (Files/move (.toPath tmp) (.toPath target)
                 (into-array java.nio.file.StandardCopyOption
                             [java.nio.file.StandardCopyOption/ATOMIC_MOVE
                              java.nio.file.StandardCopyOption/REPLACE_EXISTING]))
     packet)))

(defn -main [& _]
  (let [packet (write-snapshot! "resources/wm/eig/held-out-split-v2.edn"
                                "resources/wm/eig/held-out-observations.edn"
                                "resources/wm/eig/held-out-calibration.edn")]
    (println (pr-str (select-keys packet [:status :failing-reasons :metrics :disposition])))))
