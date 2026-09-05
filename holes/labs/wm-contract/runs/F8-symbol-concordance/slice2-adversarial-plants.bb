#!/usr/bin/env bb
;; F8 leg 2 slice 2 -- the REVIEWING seat's ADVERSARIAL PLANT SUITE against the
;; delivered checker. Written BEFORE the delivery was read, so the plants are
;; not shaped by the implementation they test.
;;
;; It is not the negative-controls section (that is the delivery's, in
;; p4ng/empirics-futon/negative_controls.sh). This is the second reader: it
;; plants the twelve cases the packet named AND eight the packet did NOT name,
;; because a checker that passes exactly its own commissioned controls has been
;; tested by its author only.
;;
;; Every plant is on a TEMP COPY. The committed registry is never written.
;; Usage: bb slice2-adversarial-plants.bb [checker.bb] [registry.edn]
(require '[clojure.edn :as edn] '[clojure.string :as str] '[babashka.process :as p])

(def lab (str (System/getenv "HOME") "/code/futon2/holes/labs/wm-contract"))
(def checker  (or (first *command-line-args*)  (str lab "/symbol_concordance_check.bb")))
(def reg-path (or (second *command-line-args*) (str lab "/symbol-concordance.edn")))
(def reg (edn/read-string {:default (fn [_ v] v)} (slurp reg-path)))
(def tmp (str (System/getProperty "java.io.tmpdir") "/f8s2-plants"))
(.mkdirs (java.io.File. tmp))

(defn run-on [m nm]
  (let [f (str tmp "/" nm ".edn")]
    (spit f (pr-str m))
    (let [r (p/sh "bb" checker f)]
      {:exit (:exit r) :out (str (:out r) (:err r))})))

(def results (atom []))
(defn expect! [nm expectation m & musts]
  (let [{:keys [exit out]} (run-on m nm)
        refused? (not= 0 exit)
        ok-dir (if (= expectation :refuse) refused? (not refused?))
        missing (vec (remove #(str/includes? out %) musts))
        ok (and ok-dir (empty? missing))]
    (swap! results conj {:plant nm :expect expectation :exit exit :ok ok
                         :missing missing :out (str/trim out)})))

;; --- helpers -----------------------------------------------------------
(defn drop-collision [m fold] (update m :collisions (fn [cs] (filterv #(not= fold (:fold %)) cs))))
(defn upd-collision [m fold f] (update m :collisions (fn [cs] (mapv #(if (= fold (:fold %)) (f %) %) cs))))
(defn upd-row [m id f] (update m :symbols (fn [rs] (mapv #(if (= id (:id %)) (f %) %) rs))))
(defn row-of [m id] (first (filter #(= id (:id %)) (:symbols m))))

;; === THE TWELVE THE PACKET NAMED ======================================
;; 1. THE ONE THE RULING NAMES BY HAND: the capital-Pi three-way case.
(expect! "01-pi-collision-deleted" :refuse (drop-collision reg "pi")
         ":precision/evidence-channel" ":policy/set" ":policy/space-capital-pi")
;; 2. a NEW row reusing a bare symbol -- the case the gate exists for on future edits.
(expect! "02-new-row-reusing-mu" :refuse
         (update reg :symbols conj {:id :planted/second-mu :bare "mu" :fold "mu"
                                    :reading "planted" :node :R3 :registry-symbol nil
                                    :glossary {:absent :planted} :lean {:absent :planted}
                                    :runtime {:absent :planted}})
         ":planted/second-mu")
;; 3. a declared member set narrower than the recomputed group.
(expect! "03-pi-members-cut-to-two" :refuse
         (upd-collision reg "pi" #(assoc % :members [:precision/evidence-channel :policy/set]))
         ":policy/space-capital-pi")
;; 4. a member id no row carries.
(expect! "04-member-id-nonexistent" :refuse
         (upd-collision reg "pi" #(update % :members conj :no/such-row) ) ":no/such-row")
;; 5. RECOMPUTED, NOT TRUSTED. Without this the whole checker could be a no-op.
(expect! "05-fold-field-drift" :refuse
         (upd-row reg :precision/evidence-channel #(assoc % :fold "NOT-THE-FOLD")))
;; 6. a column pointer past the end of a real file.
(expect! "06-column-at-past-eof" :refuse
         (upd-row reg :observation/vector #(assoc-in % [:runtime :at] "103-999999")))
;; 7. a column that is neither a pointer nor a typed absence.
(expect! "07-untyped-column" :refuse
         (upd-row reg :observation/vector #(assoc % :lean {:probably "somewhere"})))
;; 8. no population.
(expect! "08-symbols-emptied" :refuse (assoc reg :symbols []))
;; 9. a recorded collision with no pointers is a memory, not a check.
(expect! "09-collision-without-basis" :refuse (upd-collision reg "pi" #(dissoc % :basis)))
;; 10. wrong schema.
(expect! "10-foreign-schema" :refuse (assoc reg :schema :wm/something-else))
;; 11. ACCEPTANCE: member ORDER is not a ruling about precedence.
(expect! "11-pi-members-reversed" :accept
         (upd-collision reg "pi" #(update % :members (comp vec reverse))))
;; 12. ACCEPTANCE: the registry as committed.
(expect! "12-unmodified" :accept reg)

;; === THE EIGHT THE PACKET DID NOT NAME ================================
;; 13. a row with no :bare at all. A checker whose grouping key can be nil must
;;     refuse, not crash and not fold every such row together silently.
(expect! "13-row-with-no-bare" :refuse (upd-row reg :observation/vector #(dissoc % :bare)))
;; 14. a row with no :fold. The field is a convenience; its ABSENCE must not be
;;     the difference between checked and unchecked.
(expect! "14-row-with-no-fold" :refuse (upd-row reg :observation/vector #(dissoc % :fold)))
;; 15. two rows sharing an :id -- every :members lookup becomes ambiguous.
(expect! "15-duplicate-row-id" :refuse
         (update reg :symbols conj (assoc (row-of reg :policy/depth) :bare "zzz-unique" :fold "zzz-unique")))
;; 16. an INVERTED column range. A range check that only tests the end against
;;     the file length accepts 200-100 on a 900-line file.
(expect! "16-inverted-column-range" :refuse
         (upd-row reg :observation/vector #(assoc-in % [:runtime :at] "146-103")))
;; 17. a :collisions entry for a fold that has ONE member. A collision declared
;;     where none exists is a false record in the opposite direction, and no
;;     control the packet named points that way.
(expect! "17-collision-declared-for-singleton" :refuse
         (update reg :collisions conj {:fold "o" :members [:observation/vector] :kind :within-registry
                                       :statement "planted" :basis ["aif-equations.edn:1"]}))
;; 18. ACCEPTANCE: the relation is genuinely a FOLD. Re-spelling Pi as PI must
;;     change nothing -- if it does, the grouping is not case-insensitive.
(expect! "18-bare-respelled-uppercase" :accept
         (upd-row reg :precision/evidence-channel #(assoc % :bare "PI" :fold "pi")))
;; 19. a fold group that SPLITS. Renaming one member out of the pi group leaves
;;     a declared 3-member entry over a 2-member group -- the mismatch in the
;;     direction 03 does not test (the registry shrank, not the declaration).
(expect! "19-fold-group-split" :refuse
         (upd-row reg :policy/space-capital-pi #(assoc % :bare "Xi" :fold "xi")))
;; 20. an AMBIGUOUS pointer. futon2 carries two efe.clj (ants 21 lines, futon2
;;     981); a resolver that takes the first match answers about the wrong file
;;     and reports green.
(expect! "20-ambiguous-bare-path" :refuse
         (upd-row reg :score/risk #(assoc-in % [:runtime :file] "efe.clj")))

;; --- report ------------------------------------------------------------
(println "F8 leg 2 slice 2 -- adversarial plants against" checker)
(println "registry:" reg-path)
(println)
(doseq [{:keys [plant expect exit ok missing out]} @results]
  (printf "%-38s expect %-7s exit %-3d %s%s%n" plant (name expect) exit
          (if ok "OK" "*** FINDING ***")
          (if (seq missing) (str " (output never names " (str/join ", " missing) ")") ""))
  (when-not ok (println "      " (str/replace (str/join " / " (take 6 (str/split-lines out))) #"\s+" " "))))
(println)
(let [bad (count (remove :ok @results))]
  (printf "%d plants, %d findings%n" (count @results) bad)
  (System/exit 0))
