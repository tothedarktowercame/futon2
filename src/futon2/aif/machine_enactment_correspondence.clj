(ns futon2.aif.machine-enactment-correspondence
  "Pure E2b post-event verifier. Re-resolves E2a and separately pinned selection
   and enactment witnesses, then checks exact occurrence/action correspondence.
   It does not select, enact, or retroactively supply R9 authorization."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.machine-portfolio-restriction :as e2a])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.nio.file Files LinkOption Path)
           (java.security MessageDigest)))

(def schema-version :wm/r11-r6-enactment-correspondence-v1)
(def ^:private witness-schemas
  {:selection :wm/r6-selection-witness-v1
   :enactment :wm/r16-enactment-witness-v1})

(defn- refuse! [kind message data]
  (throw (ex-info message (assoc data :refusal kind))))

(defn- hex [bytes] (apply str (map #(format "%02x" (bit-and 0xff %)) bytes)))
(defn- digest [bytes]
  (hex (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bytes)))))

(defn- strict-form! [bytes path]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          reader (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bytes)))))
          eof (Object.) form (edn/read {:eof eof} reader) trailing (edn/read {:eof eof} reader)]
      (when (identical? eof form)
        (refuse! :e2b/witness-empty "Witness contains no EDN form" {:path (str path)}))
      (when-not (identical? eof trailing)
        (refuse! :e2b/witness-trailing-form "Witness contains trailing EDN" {:path (str path)}))
      form)
    (catch clojure.lang.ExceptionInfo failure (throw failure))
    (catch Throwable failure
      (refuse! :e2b/witness-malformed "Witness is not strict one-form UTF-8 EDN"
               {:path (str path) :cause (.getMessage failure)}))))

(defn- path! [root relative]
  (when-not (string? relative)
    (refuse! :e2b/witness-pin-missing "Witness relative path is missing" {}))
  (let [base (.normalize (.toAbsolutePath (.toPath (io/file root))))
        rel (Path/of relative (make-array String 0))
        path (.normalize (.toAbsolutePath (.resolve base rel)))]
    (when (or (.isAbsolute rel) (not (.startsWith path base)))
      (refuse! :e2b/witness-path-escape "Witness path escapes configured root"
               {:path relative}))
    path))

(defn- resolve-witness! [root label {:keys [relative-path sha256]}]
  (when-not (and (string? sha256) (re-matches #"[0-9a-f]{64}" sha256))
    (refuse! :e2b/witness-pin-missing "Witness SHA-256 pin is missing" {:label label}))
  (let [path (path! root relative-path)]
    (when-not (Files/isRegularFile path (into-array LinkOption [LinkOption/NOFOLLOW_LINKS]))
      (refuse! :e2b/witness-unreadable "Witness is not a regular file"
               {:label label :path (str path)}))
    (let [bytes (Files/readAllBytes path) actual (digest bytes)]
      (when-not (= sha256 actual)
        (refuse! :e2b/witness-pin-mismatch "Witness bytes changed"
                 {:label label :expected sha256 :actual actual}))
      (let [record (strict-form! bytes path)]
        (when-not (= (witness-schemas label) (:schema/version record))
          (refuse! :e2b/witness-schema-mismatch "Witness schema is wrong"
                   {:label label :actual (:schema/version record)}))
        {:label label :path (str path) :sha256 actual
         :bytes-count (alength bytes) :record record}))))

(defn- exact-binding? [binding]
  (and (map? binding)
       (= #{:model/id :model/revision :run/id :tick/index}
          (set (keys binding)))))

(defn verify-correspondence
  "Re-resolve E2a plus pinned selection/enactment witnesses and verify exact
   selected occurrence == enacted occurrence and action bytes."
  [{:keys [mode e2a-resolver witness-root witnesses]}]
  (when-not (contains? #{:isolated-test :production} mode)
    (refuse! :e2b/mode-unknown "Unknown E2b mode" {:mode mode}))
  (when (= :production mode)
    (refuse! :e2b/production-authority-unavailable
             "No independently configured production E2b witness authority is installed" {}))
  (when-not (= #{:selection :enactment} (set (keys witnesses)))
    (refuse! :e2b/witness-set-incomplete "Selection and enactment witnesses are required" {}))
  (let [restricted (e2a/restrict-portfolio e2a-resolver)
        resolved (mapv #(resolve-witness! witness-root % (witnesses %))
                       [:selection :enactment])
        records (into {} (map (juxt :label (comp :record identity)) resolved))
        selection (:selection records) enactment (:enactment records)
        binding (:identity restricted)
        approved (:approved-support restricted)
        approved-ids (mapv :candidate/id approved)
        selected-id (:selected/occurrence-id selection)
        selected-action (:selected/action selection)
        enacted-id (:enacted/occurrence-id enactment)
        enacted-action (:enacted/action enactment)
        source (first (filter #(= selected-id (:candidate/id %)) approved))]
    (doseq [[label record] records]
      (when-not (= :isolated-test (:scope record))
        (refuse! :e2b/scope-laundering "Isolated witness claims another scope"
                 {:label label :scope (:scope record)}))
      (when-not (and (exact-binding? (:binding record)) (= binding (:binding record)))
        (refuse! :e2b/cross-run-witness "Witness identity differs from E2a"
                 {:label label :expected binding :actual (:binding record)}))
      (when-not (= (:cohort/id selection) (:cohort/id record))
        (refuse! :e2b/cross-cohort-witness "Witness cohort differs" {:label label})))
    (when-not (= approved-ids (:approved-domain/occurrence-ids selection))
      (refuse! :e2b/approved-domain-mismatch
               "Selection witness does not name the full ordered approved domain"
               {:expected approved-ids :actual (:approved-domain/occurrence-ids selection)}))
    (when-not (= {:status :external-dependency
                  :dependency :r6-scoring-and-posterior-proof}
                 (:selection-proof selection))
      (refuse! :e2b/selection-proof-dependency-missing
               "R6 scoring/posterior proof must remain an explicit dependency" {}))
    (when-not source
      (refuse! :e2b/selected-occurrence-not-approved
               "Selected occurrence is not in the approved R6 domain"
               {:selected-id selected-id}))
    (when-not (= (:action source) selected-action)
      (refuse! :e2b/selected-action-mutation
               "Selected action bytes differ from the approved occurrence"
               {:selected-id selected-id}))
    (when (= :typed-divergence (:status enactment))
      (refuse! :e2b/divergence-unsupported
               "No adopted exact divergence authority is configured for E2b" {}))
    (when-not (= :enacted (:status enactment))
      (refuse! :e2b/enactment-status-unsupported "Witness is not an enactment"
               {:status (:status enactment)}))
    (when-not (= selected-id (:selected/occurrence-id enactment))
      (refuse! :e2b/enactment-selection-binding-mismatch
               "Enactment witness names another selected occurrence" {}))
    (when-not (= selected-action (:selected/action enactment))
      (refuse! :e2b/enactment-selection-action-mismatch
               "Enactment witness changed the selected action" {}))
    (when-not (= selected-id enacted-id)
      (refuse! :e2b/selection-enactment-mismatch
               "First-passing or other substitution changed occurrence identity"
               {:selected selected-id :enacted enacted-id}))
    (when-not (= selected-action enacted-action)
      (refuse! :e2b/enacted-action-mutation
               "Enacted action bytes differ from selected action" {}))
    {:schema/version schema-version
     :scope :isolated-test :identity binding :cohort/id (:cohort/id selection)
     :approved-domain approved
     :selected {:candidate/id selected-id :action selected-action}
     :enacted {:candidate/id enacted-id :action enacted-action}
     :correspondence :exact-occurrence-and-action
     :selection-proof (:selection-proof selection)
     :r9-pre-enact-authorization
     {:status :required-external-dependency
      :note "Post-event equality does not retroactively authorize enactment."}
     :sources {:e2a (:source restricted)
               :witnesses (mapv #(dissoc % :record) resolved)}}))
