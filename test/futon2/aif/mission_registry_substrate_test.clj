(ns futon2.aif.mission-registry-substrate-test
  "2026-09-17 half 2: the zero-arg registry read points at substrate-2 and
   refuses typed rather than falling back to the filesystem."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.mission-registry :as mr]
            [futon2.aif.substrate :as substrate]))

(defn- entity
  [id props]
  {:entity/external-id id
   :entity/name (str "mission|" id)
   :entity/type :mission
   :entity/props props})

(defn- with-substrate-entities
  [entities f]
  (with-redefs [substrate/entities-by-type (fn [& _] entities)]
    (f)))

(deftest zero-arg-load-reads-substrate-test
  (with-substrate-entities
    [(entity "M-alpha"
             {:mission/status-class "active"
              :mission/title "Alpha"
              :mission/status-line "ACTIVE"
              :mission/open-hole-count 2
              :provenance/repo "futon3"
              :provenance/path "/x/y/M-alpha.md"
              :provenance/sha256 "abc"})]
    (fn []
      (let [entries (:missions (mr/load-missions))]
        (is (= 1 (count entries)))
        (let [m (first entries)]
          (is (= "M-alpha" (:id m)))
          (is (= :active (:status-class m)))
          (is (= "Alpha" (:title m)))
          (is (= "/x/y/M-alpha.md" (:path m)))
          (is (= 2 (:open-hole-count m)))
          (is (mr/live-mission? m)))))))

(deftest missing-status-class-is-unknown-not-defaulted-test
  (with-substrate-entities
    [(entity "M-hinge" {})]
    #(is (= :unknown (:status-class (first (:missions (mr/load-missions))))))))

(deftest unreachable-substrate-is-typed-refusal-test
  (with-redefs [substrate/entities-by-type
                (fn [& _] (throw (ex-info "conn" {})))]
    (try (mr/load-missions)
         (is false "must throw")
         (catch clojure.lang.ExceptionInfo e
           (is (= :substrate-unreachable (:kind (ex-data e))))))))

(deftest empty-substrate-is-typed-refusal-test
  (with-redefs [substrate/entities-by-type (fn [& _] [])]
    (try (mr/load-missions)
         (is false "must throw")
         (catch clojure.lang.ExceptionInfo e
           (is (= :substrate-mission-registry-empty (:kind (ex-data e))))))))

(deftest one-arg-load-is-still-the-explicit-file-scan-test
  (let [calls (atom [])]
    (with-redefs [mr/load-missions-from-files
                  (fn [root] (swap! calls conj root) {:missions [:fake-files]})]
      (let [doc (mr/load-missions "/tmp/never-scanned-here")]
        (is (= [:fake-files] (:missions doc)))
        (is (= ["/tmp/never-scanned-here"] @calls))))))
