(ns futon2.aif.outer-menu-wiring-test
  "Unstubbed constructor/selector and retained pending replay. No fixture
  constructor binding; all stores used by migration tests are temporary."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-menu :as menu]
            [futon2.aif.outer-observation :as observation]
            [futon2.aif.outer-menu-test :as pins]
            [futon2.aif.outer-score :as score]
            [futon2.aif.outer-select :as select]
            [futon2.aif.outer-state :as state]))

(defn- makeup []
  (assoc (state/load-makeup "data/wm-learning/outer-makeup.edn")
         :task-selection-library-root pins/library-root))
(def observation
  {:focus "M-large" :relation (fn [_ target] (if (= "M-large" target) :focus :associated))
   :giraffe/pass-sha256 pins/pass-sha :giraffe/input-sha256 pins/input-sha
   :field [{:target "M-first" :reach 3/4} {:target "M-large" :reach 3/4}]
   :facts {"M-first" {:status :ready :cone 0 :unblocks 0 :rank 2}
           "M-large" {:status :ready :cone 3 :unblocks 3 :rank 1}}})
(defn- kind [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:kind (ex-data e)))))

(deftest constructor-drives-selection-not-makeup-menu
  (let [m (makeup) s (state/init-state m)
        r (select/select m s observation)
        poisoned (select/select (assoc m :cascades [{:id :poison :steps [:invalid]}]) s observation)
        construction (menu/construct pins/library-root m observation)]
    (is (= r poisoned))
    (is (= 3 (count (:cascades r)) (get-in r [:construction :policy-count])))
    (is (= construction (-> (:construction r) (update :cascades #(mapv (fn [c] (dissoc c :class)) %)))))
    (is (= :giraffe-order (:cascade-id r)))
    (is (= "M-large" (get-in r [:selection :selected :target])))
    (is (= (mapv :digest (:cascades construction)) (mapv :digest (:cascades r))))
    (is (= (mapv :units (:cascades construction)) (mapv :units (:cascades r))))
    (is (= (mapv :edges (:cascades construction)) (mapv :edges (:cascades r))))
    (is (not-any? #(= :what-others-wait-on-first (:id %)) (:cascades r)))))

(deftest pending-recovers-by-digest-or-retained-steps
  (let [m (makeup) s (state/init-state m)
        r (select/select m s observation)
        ending {:ending-class :focused :observed-gain :several}
        expected (state/next-state m s r observation ending)
        reordered (update r :cascades #(vec (reverse %)))]
    (is (= expected (state/next-state (assoc m :cascades []) s reordered observation ending)))
    (is (= expected (state/next-state m s (dissoc r :cascade-digest) observation ending)))
    (is (= expected (state/next-state m s {:steps [:ready-gate :by-cone :by-unblocks]
                                         :selection (:selection r)} observation ending)))
    (is (= :pending-steps-unrecoverable
           (kind #(state/next-state m s (assoc r :cascade-digest (apply str (repeat 64 "0"))) observation ending))))
    (is (= :pending-steps-unrecoverable
           (kind #(state/next-state m s (dissoc r :cascade-digest :cascades) observation ending))))))

(deftest no-menu-makeup-and-menu-only-migration
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "l54d2-makeup" (make-array java.nio.file.attribute.FileAttribute 0)))
        ;; The live makeup lost its menu on 2026-11-01 (L5-4d-2 migration, merged
        ;; 13:35Z), so the pre-migration shape is rebuilt here from the live kits
        ;; plus a menu, and both shapes are loaded from files for coherent shas.
        live (dissoc (state/load-makeup) :makeup/path :makeup/sha256)
        old-f (io/file dir "makeup-with-menu.edn")
        _ (spit old-f (pr-str (assoc live :cascades [{:id :menu-only :patterns ["only-what-can-start-now"] :steps [:ready-gate]}])))
        old (state/load-makeup old-f)
        f (io/file dir "makeup.edn")
        _ (spit f (pr-str live))
        new (state/load-makeup f)
        s (state/init-state old)
        migrated (:state (state/migrate-makeup-pin s old new))]
    (is (not (contains? new :cascades)))
    (is (= (dissoc s :makeup/sha256) (dissoc migrated :makeup/sha256)))
    (is (= (:makeup/sha256 new) (:makeup/sha256 migrated)))
    (is (= :makeup-migration-not-precision-only
           (kind #(state/migrate-makeup-pin s old (assoc new :omega-c 1/2)))))))

(deftest alias-and-duplicate-controls
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "l54d2-mapping" (make-array java.nio.file.attribute.FileAttribute 0)))
        _ (doseq [f (.listFiles (io/file pins/library-root)) :when (.isFile f)]
            (io/copy f (io/file dir (.getName f))))
        f (io/file dir "executable-steps.edn")
        mapping (edn/read-string (slurp f))]
    (spit f (pr-str (assoc-in mapping [:cascades 0 :status] :executable)))
    (is (= :duplicate-policy (kind #(menu/construct dir (makeup) observation))))
    (spit f (pr-str (assoc-in mapping [:cascades 0 :alias-of] :missing)))
    (is (= :outer-menu/alias-target-invalid (kind #(menu/load-library dir))))))

(deftest status-gate-is-the-recorded-interpreter-step
  (is (= [{:target "x"}]
         (score/apply-selection-step {"x" {:status :undetermined} "y" {:status :ready}}
                                     [{:target "x"} {:target "y"}]
                                     {:status-gate :undetermined}))))

(deftest retained-field-scores-discovery-without-graph-metrics
  (let [text (slurp (io/file pins/pass-root "pass.edn"))
        _ (when-not (= pins/pass-sha (identity/sha256 text))
            (throw (ex-info "Retained pass drift" {})))
        rows (:order (edn/read-string text))
        m (makeup)
        obs {:focus "WM" :relation (fn [_ _] :associated)
             :giraffe/pass-sha256 pins/pass-sha :giraffe/input-sha256 pins/input-sha
             :field (mapv #(hash-map :target (name (:id %)) :reach (:value (observation/task-reach %))) rows)
             :facts (into {} (map #(vector (name (:id %)) %) rows))}
        result (try (select/select m (state/init-state m) obs)
                    (catch clojure.lang.ExceptionInfo e
                      (is false (str "Unexpected refusal " (:kind (ex-data e))))
                      (:selection-evidence (ex-data e))))
        discovery (first (filter #(= :find-out-before-building (:id %)) (:cascades result)))]
    (is (= 2 (get-in result [:construction :policy-count]) (count (:classes result))))
    (is (= 2768528 (:g-units (first (:classes result)))))
    (is (= [:only-what-can-start-now :giraffe-order] (:members (first (:classes result)))))
    (is (= "M-pattern-retrieval-calibration" (get-in result [:selection :selected :target])))
    (is (= :discovery-yields-no-ready (:gain-source discovery)))
    (is (= [:related :none] (get-in discovery [:prediction :landing])))
    (is (= :undetermined (get-in discovery [:prediction :status])))
    (is (number? (:G discovery)))
    (is (= "M-interim-director" (:pick discovery)))))

(deftest verified-graph-pin-and-missing-or-corrupt-evidence
  (is (= pins/input-sha (observation/verified-input-pin pins/pass-root)))
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "l54d3-input" (make-array java.nio.file.attribute.FileAttribute 0)))
        manifest (io/file dir "manifest.edn") input (io/file dir "input.edn")]
    (is (= :giraffe-manifest-missing (:reason (observation/verified-input-pin dir))))
    (spit manifest (pr-str {:files [{:file "input.edn" :sha256 pins/input-sha}]}))
    (is (= :giraffe-input-missing (:reason (observation/verified-input-pin dir))))
    (spit input "corrupt")
    (is (= :giraffe-input-pin-mismatch (kind #(observation/verified-input-pin dir))))))

(deftest discovery-never-reads-numeric-gain-terms
  (let [input {:focus "x" :relation (fn [_ _] :associated)
               :facts {"x" {:status :undetermined :cone :unavailable :unblocks :unavailable}}
               :field [{:target "x" :reach 1/2}] :state-counts (:state-kit (makeup))
               :cascade [{:status-gate :undetermined}]}
        m (score/outer-model input)]
    (is (= [:related :none] (:landing m)))
    (with-redefs [score/kernel-definition (assoc-in score/kernel-definition [:discovery-gain :gain] :one)]
      (is (= [:related :one] (:landing (score/outer-model input)))))
    (is (= :outer-score/graph-term-unavailable
           (kind #(score/outer-model (assoc input :cascade [{:status-gate :undetermined} :by-cone])))))))

(deftest real-giraffe-field-selects-through-the-constructed-menu
  ;; Moved from outer-select-test on 2026-11-01 (claude-2, L5-4j review): that
  ;; namespace stubs the constructor from the makeup's :cascades, which the live
  ;; makeup no longer carries (L5-4d-2), so the test read :empty-cascade-menu
  ;; there and had pinned the pre-L5-4d-3 refusal. Here the production
  ;; constructor runs over the real 11-01 pass with constant associated
  ;; relations and the default reach, and the pin is what the restored chain
  ;; decides on that field.
  (let [text (slurp "/home/joe/code/storage/giraffe/20261101T044121.422Z-730407c5/pass.edn")
        expected "a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91"]
    (is (= expected (identity/sha256 text)))
    (when (= expected (identity/sha256 text))
      (let [rows (:order (edn/read-string text))
            makeup (state/load-makeup "data/wm-learning/outer-makeup.edn")
            st (state/init-state makeup)
            obs {:field (mapv #(hash-map :target (name (:id %)) :reach (:default-reach makeup)
                                         :reach-source :default-reach) rows)
                 :facts (into {} (map #(vector (name (:id %)) %) rows))
                 :focus "WM" :relation (fn [_ _] :associated)}
            r (select/select makeup st obs)
            chosen (first (filter #(= (:chosen-class r) (:id %)) (:classes r)))]
        (is (nil? (:kind r)) "the real field decides; it does not refuse")
        (is (= 609 (count (:outer-score/admitted r))))
        (is (= 2 (count (:classes r))) "two prediction classes: the two ready-gated arrangements, and discovery")
        (is (= [:only-what-can-start-now :giraffe-order] (:members chosen)))
        (is (= "M-pattern-retrieval-calibration" (get-in r [:selection :selected :target])))
        (is (< (:G chosen) (get-in r [:abstention :G])) "the chosen class is strictly below stopping")))))
