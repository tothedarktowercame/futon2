(ns futon2.aif.makeup-pin-migration-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [futon2.aif.outer-state :as state]
            [futon2.aif.full-loop-runtime :as runtime]))

(defn fixture []
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory "makeup-migration" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (doseq [f ["old-makeup.edn" "new-makeup.edn" "state.edn"]]
      (io/copy (io/file (io/resource (str "fixtures/makeup-pin-migration/" f))) (io/file dir f)))
    (let [old (state/load-makeup (io/file dir "old-makeup.edn"))
          new (state/load-makeup (io/file dir "new-makeup.edn"))
          path (io/file dir "state.edn")]
      {:dir dir :old old :new new :path path :before (state/read-state old path)})))

(deftest live-state-roundtrip-and-pending-consumption
  (let [{:keys [old new path before dir]} (fixture)
        original (slurp path)
        migration (state/migrate-makeup-pin before old new)
        written (state/write-makeup-migration! path migration)
        after (state/read-state new path)
        target (get-in before [:pending :selection :selection :selected :target])
        observation {:focus "WM" :relation (fn [_ _] :associated)
                     :field [{:target target :reach 1/2}]
                     :facts {target {:status :ready :unblocks 1 :cone 1}}}
        learned (with-redefs-fn
                  {#'runtime/pending-run-ending (fn [_ _] {:status :ready :ending-class :related})
                   #'runtime/previous-giraffe-observation (fn [& _] observation)}
                  #(#'runtime/resolve-pending-learning {} new after dir observation))]
    (is (= :migrated (:status written)))
    (is (= original (slurp (:backup written))))
    (is (= (dissoc before :makeup/sha256) (dissoc after :makeup/sha256)))
    (doseq [k [:c :state-counts :gain-counts :click-count :pending]]
      (is (= (pr-str (get before k)) (pr-str (get after k))) (str k)))
    (is (= (:makeup/sha256 old) (get-in after [:pending :selection :makeup/sha256])))
    (is (= :applied (:status learned)))
    (is (nil? (get-in learned [:state :pending])))
    (is (= (inc (:click-count before)) (get-in learned [:state :click-count])))
    (is (not= (:c before) (get-in learned [:state :c])))
    ;; The original immutable selection receipt has not been relabelled.
    (is (= (get-in before [:pending :selection]) (get-in after [:pending :selection])))
    ;; A pin change alone cannot authorize learning of an old-pinned receipt.
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Outer state refused"
          (state/next-state new (with-meta after nil) (get-in after [:pending :selection])
                            observation {:ending-class :related :observed-gain :none})))
    (is (thrown? clojure.lang.ExceptionInfo
          (state/write-makeup-migration! path migration)))))

(deftest changed-learning-parameters-refuse
  (let [{:keys [old new before]} (fixture)]
    (doseq [key [:eta-c :omega-c :eta-gain :omega-gain :eta-state :omega-state]]
      (is (= :makeup-migration-not-precision-only
             (:kind (ex-data (try
                               (state/migrate-makeup-pin before old (assoc new key 1/2))
                               (catch clojure.lang.ExceptionInfo e e)))))))
    (is (thrown? clojure.lang.ExceptionInfo
          (state/migrate-makeup-pin before new new)))))

(deftest altered-receipt-refuses-and-old-pin-alone-is-insufficient
  (let [{:keys [old new path before]} (fixture)
        result (state/write-makeup-migration! path (state/migrate-makeup-pin before old new))
        receipt-path (:receipt-path result)
        receipt (edn/read-string (slurp receipt-path))]
    (spit receipt-path (pr-str (assoc receipt :learning-parameters-equal false)))
    (is (= :invalid-migration-receipt
           (:kind (ex-data (try (state/read-state new path)
                                (catch clojure.lang.ExceptionInfo e e))))))
    (is (= (:makeup/sha256 old) (get-in before [:pending :selection :makeup/sha256])))))
