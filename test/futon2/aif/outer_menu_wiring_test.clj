(ns futon2.aif.outer-menu-wiring-test
  "Unstubbed constructor/selector and retained pending replay. No fixture
  constructor binding; all stores used by migration tests are temporary."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.outer-menu :as menu]
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
    (is (= construction (:construction r)))
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
        old (state/load-makeup)
        f (io/file dir "makeup.edn")
        _ (spit f (pr-str (dissoc old :cascades :makeup/path :makeup/sha256)))
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

(deftest retained-field-names-the-remaining-discovery-gain-blocker
  (let [text (slurp (io/file pins/pass-root "pass.edn"))
        _ (when-not (= pins/pass-sha (identity/sha256 text))
            (throw (ex-info "Retained pass drift" {})))
        rows (:order (edn/read-string text))
        m (makeup)
        obs {:focus "WM" :relation (fn [_ _] :associated)
             :giraffe/pass-sha256 pins/pass-sha :giraffe/input-sha256 pins/input-sha
             :field (mapv #(hash-map :target (name (:id %)) :reach 1/2) rows)
             :facts (into {} (map #(vector (name (:id %)) %) rows))}
        refusal (try (select/select m (state/init-state m) obs)
                     (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :outer-score/graph-term-unavailable (:kind refusal)))
    (is (= :unblocks (:term refusal)))
    (is (= 3 (get-in refusal [:construction :policy-count])))))
