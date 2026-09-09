#!/usr/bin/env bb
;; F2 -- THE RUN4 READINESS METER.
;;
;;   bb holes/labs/wm-contract/run4_readiness.bb             ; check the committed artifacts
;;   bb holes/labs/wm-contract/run4_readiness.bb --emit      ; regenerate them from live sources
;;   bb holes/labs/wm-contract/run4_readiness.bb --summary   ; print the VERDICT block only
;;   bb holes/labs/wm-contract/run4_readiness.bb --probe     ; re-take the Lean axiom probe
;;   bb holes/labs/wm-contract/run4_readiness.bb --negative  ; the plants, each of which must
;;                                                           ; flip a named line to :blocked
;;   bb holes/labs/wm-contract/run4_readiness.bb --negative --emit ; and commit the plant table
;;
;; Run FROM THE LAB DIRECTORY.
;;
;; WHAT THIS IS. Joe, 2026-09-05: "i don't get a sense of how far off we are from
;; being able to do RUN4 without it being a waste of time." This is the answer,
;; recomputed rather than remembered. `run4_prereg_transcribe.bb` fixes the
;; manifest -- what an acceptance would assert and which authorities it is pinned
;; to; this script compares those pins to the live repositories and reports READY
;; or BLOCKED-ON [named lines], writing READINESS.edn.
;;
;; WHAT THIS IS NOT. It accepts nothing, closes no hole and writes no ruling.
;; READY would mean the evidence an acceptance needs is intact and current; it
;; would not mean the acceptance has been made, nor that any particular run
;; qualifies -- the RUN4 ruling reserves that judgement (worklist.edn
;; :run4-lean-ruling, Joe 2026-09-03).
;;
;; A LINE NOBODY CAN DERIVE IS NOT A LINE THAT PASSED. Where a declared
;; requirement cannot be derived from a live source the line is :not-derivable
;; and BLOCKS -- the flip-readiness gate's rule (flip_readiness_check.bb), kept
;; because the failure it prevents is the same one: a requirement that silently
;; drops out of the count is a requirement nobody checked.
;;
;; THE NINE LINES, and the live source each is derived from:
;;   :certificates       runs/*/certificate.edn. The manifest must name exactly
;;                       the certificates that are live and awaiting acceptance:
;;                       one minted or accepted since the manifest was generated
;;                       makes the manifest a statement about a different set.
;;   :definitions-intact mathlib4 Holes.lean. Each certificate's generated
;;                       lean-block.lean must occur VERBATIM in the live file,
;;                       and the committed Run4Preregistration.lean artifact must
;;                       equal the module mathlib4 holds. A pin by commit alone
;;                       would go red every time Holes.lean moved for an
;;                       unrelated reason, and would go green on a restatement
;;                       that happened not to move it.
;;   :wiring-pin         p4ng control-map-edges.edn: live sha256 against the
;;                       pinned one, and its last commit against the pinned
;;                       commit. This is where a post-pin :decisions entry -- the
;;                       one declared invalidator that WOULD touch the
;;                       certificates' definitions -- becomes visible.
;;   :run-pins           each certificate's extracted trace: live sha256 against
;;                       the pinned one, plus tracked-and-clean in git, because a
;;                       pin naming bytes that are not in the object store names
;;                       nothing a reviewer can fetch.
;;   :regenerates        the producers re-run: each certificate's six artifacts
;;                       and the manifest's own artifacts must come back
;;                       byte-identical. This is the strongest single line -- it
;;                       re-derives every literal from the live sources rather
;;                       than comparing recorded numbers.
;;   :lean-probe         the committed axiom-probe receipt. Green only if it was
;;                       taken at the live Holes.lean and the live preregistration
;;                       module, every probed theorem reports no axioms, and the
;;                       module carries no sorry. Missing -> :not-derivable;
;;                       taken at other bytes -> :stale. Both block.
;;   :hole-open          holes-contract.json: wmRunConformsToWiring still typed a
;;                       hole (an acceptance over an already-closed declaration
;;                       would assert nothing), and the contract's source.git-sha
;;                       still the last commit that touched Holes.lean (C175's
;;                       comparand, not mathlib HEAD).
;;   :closability-audit  runs/U27-hole-closability/audit.edn, for the two holes
;;                       RUN4's own row names. Blocks if the audit was derived at
;;                       a different contract authority than the live one, or if
;;                       a named row is :readiness :not-ready.
;;   :invalidators       runs/F2-run4-preregistration/03-invalidators.edn: every
;;                       declared item carries an executable measurement, and the
;;                       item measured as touching the certificates' definitions
;;                       has not landed. The landing check DELEGATES to
;;                       :wiring-pin rather than re-measuring, so the two cannot
;;                       disagree.
;;
;; REPORTED AND NOT GATED: :not-a-requirement carries the two runs'
;; :selection-discrimination verdicts. They bear on WHICH RUN QUALIFIES, which
;; the RUN4 ruling reserves to Joe at certificate time; turning them into a line
;; would be writing that ruling here.
;;
;; Exit 0 all clear, 1 on any failure (house convention). A :blocked VERDICT is
;; not a failure: it is the answer.

(require '[babashka.fs :as fs]
         '[babashka.process :as process]
         '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def code-root (str (System/getProperty "user.home") "/code/"))
(def args (set *command-line-args*))

(def line-order
  [:certificates :definitions-intact :wiring-pin :run-pins :regenerates
   :lean-probe :hole-open :closability-audit :invalidators])

(def default-world
  {:manifest-dir   "runs/F2-run4-preregistration"
   :readiness-dir  "runs/F2-run4-readiness"
   :runs-dir       "runs"
   :lab            "."
   :holes-lean     (str code-root "mathlib4/DarkTower/WarMachine/Holes.lean")
   :prereg-lean    (str code-root "mathlib4/DarkTower/WarMachine/Run4Preregistration.lean")
   :contract       (str code-root "mathlib4/DarkTower/WarMachine/holes-contract.json")
   :control-map    (str code-root "p4ng/empirics-futon/control-map-edges.edn")
   :audit          "runs/U27-hole-closability/audit.edn"
   :mathlib-root   (str code-root "mathlib4")
   :p4ng-root      (str code-root "p4ng")
   :futon2-root    (str code-root "futon2")
   :regenerate?    true})

;; ---------------------------------------------------------------- helpers ---

(defn sha256 [path]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (str/join (map #(format "%02x" %) (.digest d (fs/read-all-bytes path))))))

(defn read-edn* [path]
  (when (fs/regular-file? path)
    (edn/read-string {:default (fn [_ v] v)} (slurp (str path)))))

(defn git [dir & argv]
  (let [r (apply process/shell {:continue true :out :string :err :string :dir dir} argv)]
    (if (zero? (:exit r)) (str/trim (:out r)) nil)))

(defn line
  "One readiness line. `blocked-on` is a vector of sentences a reader can act on;
   a line with any of them is :blocked. The vocabulary is deliberately two-valued
   plus :not-derivable, which is a :blocked with its reason stated."
  [verdict m blocked-on]
  (assoc m :verdict (if (seq blocked-on) (or verdict :blocked) :green)
         :blocked-on (vec blocked-on)))

;; ------------------------------------------------------------ the derivation

(defn derive-lines [w]
  (let [manifest-dir (:manifest-dir w)
        assertions (read-edn* (str manifest-dir "/01-assertions.edn"))
        authorities (read-edn* (str manifest-dir "/02-authorities.edn"))
        invalidators (read-edn* (str manifest-dir "/03-invalidators.edn"))
        probe      (read-edn* (str manifest-dir "/axiom-probe.edn"))
        holes-text (when (fs/regular-file? (:holes-lean w)) (slurp (:holes-lean w)))

        ;; live certificates, discovered the same way the producer discovers them
        live-certs
        (->> (fs/list-dir (:runs-dir w))
             (map #(fs/file % "certificate.edn"))
             (filter fs/regular-file?)
             (map (fn [f] (assoc (read-edn* (str f)) ::path (str f) ::dir (str (fs/parent f)))))
             (filter #(= :minted-awaiting-acceptance (:status %)))
             (sort-by #(get-in % [:run :name]))
             vec)

        auth-by-key (into {} (map (juxt :key identity) (:authorities authorities)))

        ;; ---- LINE 1 -------------------------------------------------------
        manifest-runs (vec (sort (distinct (map :run (:assertions assertions)))))
        live-runs (mapv #(get-in % [:run :name]) live-certs)
        l-certificates
        (line :blocked
              {:manifest-runs manifest-runs :live-runs live-runs
               :manifest-assertions (count (:assertions assertions))
               :manifest-dir manifest-dir}
              (cond-> []
                (nil? assertions)
                (conj (str "the manifest's assertion set is missing at " manifest-dir "/01-assertions.edn"
                           " -- nothing states what an acceptance would assert (:not-derivable)"))
                (empty? live-certs)
                (conj "no certificate under runs/ is :minted-awaiting-acceptance -- there is nothing to accept")
                (and assertions (seq live-certs) (not= manifest-runs (vec (sort live-runs))))
                (conj (format "the manifest names runs %s while the live awaiting-acceptance certificates are %s -- regenerate the manifest"
                              (pr-str manifest-runs) (pr-str (vec (sort live-runs)))))))

        ;; ---- LINE 2 -------------------------------------------------------
        block-rows
        (vec (for [c live-certs
                   :let [bp (str (::dir c) "/lean-block.lean")]]
               {:run (get-in c [:run :name])
                :block bp
                :present? (fs/regular-file? bp)
                :verbatim? (boolean (and holes-text (fs/regular-file? bp)
                                         (str/includes? holes-text (str/trim (slurp bp)))))}))
        artifact-prereg (str manifest-dir "/Run4Preregistration.lean")
        prereg-match?
        (and (fs/regular-file? artifact-prereg) (fs/regular-file? (:prereg-lean w))
             (= (sha256 artifact-prereg) (sha256 (:prereg-lean w))))
        l-definitions
        (line :blocked
              {:blocks block-rows
               :prereg-artifact artifact-prereg
               :prereg-module (:prereg-lean w)
               :prereg-identical? prereg-match?}
              (cond-> []
                (nil? holes-text)
                (conj (str "Holes.lean not found at " (:holes-lean w) " (:not-derivable)"))
                :always
                (into (for [r block-rows :when (not (:verbatim? r))]
                        (format "the %s certificate's generated Lean block is not verbatim in the live Holes.lean -- the theorems it is pinned to have been restated or removed"
                                (:run r))))
                (not prereg-match?)
                (conj (str "the committed preregistration artifact and the module mathlib4 holds differ"
                           " -- regenerate and copy, or the manifest in the repo is not the manifest Lean checks"))))

        ;; ---- LINE 3 -------------------------------------------------------
        cm-pin (get auth-by-key "control-map")
        cm-live-sha (when (fs/regular-file? (:control-map w)) (sha256 (:control-map w)))
        cm-last (git (:p4ng-root w) "git" "log" "-1" "--format=%H" "--"
                     "empirics-futon/control-map-edges.edn")
        cm-commit-ok? (boolean (and cm-last (:git-commit cm-pin)
                                    (str/starts-with? cm-last (:git-commit cm-pin))))
        l-wiring
        (line :blocked
              {:pinned (:identity cm-pin) :live cm-live-sha
               :pinned-commit (:git-commit cm-pin) :live-last-commit cm-last
               :path (:control-map w)}
              (cond-> []
                (nil? cm-pin) (conj "the manifest declares no control-map authority (:not-derivable)")
                (nil? cm-live-sha) (conj (str "the control map is not at " (:control-map w) " (:not-derivable)"))
                (and cm-pin cm-live-sha (not= (:identity cm-pin) cm-live-sha))
                (conj (format "the drawn control map has moved: pinned sha256 %s, live %s -- the certificates were judged against a map that is no longer the map"
                              (:identity cm-pin) cm-live-sha))
                (and cm-pin cm-last (not cm-commit-ok?))
                (conj (format "the control map's last commit is %s, not the pinned %s -- a :decisions entry may have landed since the pin"
                              (subs cm-last 0 (min 7 (count cm-last))) (:git-commit cm-pin)))))

        ;; ---- LINE 4 -------------------------------------------------------
        trace-rows
        (vec (for [c live-certs
                   :let [rel (str "holes/labs/wm-contract/" (get-in c [:run :extracted-trace]))
                         path (str (:lab w) "/" (get-in c [:run :extracted-trace]))
                         pin (get auth-by-key (str "trace:" (get-in c [:run :name])))]]
               {:run (get-in c [:run :name])
                :path path
                :pinned (:identity pin)
                :live (when (fs/regular-file? path) (sha256 path))
                :tracked? (boolean (seq (or (git (:futon2-root w) "git" "ls-files" "--" rel) "")))
                :dirty? (boolean (seq (or (git (:futon2-root w) "git" "status" "--porcelain" "--" rel) "")))}))
        l-run-pins
        (line :blocked
              {:runs trace-rows}
              (into []
                    (concat
                     (for [r trace-rows :when (nil? (:live r))]
                       (format "the %s run's extracted trace is not at %s (:not-derivable)" (:run r) (:path r)))
                     (for [r trace-rows :when (and (:live r) (not= (:pinned r) (:live r)))]
                       (format "the %s run's trace has changed since the pin: pinned %s, live %s"
                               (:run r) (:pinned r) (:live r)))
                     (for [r trace-rows :when (not (:tracked? r))]
                       (format "the %s run's trace is untracked -- the pin names bytes no reviewer can fetch" (:run r)))
                     (for [r trace-rows :when (:dirty? r)]
                       (format "the %s run's trace is dirty in the worktree" (:run r))))))

        ;; ---- LINE 5 -------------------------------------------------------
        regen
        (when (:regenerate? w)
          (let [tmp (str (fs/create-temp-dir {:prefix "run4-readiness-"}))
                cert-rows
                (vec (for [c live-certs
                           :let [d (str tmp "/" (get-in c [:run :name]))
                                 env (into {} (:produced-with c))
                                 r (process/shell {:continue true :out :string :err :string
                                                   :dir (:lab w) :extra-env env}
                                                  "bb" "u49_route_transcribe.bb" d)
                                 files ["00-source.edn" "01-topology.edn" "02-routes.edn"
                                        "03-classification.edn" "04-controls.edn" "lean-block.lean"]]]
                       {:run (get-in c [:run :name])
                        :exit (:exit r)
                        :differing (vec (for [f files
                                              :when (not (and (fs/regular-file? (str d "/" f))
                                                              (= (sha256 (str d "/" f))
                                                                 (sha256 (str (::dir c) "/" f)))))]
                                          f))}))
                mdir (str tmp "/manifest")
                mres (process/shell {:continue true :out :string :err :string :dir (:lab w)}
                                    "bb" "run4_prereg_transcribe.bb" mdir)
                mfiles ["00-source.edn" "01-assertions.edn" "02-authorities.edn"
                        "03-invalidators.edn" "04-controls.edn" "Run4Preregistration.lean"]
                mdiff (vec (for [f mfiles
                                 :when (not (and (fs/regular-file? (str mdir "/" f))
                                                 (fs/regular-file? (str manifest-dir "/" f))
                                                 (= (sha256 (str mdir "/" f))
                                                    (sha256 (str manifest-dir "/" f)))))]
                             f))]
            (fs/delete-tree tmp)
            {:certificates cert-rows :manifest-exit (:exit mres) :manifest-differing mdiff}))
        l-regenerates
        (if-not (:regenerate? w)
          {:verdict :not-derivable :blocked-on ["regeneration was skipped (F2_SKIP_REGEN)"] :skipped? true}
          (line :blocked
                regen
                (into []
                      (concat
                       (for [r (:certificates regen) :when (not (zero? (:exit r)))]
                         (format "the U49 producer exited %d re-running the %s certificate" (:exit r) (:run r)))
                       (for [r (:certificates regen) :when (seq (:differing r))]
                         (format "the %s certificate does not regenerate byte-identically from the live sources: %s"
                                 (:run r) (str/join " " (:differing r))))
                       (when-not (zero? (:manifest-exit regen))
                         [(format "the preregistration producer exited %d" (:manifest-exit regen))])
                       (when (seq (:manifest-differing regen))
                         [(format "the committed manifest does not regenerate byte-identically: %s"
                                  (str/join " " (:manifest-differing regen)))])))))

        ;; ---- LINE 6 -------------------------------------------------------
        live-holes-sha (when holes-text (sha256 (:holes-lean w)))
        live-prereg-sha (when (fs/regular-file? (:prereg-lean w)) (sha256 (:prereg-lean w)))
        l-probe
        (line :blocked
              {:receipt (str manifest-dir "/axiom-probe.edn")
               :taken-at-holes (:holes-lean-sha256 probe)
               :live-holes live-holes-sha
               :taken-at-prereg (:prereg-sha256 probe)
               :live-prereg live-prereg-sha
               :theorems (:theorems probe)
               :sorry-in-module (:sorry-in-module probe)
               :build (:lake-build probe)}
              (cond-> []
                (nil? probe)
                (conj (str "no axiom probe receipt at " manifest-dir "/axiom-probe.edn"
                           " -- run --probe (:not-derivable)"))
                (and probe (not= (:holes-lean-sha256 probe) live-holes-sha))
                (conj "the axiom probe was taken at a different Holes.lean than the live one (:stale) -- re-run --probe")
                (and probe (not= (:prereg-sha256 probe) live-prereg-sha))
                (conj "the axiom probe was taken at a different preregistration module than the live one (:stale) -- re-run --probe")
                (and probe (not= :ok (:lake-build probe)))
                (conj (str "the probe receipt does not record a green lake build: " (pr-str (:lake-build probe))))
                (and probe (pos? (or (:sorry-in-module probe) 0)))
                (conj (format "the preregistration module carries %d sorry" (:sorry-in-module probe)))
                :always
                (into (for [t (:theorems probe) :when (seq (:axioms t))]
                        (format "%s depends on axioms %s" (:name t) (pr-str (:axioms t)))))))

        ;; ---- LINE 7 -------------------------------------------------------
        contract (when (fs/regular-file? (:contract w))
                   (json/parse-string (slurp (:contract w)) true))
        decl (first (filter #(= "wmRunConformsToWiring" (:name %)) (:declarations contract)))
        holes-last (git (:mathlib-root w) "git" "log" "-1" "--format=%H" "--"
                        "DarkTower/WarMachine/Holes.lean")
        authority (get-in contract [:source :git-sha])
        l-hole
        (line :blocked
              {:declaration (:name decl) :kind (:kind decl)
               :contract-authority authority
               :holes-last-content-change holes-last
               :comparand "the last commit that touched Holes.lean, not mathlib HEAD (C175)"}
              (cond-> []
                (nil? contract) (conj (str "the emitted contract is not at " (:contract w) " (:not-derivable)"))
                (nil? decl) (conj "the contract declares no wmRunConformsToWiring -- the declaration an acceptance closes is not there")
                (and decl (not= "hole" (:kind decl)))
                (conj (format "wmRunConformsToWiring is typed %s, not a hole -- there is nothing left for an acceptance to close"
                              (pr-str (:kind decl))))
                (and authority holes-last (not= authority holes-last))
                (conj (format "the emitted contract is pinned at %s while Holes.lean last changed at %s -- re-emit"
                              (subs authority 0 7) (subs holes-last 0 7)))))

        ;; ---- LINE 8 -------------------------------------------------------
        audit (read-edn* (:audit w))
        ;; The two holes RUN4's own row names: the one an acceptance closes, and
        ;; the H1b bound its statement puts on the same run.
        run4-holes ["wmRunConformsToWiring" "enactedEqualsSelectedWhenRankOneGated"]
        audit-rows (into {} (map (juxt :name identity) (:rows audit)))
        audit-authority (get-in audit [:authority :contract-git-sha])
        l-audit
        (line :blocked
              {:audit (:audit w)
               :audit-as-of (:as-of audit)
               :audit-authority audit-authority
               :live-authority authority
               :rows (into (sorted-map)
                           (for [n run4-holes]
                             [n (select-keys (get audit-rows n) [:closability :readiness :hole-class :non-run-blockers :blocker :blockers])]))}
              (cond-> []
                (nil? audit) (conj (str "the closability audit is not at " (:audit w) " (:not-derivable)"))
                (and audit authority (not= audit-authority authority))
                (conj (format "the closability audit was derived at contract %s (:as-of %s) while the live authority is %s -- its readiness typings are about a different contract"
                              (subs (str audit-authority) 0 (min 7 (count (str audit-authority))))
                              (:as-of audit) (subs (str authority) 0 7)))
                :always
                (into (for [n run4-holes
                            :let [r (get audit-rows n)]
                            :when (or (not= :run-gated (:closability r))
                                      (seq (:non-run-blockers r))
                                      (some? (:blocker r))
                                      (seq (:blockers r)))]
                        (if r
                          (format "the closability audit types %s :readiness %s (:closability %s) -- requires run-gated with no non-run blockers; blockers %s"
                                  n (pr-str (:readiness r)) (pr-str (:closability r))
                                  (pr-str (select-keys r [:non-run-blockers :blocker :blockers])))
                          (format "the closability audit carries no row for %s, which RUN4's row names (:not-derivable)" n))))))

        ;; ---- LINE 9 -------------------------------------------------------
        inv (:invalidators invalidators)
        unmeasured (filterv #(or (str/blank? (str (:measured-by %))) (empty? (:measurements %))) inv)
        touching (filterv :touches-certificate-definitions? inv)
        wiring-green? (= :green (:verdict l-wiring))
        l-invalidators
        (line :blocked
              {:declared (mapv :id inv)
               :touching (mapv :id touching)
               :unmeasured (mapv :id unmeasured)
               ;; The landing check is DELEGATED, not re-measured: :wiring-pin is
               ;; the comparison that would see a post-pin :decisions entry, and
               ;; a second measurement here could disagree with it.
               :landing-check {:post-pin-control-map-decisions :wiring-pin}
               :landed? (not wiring-green?)}
              (cond-> []
                (nil? invalidators)
                (conj (str "the declared invalidation set is missing at " manifest-dir "/03-invalidators.edn (:not-derivable)"))
                (and invalidators (empty? inv))
                (conj "the declared invalidation set is empty -- a manifest that declares no way to go stale has not been checked")
                :always
                (into (for [i unmeasured]
                        (format "declared invalidator %s carries no executable measurement (:not-derivable)" (:id i))))
                (and (seq touching) (not wiring-green?))
                (conj (format "an invalidator measured as touching the certificates' definitions has landed: %s -- see :wiring-pin"
                              (str/join " " (map :id touching))))))]

    {:certificates l-certificates
     :definitions-intact l-definitions
     :wiring-pin l-wiring
     :run-pins l-run-pins
     :regenerates l-regenerates
     :lean-probe l-probe
     :hole-open l-hole
     :closability-audit l-audit
     :invalidators l-invalidators}))

;; ------------------------------------------------------------- the verdict --

(defn verdict-of [lines]
  (if (every? #(= :green (:verdict (get lines %))) line-order) :ready :blocked))

(defn blockers-of [lines]
  (vec (for [k line-order
             b (:blocked-on (get lines k))]
         (str (name k) ": " b))))

(defn verdict-block [lines]
  (str/join "\n"
            (concat ["```"]
                    (for [k line-order]
                      (let [v (get lines k)]
                        (format "RUN4 %-19s %s" (name k)
                                (if (= :green (:verdict v))
                                  "GREEN"
                                  (str "BLOCKED-ON [" (count (:blocked-on v)) "]")))))
                    [(format "VERDICT: %s"
                             (if (= :ready (verdict-of lines))
                               "READY"
                               (str "BLOCKED-ON ["
                                    (str/join " " (map name (filter #(not= :green (:verdict (get lines %))) line-order)))
                                    "]")))]
                    (for [b (blockers-of lines)] (str "  - " b))
                    ["```"])))

(defn not-a-requirement [w candidate-runs]
  {:note (str "REPORTED, NOT GATED. These bear on WHICH RUN QUALIFIES, and the RUN4 ruling "
              "reserves that judgement to Joe at certificate time. Turning them into a line "
              "would be writing his ruling here.")
   ;; The candidates are named, because the RE7 directory holds verdicts for
   ;; runs that are not candidates and a bare list would read as if they were.
   :candidate-runs candidate-runs
   :selection-discrimination
   (into (sorted-map)
         (for [d (sort (map str (fs/list-dir (str (:runs-dir w) "/RE7-selection-discrimination"))))
               :let [f (str d "/01-decisions.edn")]
               :when (fs/regular-file? f)]
           [(fs/file-name d) (:verdict (read-edn* f))]))
   :censuses-identical
   (str "both certified runs walk the same nine-hop route and their censuses agree hop for hop "
        "(runs/RE5-run-conformance/certificate.edn :limits) -- two runs agreeing about a route "
        "they were always going to take is one measurement repeated, not two")})

(defn sidecar [w lines]
  {:schema :wm/run4-readiness-v1
   :row :F2
   ;; THE JOIN KEY. voxterm's /agency/backlog reads this file and attaches
   ;; :verdict and :blocked-on to the row named here, so the RUN4 row in
   ;; needs-Joe carries its readiness without a bb call per poll.
   :subject {:board "wm-contract" :item :RUN4}
   :verdict (verdict-of lines)
   :blocked-on (blockers-of lines)
   :summary (if (= :ready (verdict-of lines))
              "READY"
              (str "BLOCKED-ON ["
                   (str/join " " (map name (filter #(not= :green (:verdict (get lines %))) line-order)))
                   "]"))
   :emitted-at (str (java.time.LocalDate/now))
   :emitted-by "futon2/holes/labs/wm-contract/run4_readiness.bb"
   :manifest "futon2/holes/labs/wm-contract/runs/F2-run4-preregistration"
   :preregistration "mathlib4/DarkTower/WarMachine/Run4Preregistration.lean"
   :accepts-nothing (str "This artifact performs no acceptance and writes no ruling. READY means "
                         "the evidence an acceptance needs is intact and current; it is not "
                         "permission, and it does not say which run qualifies.")
   :line-order line-order
   :lines lines
   :not-a-requirement
   (not-a-requirement w (vec (sort (distinct (map :run (:assertions
                                                        (read-edn* (str (:manifest-dir w)
                                                                        "/01-assertions.edn")))))))) })

;; --------------------------------------------------------------- --probe ----

(defn take-probe! [w]
  (let [names ["run4CertifiedS5Conformance" "run4CertifiedS5Census"
               "run4CertifiedRe5Conformance" "run4CertifiedRe5Census"
               "wmRun4PreregCensus" "wmRun4InvalidatorsTouchingDefinitions"]
        build (process/shell {:continue true :out :string :err :string :dir (:mathlib-root w)}
                             "lake" "build" "DarkTower.WarMachine.Run4Preregistration")
        build-out (str (:out build) (:err build))
        sorries (count (re-seq #"Run4Preregistration\.lean:\d+:\d+: declaration uses `sorry`" build-out))
        probe-file (str (fs/create-temp-file {:prefix "run4-axprobe-" :suffix ".lean"}))
        _ (spit probe-file
                (str "import DarkTower.WarMachine.Run4Preregistration\n"
                     "open DarkTower.WarMachine.Run4Preregistration\n"
                     (str/join "\n" (for [n names] (str "#print axioms " n))) "\n"))
        r (process/shell {:continue true :out :string :err :string :dir (:mathlib-root w)}
                         "lake" "env" "lean" probe-file)
        out (str (:out r) (:err r))
        rows (vec (for [n names]
                    (let [pat (re-pattern (str "'[^']*\\." n "' (does not depend on any axioms|depends on axioms: \\[([^\\]]*)\\])"))
                          m (re-find pat out)]
                      {:name n
                       :axioms (cond
                                 (nil? m) [:not-reported]
                                 (= "does not depend on any axioms" (second m)) []
                                 :else (mapv str/trim (str/split (or (nth m 2) "") #",")))})))]
    (fs/delete probe-file)
    {:schema :wm/run4-axiom-probe-v1
     :row :F2
     :taken-by "futon2/holes/labs/wm-contract/run4_readiness.bb --probe"
     :module "DarkTower.WarMachine.Run4Preregistration"
     :holes-lean-sha256 (sha256 (:holes-lean w))
     :prereg-sha256 (sha256 (:prereg-lean w))
     :lake-build (if (zero? (:exit build)) :ok :failed)
     :lake-build-jobs (second (re-find #"Build completed successfully \((\d+) jobs\)" build-out))
     :probe-exit (:exit r)
     :sorry-in-module sorries
     :theorems rows
     :note (str "The receipt is what the :lean-probe line reads. It is pinned to the BYTES of "
                "both files, so a Holes.lean or a module edit makes it :stale rather than "
                "silently keeping a green line that was true of other bytes.")}))

;; -------------------------------------------------------------- --negative --
;;
;; Each plant perturbs ONE input in a copy of the world and must move ONE named
;; line from :green to :blocked. Without these the meter would be a check that
;; cannot fail: every line above is green today, which is exactly the state in
;; which a broken comparison is invisible.

(defn- copy-world!
  "A temp world: the manifest directory copied, everything else pointed at
   copies of the files a plant needs to perturb."
  [w]
  (let [tmp (str (fs/create-temp-dir {:prefix "run4-negative-"}))]
    (fs/copy-tree (:manifest-dir w) (str tmp "/manifest"))
    (fs/copy (:holes-lean w) (str tmp "/Holes.lean"))
    (fs/copy (:prereg-lean w) (str tmp "/Run4Preregistration.lean"))
    (fs/copy (:contract w) (str tmp "/holes-contract.json"))
    (fs/copy (:audit w) (str tmp "/audit.edn"))
    [tmp (assoc w
                :manifest-dir (str tmp "/manifest")
                :holes-lean (str tmp "/Holes.lean")
                :prereg-lean (str tmp "/Run4Preregistration.lean")
                :contract (str tmp "/holes-contract.json")
                :audit (str tmp "/audit.edn")
                :regenerate? false)]))

(defn- edit! [path f]
  (spit path (f (slurp path))))

(defn- edit-edn!
  "Plants that target an EDN artifact edit the DATA and re-print it. String
   surgery on a pretty-printed artifact is what the first version of these
   plants did, and two of the nine silently matched nothing -- a negative
   control that plants nothing reports the check as sound."
  [path f]
  (spit path (with-out-str (pp/pprint (f (read-edn* path))))))

(defn- perturb-identity
  "Flip the last hex digit of the pinned identity under `key`."
  [m key]
  (update m :authorities
          (fn [as] (mapv (fn [a]
                           (if (= key (:key a))
                             (update a :identity #(str (subs % 0 (dec (count %)))
                                                       (if (= \0 (last %)) \1 \0)))
                             a))
                         as))))

(def plants
  [{:id :N1 :line :wiring-pin
    :what "the pinned control-map sha256 perturbed in the manifest"
    :plant (fn [tmp _] (edit-edn! (str tmp "/manifest/02-authorities.edn")
                                  #(perturb-identity % "control-map")))}
   {:id :N2 :line :run-pins
    :what "a pinned trace sha256 perturbed in the manifest"
    :plant (fn [tmp _] (edit-edn! (str tmp "/manifest/02-authorities.edn")
                                  #(perturb-identity % "trace:2026-09-01-s5")))}
   {:id :N3 :line :lean-probe
    :what "the probe receipt's Holes.lean hash altered -- a probe taken at other bytes"
    :plant (fn [tmp _] (edit-edn! (str tmp "/manifest/axiom-probe.edn")
                                  #(assoc % :holes-lean-sha256 (str/join (repeat 64 "0")))))}
   {:id :N4 :line :lean-probe
    :what "the probe receipt reports an axiom dependency"
    :plant (fn [tmp _] (edit-edn! (str tmp "/manifest/axiom-probe.edn")
                                  #(update % :theorems
                                           (fn [ts] (assoc-in (vec ts) [0 :axioms] ["propext"])))))}
   {:id :N5 :line :definitions-intact
    :what "the preregistration module edited away from the committed artifact"
    :plant (fn [tmp _] (edit! (str tmp "/Run4Preregistration.lean") #(str % "\n-- planted\n")))}
   {:id :N6 :line :definitions-intact
    :what "a certificate's transcribed block no longer verbatim in Holes.lean"
    :plant (fn [tmp _] (edit! (str tmp "/Holes.lean")
                              #(str/replace % "def s5Routes" "def s5RoutesPlanted")))}
   {:id :N7 :line :invalidators
    :what "a declared invalidator stripped of its measurement"
    :plant (fn [tmp _] (edit-edn! (str tmp "/manifest/03-invalidators.edn")
                                  #(update % :invalidators
                                           (fn [is] (-> (vec is)
                                                        (assoc-in [0 :measured-by] "")
                                                        (assoc-in [0 :measurements] {}))))))}
   {:id :N8 :line :hole-open
    :what "the contract types wmRunConformsToWiring closed"
    :plant (fn [tmp _] (edit! (str tmp "/holes-contract.json")
                              #(str/replace % "\"kind\":\"hole\",\"name\":\"wmRunConformsToWiring\""
                                            "\"kind\":\"closed\",\"name\":\"wmRunConformsToWiring\"")))}
   {:id :N9 :line :certificates
    :what "the manifest's assertion set emptied"
    :plant (fn [tmp _] (edit-edn! (str tmp "/manifest/01-assertions.edn")
                                  #(assoc % :assertions [])))}
   {:id :N10 :line :closability-audit
    :what "the closability audit loses the row for a hole RUN4's own row names"
    :plant (fn [tmp _] (edit-edn! (str tmp "/audit.edn")
                                  #(update % :rows
                                           (fn [rs] (vec (remove (fn [r] (= "wmRunConformsToWiring" (:name r))) rs))))))}])

(def plants-with-blocker
  (conj plants
        {:id :N11 :line :closability-audit
         :what "a run-gated hole carries a non-run implementation blocker"
         :plant (fn [tmp _]
                  (edit-edn! (str tmp "/audit.edn")
                             #(update % :rows
                                      (fn [rs]
                                        (mapv (fn [r]
                                                (if (= "wmRunConformsToWiring" (:name r))
                                                  (assoc r :non-run-blockers [:implementation-missing])
                                                  r)) rs)))))}))

(defn run-negative! [w]
  (let [base (derive-lines (assoc w :regenerate? false))
        rows (vec (for [p plants-with-blocker]
                    (let [[tmp w'] (copy-world! w)
                          _ ((:plant p) tmp w')
                          lines (derive-lines w')
                          before (:verdict (get base (:line p)))
                          after (:verdict (get lines (:line p)))
                          ;; CAUGHT means the plant produced a blocker the base
                          ;; derivation does not carry -- not merely that the line
                          ;; is blocked. A line already red for another reason
                          ;; would otherwise report every plant against it as
                          ;; caught, which is the failure this suite exists to
                          ;; rule out.
                          new-blockers (vec (remove (set (:blocked-on (get base (:line p))))
                                                    (:blocked-on (get lines (:line p)))))
                          caught? (and (not= :green after) (seq new-blockers))]
                      (fs/delete-tree tmp)
                      {:id (:id p) :line (:line p) :what (:what p)
                       :before before :after after
                       :new-blockers new-blockers
                       :caught? (boolean caught?)})))]
    {:base-verdicts (into (sorted-map) (for [k line-order] [k (:verdict (get base k))]))
     :plants rows
     :caught (count (filter :caught? rows))
     :missed (mapv :id (remove :caught? rows))}))

;; ------------------------------------------------------------------ main ----

(def world
  (cond-> default-world
    (System/getenv "F2_SKIP_REGEN") (assoc :regenerate? false)))

(def readiness-path (str (:readiness-dir world) "/READINESS.edn"))
(def readme-path (str (:readiness-dir world) "/README.md"))

(cond
  (args "--probe")
  (let [r (take-probe! world)]
    (io/make-parents (io/file (str (:manifest-dir world) "/axiom-probe.edn")))
    (spit (str (:manifest-dir world) "/axiom-probe.edn") (with-out-str (pp/pprint r)))
    (println "run4_readiness --probe:" (:lake-build r) "| sorry" (:sorry-in-module r)
             "|" (count (remove #(empty? (:axioms %)) (:theorems r))) "of" (count (:theorems r))
             "theorems carry axioms ->" (str (:manifest-dir world) "/axiom-probe.edn"))
    (System/exit (if (and (= :ok (:lake-build r))
                          (zero? (:sorry-in-module r))
                          (every? #(empty? (:axioms %)) (:theorems r)))
                   0 1)))

  (args "--negative")
  (let [r (run-negative! world)]
    (when (args "--emit")
      (io/make-parents (io/file (str (:readiness-dir world) "/negative-controls.edn")))
      (spit (str (:readiness-dir world) "/negative-controls.edn")
            (with-out-str (pp/pprint r)))
      (println "run4_readiness --negative --emit:" (str (:readiness-dir world) "/negative-controls.edn")))
    (doseq [p (:plants r)]
      (println (format "%-4s %-19s %-7s -> %-7s %s | %s"
                       (name (:id p)) (name (:line p)) (name (:before p)) (name (:after p))
                       (if (:caught? p) "CAUGHT" "MISSED") (:what p))))
    (println (format "run4_readiness --negative: %d of %d plants caught%s"
                     (:caught r) (count (:plants r))
                     (if (seq (:missed r)) (str "; MISSED " (str/join " " (map name (:missed r)))) "")))
    (System/exit (if (= (:caught r) (count (:plants r))) 0 1)))

  :else
  (let [lines (derive-lines world)
        block (verdict-block lines)
        emit? (args "--emit")]
    (cond
      (args "--summary")
      (do (println block) (System/exit 0))

      emit?
      (do (io/make-parents (io/file readiness-path))
          (spit readiness-path (with-out-str (pp/pprint (sidecar world lines))))
          (when (fs/regular-file? readme-path)
            (spit readme-path
                  (str/replace (slurp readme-path)
                               #"(?s)<!-- BEGIN run4_readiness -->.*?<!-- END run4_readiness -->"
                               (str/re-quote-replacement
                                (str "<!-- BEGIN run4_readiness -->\n" block "\n<!-- END run4_readiness -->")))))
          (println "run4_readiness: emitted" readiness-path)
          (System/exit 0))

      :else
      (let [committed (read-edn* readiness-path)
            problems
            (cond-> []
              (nil? committed)
              (conj (str "no committed READINESS.edn at " readiness-path " -- run --emit"))
              (and committed (not= (:lines committed) lines))
              (conj (str "the committed READINESS.edn disagrees with the live derivation on: "
                         (str/join " " (map name (for [k line-order
                                                       :when (not= (get (:lines committed) k) (get lines k))]
                                                   k)))))
              (and (fs/regular-file? readme-path) (not (str/includes? (slurp readme-path) block)))
              (conj (str "the committed " readme-path " does not carry the computed verdict block")))]
        (println block)
        (doseq [p problems] (println "  PROBLEM" p))
        (if (seq problems)
          (do (println (format "run4_readiness: FAIL (%d problems) exit-convention=0-pass/1-fail" (count problems)))
              (System/exit 1))
          (println "run4_readiness: PASS exit-convention=0-pass/1-fail"))))))
