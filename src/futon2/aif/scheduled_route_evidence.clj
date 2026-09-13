(ns futon2.aif.scheduled-route-evidence
  "Pure E4 causal-route verifier.  Scheduler metadata proves which bounded
   ticks were commissioned; it is never an F_pi operand.  Source locations and
   expected byte digests belong to the independently supplied authority, not
   to the candidate being checked."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io PushbackReader StringReader]
           [java.nio ByteBuffer]
           [java.nio.charset CharacterCodingException CodingErrorAction StandardCharsets]
           [java.nio.file Files]
           [java.math BigInteger]
           [java.security MessageDigest]))

(def source-roles
  [:commission :dispatch :run-launch :launch-history :tick-entries :observations
   :occurrence-universe :predecessor-predictions :r8-occurrences])

(def ^:private schemas
  {:commission :wm/e4-commission-v1
   :dispatch :wm/e4-dispatch-v1
   :run-launch :wm/e4-run-launch-v1
   :launch-history :wm/e4-launch-history-v1
   :tick-entries :wm/e4-tick-entry-v1
   :observations :wm/e4-r2-observation-v1
   :occurrence-universe :wm/e4-occurrence-universe-v1
   :predecessor-predictions :wm/e4-predecessor-v1
   :r8-occurrences :wm/e4-r8-occurrences-v1})

(defn- refuse! [kind data]
  (throw (ex-info (name kind) (assoc data :refusal kind))))

(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))

(defn- sha256 [^bytes bs]
  (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") bs))))

(defn- read-one-edn [^bytes bs role]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          s (str (.decode decoder (ByteBuffer/wrap bs)))]
      (with-open [r (PushbackReader. (StringReader. s))]
        (let [v (edn/read {:eof ::eof} r)
              tail (edn/read {:eof ::eof} r)]
          (when (or (= ::eof v) (not= ::eof tail))
            (refuse! :e4/source-not-single-edn {:role role}))
          v)))
    (catch CharacterCodingException _
      (refuse! :e4/invalid-utf8 {:role role}))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Exception e
      (refuse! :e4/source-read-failed {:role role :cause (.getName (class e))}))))

(defn file-authority
  "Build an isolated, configured authority. PATHS and digests are configured
   outside candidate evidence. Production use additionally requires an
   independently retained production authority; this constructor deliberately
   labels itself fixture-only."
  [entries]
  {:schema :wm/e4-source-authority-v1
   :scope :isolated-fixture
   :sources
   (into {}
         (for [role source-roles
               :let [{:keys [path sha256]} (get entries role)]]
           [role {:sha256 sha256
                  :resolve (fn []
                             (try
                               (Files/readAllBytes (.toPath (io/file path)))
                               (catch Exception e
                                 (refuse! :e4/source-read-failed
                                          {:role role :cause (.getName (class e))}))))}]))})

(defn- resolve-sources! [{:keys [schema scope sources]}]
  (when-not (= :wm/e4-source-authority-v1 schema)
    (refuse! :e4/invalid-source-authority {:schema schema}))
  (when-not (#{:isolated-fixture :independently-retained-production} scope)
    (refuse! :e4/untrusted-source-scope {:scope scope}))
  (into {}
        (for [role source-roles
              :let [{expected-sha :sha256 :keys [resolve]} (get sources role)]]
          (do
            (when-not (and (re-matches #"[0-9a-f]{64}" (or expected-sha "")) (fn? resolve))
              (refuse! :e4/missing-source-authority {:role role}))
            (let [bs (resolve)]
              (when-not (instance? (Class/forName "[B") bs)
                (refuse! :e4/source-not-bytes {:role role}))
              (let [actual (sha256 bs)]
                (when-not (= expected-sha actual)
                  (refuse! :e4/source-digest-mismatch
                           {:role role :expected expected-sha :actual actual}))
                [role {:sha256 actual :record (read-one-edn bs role)}]))))))

(defn- unique-ordered! [xs refusal data]
  (when-not (and (vector? xs) (every? nonblank? xs) (= (count xs) (count (distinct xs))))
    (refuse! refusal data))
  xs)

(defn- indexed! [rows keyf refusal]
  (when-not (vector? rows) (refuse! refusal {:value rows}))
  (let [ks (mapv keyf rows)]
    (when (or (some nil? ks) (not= (count ks) (count (distinct ks))))
      (refuse! refusal {:keys ks}))
    (zipmap ks rows)))

(defn- schemas! [resolved]
  (doseq [[role expected] schemas
          :let [record (get-in resolved [role :record])
                records (if (#{:launch-history :tick-entries :observations :occurrence-universe
                               :predecessor-predictions :r8-occurrences} role)
                          record [record])]]
    (when-not (and (seq records) (every? #(= expected (:schema %)) records))
      (refuse! :e4/source-schema-mismatch {:role role :expected expected}))))

(defn verify-route!
  "Verify one bounded, ordered R10 -> tick -> R2/(predecessor) -> R8 route.
   Returns fixture evidence only for fixture authority and never authorizes a
   scheduler or changes the separately proved R8 computation."
  [authority]
  (let [resolved (resolve-sources! authority)
        r #(get-in resolved [% :record])
        commission (r :commission) dispatch (r :dispatch) launch (r :run-launch)
        history (r :launch-history) ticks (r :tick-entries) observations (r :observations)
        universes (r :occurrence-universe)
        predictions (r :predecessor-predictions) r8s (r :r8-occurrences)
        plan (:tick/plan commission)
        run-id (:run/id launch) model-id (:model/id launch) revision (:model/revision launch)]
    (schemas! resolved)
    ;; No production completeness/ownership authority exists yet.  Refusing is
    ;; safer than allowing a caller to promote fixture resolvers with a label.
    (when (= :independently-retained-production (:scope authority))
      (refuse! :e4/production-authority-unavailable
               {:required :independent-dispatch-run-and-complete-tick-source-acceptance}))
    (when-not (and (= :wm/e4-commission-v1 (:schema commission))
                   (= :R10 (:node commission)) (nonblank? (:commission/id commission)))
      (refuse! :e4/invalid-commission {:commission commission}))
    (unique-ordered! plan :e4/invalid-tick-plan {:tick-plan plan})
    (when-not (and (= :wm/e4-dispatch-v1 (:schema dispatch))
                   (= :success (:execution/outcome dispatch))
                   (= (:commission/id commission) (:commission/id dispatch))
                   (nonblank? (:dispatch/id dispatch)))
      (refuse! :e4/dispatch-not-successful-or-unjoined {:dispatch dispatch}))
    (when-not (and (= :wm/e4-run-launch-v1 (:schema launch))
                   (every? nonblank? [run-id (:launch/id launch) model-id revision])
                   (= (:commission/id commission) (:commission/id launch))
                   (= (:dispatch/id dispatch) (:dispatch/id launch))
                   (= plan (:tick/plan launch))
                   (= :idempotent (:launch/semantics launch)))
      (refuse! :e4/run-launch-unjoined {:run-launch launch}))
    (let [matching (filterv #(and (= (:commission/id commission) (:commission/id %))
                                  (= (:dispatch/id dispatch) (:dispatch/id %))) history)]
      (when-not (and (= 1 (count matching))
                     (= (:launch/id launch) (:launch/id (first matching)))
                     (= run-id (:run/id (first matching))))
        (refuse! :e4/launch-uniqueness-unwitnessed
                 {:matching-launches (mapv #(select-keys % [:launch/id :run/id]) matching)})))
    (let [by-tick (indexed! ticks :tick/id :e4/duplicate-or-invalid-tick-entry)
          obs-by-tick (indexed! observations :tick/id :e4/duplicate-or-invalid-observation)
          universe-by-tick (indexed! universes :tick/id :e4/duplicate-or-invalid-occurrence-universe)
          pred-by-tick (indexed! predictions :tick/id :e4/duplicate-or-invalid-prediction)
          r8-by-tick (indexed! r8s :tick/id :e4/duplicate-or-invalid-r8-occurrence)]
      (doseq [[label ks] [[:tick-entry (mapv :tick/id ticks)]
                          [:observation (mapv :tick/id observations)]
                          [:occurrence-universe (mapv :tick/id universes)]
                          [:prediction (mapv :tick/id predictions)]
                          [:r8 (mapv :tick/id r8s)]]]
        (when-not (= plan ks)
          (refuse! :e4/tick-plan-coverage-mismatch {:role label :declared plan :actual ks})))
      (doseq [[idx tick-id] (map-indexed vector plan)]
        (let [tick (by-tick tick-id) obs (obs-by-tick tick-id)
              universe (universe-by-tick tick-id)
              pred (pred-by-tick tick-id) r8 (r8-by-tick tick-id)
              common [run-id tick-id idx model-id revision]
              actual (fn [m] [(:run/id m) (:tick/id m) (:tick/index m)
                              (:model/id m) (:model/revision m)])]
          (when-not (every? #(= common (actual %)) [tick obs universe pred r8])
            (refuse! :e4/run-tick-model-identity-mismatch {:tick/id tick-id}))
          (when-not (= (:launch/id launch) (:launch/id tick))
            (refuse! :e4/tick-entry-launch-mismatch {:tick/id tick-id}))
          (let [channels (:observation/channels obs)
                channel-ids (mapv :channel/id channels)
                _ (when-not (and (vector? channels) (seq channels)
                                 (every? #(and (nonblank? (:channel/id %))
                                               (number? (:value %))
                                               (Double/isFinite (double (:value %)))) channels)
                                 (= (count channel-ids) (count (distinct channel-ids))))
                    (refuse! :e4/invalid-observation-payload {:tick/id tick-id}))
                support (unique-ordered! (:candidate/support universe)
                                         :e4/invalid-candidate-support {:tick/id tick-id})]
            (when-not (and (= support (:candidate/support pred))
                           (= support (:candidate/support r8)))
              (refuse! :e4/candidate-coverage-mismatch {:tick/id tick-id}))
            (if (zero? idx)
              (when-not (and (= :off (:coverage r8)) (= :initial-tick (:reason r8))
                             (= :off (:coverage pred)) (= :initial-tick (:reason pred)))
                (refuse! :e4/initial-tick-prediction-required-off {:tick/id tick-id}))
              (let [previous (nth plan (dec idx))
                    rows (:rows pred) occurrences (:rows r8)
                    pmap (indexed! rows :candidate/id :e4/duplicate-prediction-candidate)
                    omap (indexed! occurrences :candidate/id :e4/duplicate-r8-candidate)]
                (when-not (and (= :complete (:coverage pred)) (= :complete (:coverage r8))
                               (= previous (:predecessor/tick-id pred))
                               (= support (mapv :candidate/id rows))
                               (= support (mapv :candidate/id occurrences)))
                  (refuse! :e4/missing-stale-or-reordered-predecessor
                           {:tick/id tick-id :expected-predecessor previous}))
                (doseq [cid support
                        :let [p (pmap cid) o (omap cid)]]
                  (let [action (:action p)
                        prediction (:prediction p)
                        observation-ref (:observation/ref o)
                        prediction-ref (:prediction/ref o)]
                  (when-not (and (map? action) (keyword? (:type action))
                                 (map? prediction)
                                 (= (set channel-ids) (set (keys (:mean prediction))))
                                 (= (set channel-ids) (set (keys (:variance prediction))))
                                 (every? number? (vals (:mean prediction)))
                                 (every? #(and (number? %) (pos? (double %)))
                                         (vals (:variance prediction)))
                                 (= {:source-role :observations :tick/id tick-id
                                     :sha256 (get-in resolved [:observations :sha256])}
                                    observation-ref)
                                 (= {:source-role :predecessor-predictions
                                     :tick/id tick-id :candidate/id cid
                                     :sha256 (get-in resolved [:predecessor-predictions :sha256])}
                                    prediction-ref)
                                 (= prediction (:prediction/input o))
                                 (= (:observation/channels obs) (:observation/input o))
                                 (= previous (:produced-at/tick-id p))
                                 (= (:action p) (:action o))
                                 (= (:action/model-revision p) (:action/model-revision o))
                                 (= revision (:action/model-revision p)))
                    (refuse! :e4/prediction-occurrence-mismatch
                             {:tick/id tick-id :candidate/id cid})))))))))
      {:status :verified-causal-route
       :scope (:scope authority)
       :scheduler-is-f-pi-operand? false
       :commission/id (:commission/id commission)
       :dispatch/id (:dispatch/id dispatch)
       :run/id run-id
       :tick/plan plan
       :launch/uniqueness :witnessed-in-resolved-history
       :r8-arithmetic-verified? false
       :source-pins (into {} (map (fn [[k v]] [k (:sha256 v)]) resolved))
       :restart-authorized false
       :production-edge-fired? (= :independently-retained-production (:scope authority))})))
