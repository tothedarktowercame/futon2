;; Validate overlay-v0.edn's structure against BOTH domains before building anything.
;;
;; WR-3: the diagram gets validated before the implementation. The predictions in
;; :overlay/predictions were written first; this script tries to break them.
;;
;; The ants' deltas are read from the .flexiarg SOURCE FILES, not from
;; cascade-ants.edn's transcription of them — so prediction :transcription-fidelity
;; can actually fail. Trusting the cascade's copy would be assuming exactly the
;; thing WR-2 says nothing checks.
;;
;; Run: cd ~/code/futon2/holes && bb check_overlay.clj

(require '[clojure.edn :as edn] '[clojure.string :as str] '[clojure.java.io :as io]
         '[babashka.process :refer [shell]])

(def OVERLAY (edn/read-string (slurp "overlay-v0.edn")))
(def ANTS-DIR "/home/joe/code/futon3/library/ants")

;; ---------------------------------------------------------------------------
;; Read @aif-delta straight out of each ant flexiarg.
;; ---------------------------------------------------------------------------
(defn read-delta [f]
  (let [txt (slurp f)]
    (when-let [i (str/index-of txt "@aif-delta")]
      (let [after (subs txt (+ i (count "@aif-delta")))
            ;; strip ;; comments before reading — the deltas are commented EDN
            cleaned (->> (str/split-lines after)
                         (map #(str/replace % #";;.*$" ""))
                         (str/join "\n"))]
        (try (edn/read-string cleaned) (catch Exception _ nil))))))

(def ants
  (->> (.listFiles (io/file ANTS-DIR))
       (filter #(str/ends-with? (.getName %) ".flexiarg"))
       (map (fn [f] {:id (str/replace (.getName f) ".flexiarg" "")
                     :delta (read-delta f)}))
       (sort-by :id)))

(println "=== OVERLAY STRUCTURE CHECK —" (:overlay/id OVERLAY))
(println "    status:" (:overlay/status OVERLAY) "| records:" (count (:overlay/records OVERLAY)))
(println "    ants read from SOURCE:" (count ants) "patterns\n")

;; ---------------------------------------------------------------------------
;; leaf paths of a nested delta -> the key-space merge-deep operates on
;; ---------------------------------------------------------------------------
(defn leaf-paths [m prefix]
  (mapcat (fn [[k v]] (if (map? v) (leaf-paths v (conj prefix k)) [[(conj prefix k) v]]))
          m))

(def ant-paths
  (mapcat (fn [{:keys [id delta]}] (map (fn [[p v]] {:ant id :path p :val v}) (leaf-paths (or delta {}) [])))
          ants))

(defn overlay-for [scope]
  (->> (:overlay/records OVERLAY)
       (filter #(and (= :define (:op %)) (= scope (:scope %))))
       (map (juxt :surface :maps-to))
       (into {})))

(def ants-map (overlay-for :ants))

(def results (atom []))
(defn check! [id pass? & msg]
  (swap! results conj [id (boolean pass?)])
  (println (format "  [%s] %s %s" (if pass? "PASS" "FAIL") id (str/join " " msg))))

;; ---- P1: merge-deep is blind to the tau family -----------------------------
(println "--- prediction :ants-tau-invisible-to-merge")
(def tau-paths (filter #(= :tau (ants-map (:path %))) ant-paths))
(def tau-writers (distinct (map :ant tau-paths)))
(def tau-key-collisions
  (->> tau-paths (group-by :path) (filter (fn [[_ v]] (> (count v) 1))) count))
(println "    key paths the overlay maps to :tau, as found in the SOURCE deltas:")
(doseq [{:keys [ant path val]} (sort-by :ant tau-paths)]
  (println (format "      %-28s %-34s = %s" ant (pr-str path) val)))
(println (format "    merge-deep key collisions on those paths : %d" tau-key-collisions))
(println (format "    overlay: distinct patterns writing :tau   : %d  %s"
                 (count tau-writers) (pr-str (vec tau-writers))))
(check! :ants-tau-invisible-to-merge
        (and (zero? tau-key-collisions) (>= (count tau-writers) 3))
        "— merge-deep sees no collision; overlay sees" (count tau-writers) "patterns arguing about one quantity")

;; ---- P2: the info control — merge-deep DOES see this one -------------------
(println "\n--- prediction :info-control (negative control)")
(def info-paths (filter #(= [:efe :lambda :info] (:path %)) ant-paths))
(doseq [{:keys [ant path val]} info-paths]
  (println (format "      %-28s %-34s = %s" ant (pr-str path) val)))
(def info-collides (> (count info-paths) 1))
(def info-distinct-vals (distinct (map :val info-paths)))
(check! :info-control
        (and info-collides (> (count info-distinct-vals) 1))
        "— merge-deep DOES collide here (identical key, values" (pr-str info-distinct-vals)
        ") so the overlay earns no credit for it")

;; ---- P3: transcription fidelity — cascade vs source ------------------------
(println "\n--- prediction :transcription-fidelity (cascade-ants.edn vs the .flexiarg files)")
(def cascade-edn
  (try (edn/read-string (:out (shell {:out :string :dir "/home/joe/code/futon2"}
                                     "git" "show" "origin/M-propagators-ant-gate:holes/cascade-ants.edn")))
       (catch Exception e (println "    (could not read branch:" (.getMessage e) ")") nil)))
(when cascade-edn
  (let [by-pattern (into {} (keep (fn [b] (when-let [p (:pattern b)]
                                            [(last (str/split p #"/")) (:delta b)]))
                                  (:cascade/boxes cascade-edn)))
        cmp (for [{:keys [id delta]} ants
                  :when (contains? by-pattern id)]
              {:id id :same? (= (or delta {}) (or (by-pattern id) {}))
               :src delta :cascade (by-pattern id)})]
    (doseq [{:keys [id same?]} cmp]
      (println (format "      %-28s %s" id (if same? "matches source" "*** DIVERGED ***"))))
    (doseq [{:keys [id same? src cascade]} cmp :when (not same?)]
      (println "        source :" (pr-str src))
      (println "        cascade:" (pr-str cascade)))
    (check! :transcription-fidelity (every? :same? cmp)
            "—" (count (filter :same? cmp)) "of" (count cmp) "transcribed deltas match their source")))

;; ---- P4: the FUTON side — token search cannot see the family ---------------
(println "\n--- prediction :futon-family-invisible")
(def DB "/home/joe/code/futon1bi/state/flexiarg-fts.db")
(if-not (.exists (io/file DB))
  (println "    SKIP — no snapshot at" DB "(run futon1bi/first_checking.clj)")
  ;; via futon1bi's JDBC (xerial) — the same driver the live sidecar uses. The
  ;; sqlite3 CLI is not installed on this box, and reaching for it was a guess.
  (let [q (fn [term]
            (-> (shell {:out :string :dir "/home/joe/code/futon1bi"}
                       "clojure" "-M" "-e"
                       (str "(require '[next.jdbc :as j])(print (:c (j/execute-one! (j/get-datasource {:dbtype \"sqlite\" :dbname \"" DB "\"}) [\"SELECT count(*) c FROM flexiarg_fts WHERE then_slot MATCH ?\" \"" term "\"] {:builder-fn next.jdbc.result-set/as-unqualified-maps})))"))
                :out str/trim parse-long))
        forms (->> (:overlay/records OVERLAY)
                   (filter #(and (= :prose (:scope %)) (= :inhabit (:maps-to %))))
                   (map :surface))
        counts (into {} (map (fn [f] [f (q f)]) forms))
        union (q "inhabit*")]
    (doseq [[f c] counts] (println (format "      THEN MATCH %-14s -> %d" (pr-str f) c)))
    (println (format "      THEN MATCH %-14s -> %d   (the family, via prefix)" "\"inhabit*\"" union))
    (check! :futon-family-invisible
            (and (zero? (get counts "inhabit" -1)) (>= union 8))
            "— no bare token covers the family in THEN; canonical :inhabit would cover" union)))

;; ---------------------------------------------------------------------------
(println "\n=== structure verdict")
(let [rs @results]
  (println (format "  %d/%d predictions held" (count (filter second rs)) (count rs)))
  (doseq [[id ok] rs] (println (format "   %-32s %s" id (if ok "held" "BROKEN — structure is wrong"))))
  (println "\n  The cross-domain claim: the SAME structure catches a family the native")
  (println "  mechanism misses in both domains — morphological in FUTON, key-shaped in")
  (println "  the ants. If either side had failed, overlay-v0 would not deserve the name")
  (println "  substrate. Status stays :structure-only until an operator adopts a mapping.")
  (System/exit (if (every? second rs) 0 1)))
