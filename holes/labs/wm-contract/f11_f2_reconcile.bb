#!/usr/bin/env bb
;; :F11 slice 1 -- the F2-falsifier reconciliation, measured against the
;; current library state.
;;
;; P-validated-R5.md:490-497 (the C61 amendment, 2026-08-31) records the four
;; `findF*` bindings as stale "until the representations are reconciled and the
;; strengthened check passes".  Both happened (C73, C500).  What this script
;; measures is whether the reconciliation still holds at the library as it is
;; TODAY, and what moved in the pinned record since C500 measured it.
;;
;; It does NOT invoke `find_snatch.clj`, for the reason `u46_transcribe.bb`
;; does not: the positive path recomputes the report from the live library and
;; OVERWRITES `futon3:checks/find-snatch.edn`, which is the pinned fixture the
;; four Lean docstrings name (C500 s3).  The live re-run is therefore taken
;; once, by hand, and committed here as `01-find-snatch-live.edn`; the
;; comparison below is a pure function of that file and the pin, so it
;; reproduces.
;;
;;   --negative  mutate one live `:if-text` and confirm the classifier stops
;;               calling the difference lines-only.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str])

(def home (System/getenv "HOME"))
(def pin-path (str home "/code/futon3/checks/find-snatch.edn"))
(def live-path "runs/F11-find/01-find-snatch-live.edn")
(def out-path "runs/F11-find/02-reconciliation.edn")

(defn sha256 [path]
  (let [d (java.security.MessageDigest/getInstance "SHA-256")]
    (->> (.digest d (java.nio.file.Files/readAllBytes (.toPath (io/file path))))
         (map #(format "%02x" %))
         str/join)))

(defn receipt-pairs
  "Every (pinned receipt, live receipt) pair, keyed by scenario/round/pattern."
  [pin live]
  (for [[sp sl] (map vector (:scenarios pin) (:scenarios live))
        [rp rl] (map vector (:round-results sp) (:round-results sl))
        [id recp] (:receipts (:find rp))
        :let [recl (get-in rl [:find :receipts id])]
        :when recl]
    {:scenario [(:treatment sp) (:disposition sp)] :round (:round rp) :pattern id
     :pinned recp :live recl}))

(defn classify
  "Which fields of a receipt pair differ.  `:if-lines`/`:however-lines` are
   coordinates into the flexiarg; `:if-text`/`:however-text` are the clause
   itself.  F2 is about the clause, so the two must be counted apart."
  [{:keys [pinned live]}]
  (let [wp (:warrant pinned) wl (:warrant live)
        keys* (sort (distinct (concat (keys wp) (keys wl))))]
    (into (sorted-set)
          (concat (for [k (sort (distinct (concat (keys pinned) (keys live))))
                        :when (and (not= k :warrant) (not= (get pinned k) (get live k)))]
                    k)
                  (for [k keys* :when (not= (get wp k) (get wl k))]
                    (keyword "warrant" (name k)))))))

(defn report [pin live]
  (let [pairs (receipt-pairs pin live)
        diffs (remove (comp empty? classify) pairs)
        fields (frequencies (mapcat classify diffs))
        line-fields #{:warrant/if-lines :warrant/however-lines}
        text-fields #{:warrant/if-text :warrant/however-text :warrant/file :route}
        rounds (fn [xs] (count (distinct (map (juxt :scenario :round) xs))))
        all-rounds (rounds pairs)]
    (sorted-map
     :as-of-pin (:as-of pin)
     :as-of-live (:as-of live)
     :repository-count-pin (count (:repository pin))
     :repository-count-live (count (:repository live))
     :repository-added (vec (sort (remove (set (:repository pin)) (:repository live))))
     :repository-removed (vec (sort (remove (set (:repository live)) (:repository pin))))
     :drift-mismatch-count-live (:mismatch-count (:drift live))
     :laws-identical? (= (:laws pin) (:laws live))
     :receipts-compared (count pairs)
     :receipts-differing (count diffs)
     :rounds-total all-rounds
     :rounds-differing (rounds diffs)
     :differing-fields (into (sorted-map) fields)
     ;; The finding, stated so it can be false: every difference is a line
     ;; coordinate and no difference is a clause text, a warrant file or a
     ;; retrieval route.
     :difference-is-line-coordinates-only?
     (and (seq diffs)
          (every? #(every? line-fields (classify %)) diffs)
          (zero? (reduce + 0 (map #(get fields % 0) text-fields))))
     :shifted-patterns
     (into (sorted-map)
           (for [[id ps] (group-by :pattern diffs)
                 :let [p (first ps)]]
             [id (sorted-map
                  :if-lines-pin (get-in p [:pinned :warrant :if-lines])
                  :if-lines-live (get-in p [:live :warrant :if-lines])
                  :however-lines-pin (get-in p [:pinned :warrant :however-lines])
                  :however-lines-live (get-in p [:live :warrant :however-lines])
                  :if-text-identical? (= (get-in p [:pinned :warrant :if-text])
                                         (get-in p [:live :warrant :if-text]))
                  :however-text-identical? (= (get-in p [:pinned :warrant :however-text])
                                              (get-in p [:live :warrant :however-text])))])))))

(defn -main [& args]
  (let [pin (edn/read-string (slurp pin-path))
        live (edn/read-string (slurp live-path))]
    (if (some #{"--negative"} args)
      (let [mutated (update-in live [:scenarios 0 :round-results 0 :find :receipts
                                     (first (get-in live [:scenarios 0 :round-results 0 :find :selected]))
                                     :warrant :if-text]
                               (constantly "a clause the library does not carry"))
            r (report pin mutated)]
        (if (:difference-is-line-coordinates-only? r)
          (do (println "f11-f2-reconcile: FAIL a mutated clause text was still counted lines-only"
                       "exit-convention=0-pass/1-fail")
              (System/exit 2))
          (do (println "f11-f2-reconcile: PASS mutated clause text rejected"
                       "finding=:clause-text-differs exit-convention=0-pass/1-fail")
              (System/exit 0))))
      (let [r (assoc (report pin live)
                     :pin-path "futon3:checks/find-snatch.edn"
                     :pin-sha256 (sha256 pin-path)
                     :live-path live-path
                     :live-sha256 (sha256 live-path))]
        (io/make-parents out-path)
        (spit out-path (with-out-str (pprint/pprint r)))
        (println (format "f11-f2-reconcile: %d/%d receipts differ over %d/%d rounds; fields %s; lines-only? %s; live drift %d"
                         (:receipts-differing r) (:receipts-compared r)
                         (:rounds-differing r) (:rounds-total r)
                         (pr-str (keys (:differing-fields r)))
                         (:difference-is-line-coordinates-only? r)
                         (:drift-mismatch-count-live r)))
        (println (str "wrote " out-path))))))

(apply -main *command-line-args*)
