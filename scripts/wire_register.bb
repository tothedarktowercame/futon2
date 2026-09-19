#!/usr/bin/env bb
;; wire_register.bb — the real wires of the system, reviewable and queriable.
;;
;; Joe, 2026-09-19: "The warrants that the registry will produce will certify
;; only needed behaviour, required for running the machine. ... I'd like a
;; clear list of the areas that are being tested and warranted because these
;; are the real 'wires' of the system." The 27 ARGUE'd requirements are types
;; of wires or instances of wires.
;;
;; This JOINS the sources of truth live rather than materializing a copy:
;;   - p4ng/empirics-futon/aif-conformance.edn   Fig 5A/6a: drawn+theory edges
;;     (:conformant = drawn & equation-justified; :missing = theory demands,
;;      not realised; :plumbing = drawn, no equation; :unexplained = drawn,
;;      no justification — as-of its own :as-of stamp, currently 2026-09-01)
;;   - holes/labs/wm-contract/wire-requirements.edn   the 27, as wires
;;   - futon3c data/test-registry-validation/subjects.ednlog   live bindings
;;
;;   bb scripts/wire_register.bb                  # summary + everything
;;   bb scripts/wire_register.bb edges            # edge wires only
;;   bb scripts/wire_register.bb requirements     # the 27 only
;;   bb scripts/wire_register.bb unwarranted      # wires with no binding
;;   bb scripts/wire_register.bb form=type|instance
;;   bb scripts/wire_register.bb kind=aif-logical|plumbing|unexplained|missing

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(def home (System/getProperty "user.home"))
(def conformance-file (str home "/code/p4ng/empirics-futon/aif-conformance.edn"))
(def requirements-file (str home "/code/futon2/holes/labs/wm-contract/wire-requirements.edn"))
(def subjects-file (str home "/code/futon3c/data/test-registry-validation/subjects.ednlog"))

(defn read-subjects []
  (let [f (io/file subjects-file)]
    (if-not (.isFile f)
      {}
      (->> (str/split-lines (slurp f))
           (remove str/blank?)
           (map edn/read-string)
           (filter #(= :subject-binding (:entry/type %)))
           (reduce (fn [m e] (assoc m (:subject-id e) (:warrant-id e))) {})))))

(defn edge-rows [conf]
  (let [cls (fn [k kind] (map (fn [e] {:wire e :class k :kind kind}) (get conf k)))]
    (concat (cls :conformant :aif-logical)
            (cls :missing :aif-logical)
            (cls :plumbing :plumbing)
            (cls :unexplained :unexplained))))

(defn requirement-rows [reqs subjects]
  (map (fn [{:keys [id wire-form wires basis subject-id note]}]
         (let [sid (or subject-id id)]
           {:id id :form wire-form :wires wires :basis basis :note note
            :subject sid :warrant (get subjects sid)}))
       (:requirements reqs)))

(defn print-edges [rows]
  (println "\n== edge wires (control map vs equations, as-of conformance stamp)")
  (doseq [{:keys [wire class kind]} rows]
    (println (format "  %-14s %-12s %s"
                     (str (name (first wire)) "->" (name (second wire)))
                     (name kind) (name class)))))

(defn print-reqs [rows]
  (println "\n== requirement wires (the 27, ARGUE'd)")
  (doseq [{:keys [id form wires basis warrant]} rows]
    (println (format "  %-28s %-9s %-28s %s"
                     id (name form)
                     (if wires (pr-str wires) "-")
                     (if warrant (str "WARRANTED " (subs warrant 0 (min 22 (count warrant))) "…")
                         "unwarranted")))
    (println (format "  %28s basis: %s" "" basis))))

(let [conf (edn/read-string (slurp conformance-file))
      reqs (edn/read-string (slurp requirements-file))
      subjects (read-subjects)
      edges (edge-rows conf)
      rrows (requirement-rows reqs subjects)
      args (set *command-line-args*)
      kindf (some #(when (str/starts-with? % "kind=") (keyword (subs % 5))) args)
      formf (some #(when (str/starts-with? % "form=") (keyword (subs % 5))) args)
      edges (cond->> edges kindf (filter #(= kindf (:kind %))))
      rrows (cond->> rrows
              formf (filter #(= formf (:form %)))
              (args "unwarranted") (remove :warrant))]
  (println "wire register — conformance as-of" (:as-of conf)
           "| requirements as-of" (:as-of reqs)
           "| bindings" (count subjects))
  (when-not (or (args "requirements") formf)
    (print-edges edges))
  (when-not (args "edges")
    (print-reqs rrows))
  (println "\n== summary")
  (println "  edge wires:" (count edges) (pr-str (frequencies (map :class edges))))
  (println "  requirement wires:" (count rrows)
           (pr-str (frequencies (map :form rrows)))
           "| warranted" (count (filter :warrant rrows)) "of" (count rrows)))
