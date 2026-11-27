(ns futon2.aif.outer-menu-fixture
  "Small-menu scoring fixtures only. Production library construction is tested
  without this redefinition in outer-menu-test and outer-menu-wiring-test.
  E4: selections through this fixture read R1 from the bound
  outer-select/*r1-of*. Joe 2026-11-27 (admission by benefit path): the
  fixture's picks are LINKED, so they remain choosable; tests that exercise
  unlinked picks bind their own r1-of."
  (:require [futon2.aif.outer-menu :as menu]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.q2-construction :as q2]
            [futon2.aif.selection-canonical-g :as selection-g]))

(defn with-fixture-constructor [f]
  (binding [outer-select/*r1-of* (constantly {:status :linked :below 1
                                              :benefits ["B-fixture"]
                                              :log-hash "fixture"
                                              :calibration #{:placeholder}})]
    (with-redefs [menu/construct
                  (fn [root makeup _]
                    (let [library (q2/library-manifest root)]
                      (cond-> {:library library
                       :cascades
                       (mapv (fn [c]
                               (let [units (mapv (fn [i p]
                                                   {:pattern (str "futon3/library/task-selection/" p ".flexiarg")
                                                    :sha256 (get-in library [:patterns p]) :index i})
                                                 (range) (:patterns c))
                                     c (assoc c :units units :edges nil :readme-cascade nil
                                                :lineage-status :absent)]
                                 (assoc c :digest (menu/cascade-digest c))))
                             (:cascades makeup))}
                        (:enactment-modes makeup)
                        (assoc :enactment-modes (:enactment-modes makeup)))))]
      ;; The fixture library's synthetic pattern "test" needs an interpretation for
      ;; the canonical selection G (selection-canonical-g): read it like
      ;; only-what-can-start-now, so a ready pick can begin.
      (binding [selection-g/*extra-interpretations*
                {"test" {:consumes #{:ready} :forbids #{} :produces #{:can-begin}}}]
        (f)))))
