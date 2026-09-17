(ns futon2.aif.find-designation
  "F4 designation artifact, authority-agnostic.
  Grounded in the WM-08 owner's AUTH-F4-scope position (accepted by claude-4
  2026-09-17) and the F11 pricing it rests on: the F4 'must not' is deontic
  (runs/F11-find/08-dispatch.edn -- the zero-mass designation comes from
  outside the finder), and an F4 conjunct on the finder's own return
  constrains nothing (amendedIdentity, mathlib4 DarkTower/WarMachine/
  F11AmendedCarrier.lean:358, selects the whole repository with an empty
  designation and satisfies all four joint laws). So a designation is
  NEGATIVE KNOWLEDGE about a tension, asserted before the finder runs by an
  authority outside the run. Joe settles AUTH-F4-scope; this namespace
  predetermines none of the candidate authorities -- it accepts an artifact
  from whichever he settles on, refuses a self-supplied one, and reports
  honest vacuity. It never manufactures a designation, never derives one
  from what fired, and never reads the interpretation record.

  Structurally parallel to futon2.aif.find-expectations (one reader will
  meet both): same artifact shape, same occurrence binding, same
  declared-role control, same refusal style, with :law :F4."
  (:require [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]))

(def schema-id :wm/find-designation-v1)

(def role :designation-author)

(defn- need! [ok reason data]
  (when-not ok
    (throw (ex-info "F4 designation refused"
                    (merge {:finding :find/refusal :law :F4 :reason reason} data)))))

(defn- artifact-shape! [artifact]
  (need! (= schema-id (:schema artifact)) :invalid-designation-artifact
         {:schema (:schema artifact)})
  (need! (and (map? (:author artifact)) (:id (:author artifact))
              (keyword? (:role (:author artifact))))
         :invalid-designation-artifact {:author (:author artifact)})
  (need! (and (map? (:occurrence artifact))
              (:target (:occurrence artifact))
              (map? (:target-source (:occurrence artifact)))
              (:repository-sha256 (:occurrence artifact))
              (:pinned-at (:occurrence artifact)))
         :invalid-designation-artifact {:occurrence (:occurrence artifact)})
  (need! (and (set? (:designated artifact))
              (every? #(and (keyword? %) (namespace %)) (:designated artifact)))
         :invalid-designation-artifact
         {:designated (:designated artifact)})
  ;; A designation with no stated reason is not negative knowledge about
  ;; the tension, it is a bare list.
  (need! (and (string? (:basis artifact))
              (seq (str/trim (:basis artifact))))
         :invalid-designation-artifact {:basis (:basis artifact)})
  artifact)

(defn read-artifact
  "Read and structurally validate a designation artifact from EDN. This is
  the ONLY entry point that mints a usable artifact."
  [source]
  (artifact-shape! (edn/read-string (slurp source))))

(defn- check-occurrence-binding! [occurrence artifact]
  (need! (and (map? occurrence) (:target occurrence)
              (:target-source occurrence) (:repository-sha256 occurrence)
              (:pinned-at occurrence))
         :occurrence-required {})
  (let [o (:occurrence artifact)]
    (doseq [k [:target :target-source :repository-sha256 :pinned-at]]
      (need! (= (get occurrence k) (get o k))
             :occurrence-binding-mismatch
             {:field k :artifact (get o k) :occurrence (get occurrence k)}))
    (when (and (:source-digests occurrence) (:source-digests o))
      (need! (= (:source-digests occurrence) (:source-digests o))
             :occurrence-binding-mismatch
             {:field :source-digests :artifact (:source-digests o)
              :occurrence (:source-digests occurrence)}))))

(defn resolve-designation
  "Resolve OCCURRENCE + ARTIFACT into the designated set with its honest F4
  status: {:designated <set or nil> :f4 :discriminating | :vacuous}.
  No artifact is the correct, honest :vacuous report, not a deficiency --
  validate-result! already accepts nil. The declared-role control is not
  authentication: an author role other than :designation-author is the
  finder's/interpreter's own side of the run and is refused. REPOSITORY is
  used only to report the status of the applicable designation (the same
  intersection validate-result! computes); a designated id outside the
  repository is validated there, not silently dropped here."
  [occurrence artifact repository]
  (need! (or (nil? artifact) (map? artifact)) :invalid-designation-artifact {})
  (when (some? artifact)
    (artifact-shape! artifact)
    (need! (= role (get-in artifact [:author :role]))
           :self-supplied-designation {:author-role (get-in artifact [:author :role])})
    (check-occurrence-binding! occurrence artifact))
  (let [designated (:designated artifact)
        applicable (and designated repository
                        (set/intersection designated (:patterns repository)))]
    {:designated designated
     :f4 (if (seq applicable) :discriminating :vacuous)}))

(defn designation-for
  "The designated set (or nil) for find-receipt/validate-result!'s third
  argument. Refuses exactly as resolve-designation does."
  [occurrence artifact repository]
  (:designated (resolve-designation occurrence artifact repository)))
