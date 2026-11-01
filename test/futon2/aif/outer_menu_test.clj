(ns futon2.aif.outer-menu-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.outer-menu :as menu]
            [futon2.aif.outer-score :as score]
            [futon2.aif.outer-observation :as observation]
            [futon2.aif.outer-state :as state])
  (:import [java.nio.file Files]))

(def library-root
  (or (System/getenv "L54D_LIBRARY_ROOT")
      "/home/joe/worktrees/futon3-l5-4d/library/task-selection"))
(def pass-root "/home/joe/code/storage/giraffe/20261101T044121.422Z-730407c5")
(def pass-sha "a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91")
(def input-sha "730407c514e2fd21905da2b533d92e046a454372c919fbd89089fc953ebf1667")
(defn- read-bytes [f] (Files/readAllBytes (.toPath (io/file f))))
(defn- makeup [] (state/load-makeup))
(defn- construct []
  (menu/construct library-root (makeup) {:giraffe/pass-sha256 pass-sha
                                       :giraffe/input-sha256 input-sha}))
(defn- refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:kind (ex-data e)))))
(defn- temp-library []
  (let [dir (.toFile (Files/createTempDirectory "l54d-library" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (doseq [f (.listFiles (io/file library-root)) :when (.isFile f)]
      (io/copy f (io/file dir (.getName f))))
    dir))
(defn- alter-mapping! [dir f]
  (let [path (io/file dir "executable-steps.edn")]
    (spit path (pr-str (f (edn/read-string (slurp path)))))))

(deftest real-library-partition-and-witness-digest
  (is (= pass-sha (identity/sha256 (read-bytes (io/file pass-root "pass.edn")))))
  (is (= input-sha (identity/sha256 (read-bytes (io/file pass-root "input.edn")))))
  (let [c (construct) lib (:library c) retrieval (:retrieval c)
        witness (edn/read-string (slurp "holes/labs/wm-contract/witnesses/outer-selection-conformant-2026-11-01.edn"))
        w (first (:cascades witness))
        giraffe (first (filter #(= :giraffe-order (:id %)) (:cascades c)))
        all (concat (:included retrieval) (map :pattern (:excluded retrieval)))]
    (is (= 4 (:policy-count c)))
    (is (= 19 (:count lib) (count all) (count (set all))))
    (is (= (set (keys (:patterns lib))) (set all)))
    (is (= (:slice c) (:pool c) (:included retrieval)))
    (is (every? :reason (:excluded retrieval)))
    (doseq [cascade (:cascades c) u (:units cascade)]
      (is (= (:sha256 u)
             (identity/sha256 (read-bytes (io/file library-root (.getName (io/file (:pattern u)))))))))
    (is (= (:digest giraffe) (menu/cascade-digest w)))
    (is (= (:units w) (:units giraffe)))
    (is (= (:edges w) (:edges giraffe)))
    (is (= pass-sha (:reading c)))
    (is (= input-sha (:graph c)))
    (is (= (:kernel c) (identity/sha256 (.getBytes (pr-str (score/kernel-parameters (makeup))) "UTF-8"))))
    (is (= c (construct)))))

(deftest mapping-must-bind-readme-and-safe-executable-steps
  (doseq [[alter reason]
          [[#(assoc % :readme-sha256 (apply str (repeat 64 "0"))) :outer-menu/mapping-invalid]
           [#(assoc-in % [:cascades 0 :steps] [{:status-gate :undetermined} :by-cone])
            :outer-menu/unavailable-metric-order]
           [#(assoc-in % [:cascades 0 :steps] [:by-unblocks :ready-gate])
            :outer-menu/unavailable-metric-order]
           [#(assoc-in % [:cascades 0 :patterns] ["not-in-library"])
            :outer-menu/pattern-outside-manifest]
           [#(assoc-in % [:cascades 0 :steps] [{:gate "not-code"}]) :outer-menu/invalid-steps]]]
    (let [dir (temp-library)]
      (alter-mapping! dir alter)
      (is (= reason (refusal #(menu/load-library dir)))))))

(deftest composite-pin-binds-mapping-and-readme
  (let [dir (temp-library)
        before (:library (menu/load-library dir))]
    (alter-mapping! dir #(assoc % :version 2))
    (let [after (:library (menu/load-library dir))]
      (is (= (:manifest-sha before) (:manifest-sha after)))
      (is (not= (:sha256 before) (:sha256 after)))
      (is (not= (:mapping-sha before) (:mapping-sha after))))))

(deftest missing-pins-remain-absence
  (let [c (menu/construct library-root {} {})]
    (is (= {:status :absent :reason :giraffe-input-pin-unavailable} (:graph c)))
    (is (= {:status :absent :reason :giraffe-pass-pin-unavailable} (:reading c)))
    (is (= {:status :absent :reason :kernel-parameters-unretained} (:kernel c)))))

(deftest kernel-carrier-describes-consumed-constants
  (let [m (makeup)
        model (score/outer-model {:focus "x" :relation (fn [_ _] :focus)
                                 :facts {"x" {:status :ready :unblocks 1}}
                                 :field [{:target "x" :reach 1/2}]
                                 :state-counts (:state-kit m) :cascade [:ready-gate]})
        input {:ending-counts (:c (state/init-state m)) :gain-counts (:gain-kit m)}
        g (:G (score/score-model input model))
        carrier (score/kernel-parameters m)]
    (with-redefs [score/kernel-definition (assoc score/kernel-definition :novelty-coefficient 0.0)]
      (is (not= carrier (score/kernel-parameters m)))
      (is (not= g (:G (score/score-model input model)))))
    (let [changed (assoc-in m [:gain-kit :one] 20)]
      (is (not= (score/kernel-parameters m) (score/kernel-parameters changed)))
      (is (not= g (:G (score/score-model (assoc input :gain-counts (:gain-kit changed)) model)))))
    (with-redefs [score/kernel-definition (assoc-in score/kernel-definition [:readiness-reach-prior :ready] 2/3)]
      (is (= 2/3 (:value (observation/task-reach {:status :ready :dependency-reading {:record-pin {:sha256 pass-sha}}})))))))
