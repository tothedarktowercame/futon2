(ns futon2.aif.meta-loop-node-injury-acceptance-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.full-loop-runner :as flr]
            [futon2.aif.wm.loop-node-exercise :as lne]
            [futon2.aif.wm.terminal-receipt :as tr]))

(def ^:const injury-id "injury/d755c886a22d6fc8c04db7fada64089e07157f62")

(def ^:const record-path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-19-f9abc990-b43a-491b-8f2b-d8b14c7fa796.edn")

(def ^:const record-sha256 "f6634b0452b8c69e7051041b58b1e936a2b176311058e8921c86fd753867dd3f")

(defn- sha256 [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes (slurp path) "UTF-8"))
    (apply str (map #(format "%02x" %) (.digest digest)))))

(defn- injured-record []
  (edn/read-string (slurp record-path)))

(def repaired-selection
  {:status "selected"
   :target "M-aif-policy-conditioned-eig"
   :candidates
   [{:id "cand-head"
     :class-emission {:status :computed :consumed-in-g? true :risk 0.5978370007556204 :ambiguity 0.0
                      :model {:kind :class-emission :horizon 2 :class-preference {2 {:focused 1.0}}}}}
    {:id "cand-rank2"
     :class-emission {:status :computed :consumed-in-g? true :risk 0.4021629992443796 :ambiguity 0.0
                      :model {:kind :class-emission :horizon 2 :class-preference {2 {:focused 0.5 :diffuse 0.5}}}}}]
   :policy-precision-state {:probabilities {"cand-head" 0.75 "cand-rank2" 0.25} :temperature 2.0}
   :policy-comparison {:status :computed :precedence [[:policy-a :policy-b] [:policy-b :policy-c]] :horizons #{2}}
   :selection-law {:posterior {"cand-head" 0.75 "cand-rank2" 0.25} :beta {:alpha 3.0 :beta 1.0}}})

(deftest t1-injury-pinned-in-record
  (testing "the pinned run record is byte-identical and shows the injury"
    (is (= record-sha256 (sha256 record-path)))
    (let [text (slurp record-path)]
      (is (str/includes? text ":required-loop-node-unexercised"))
      (is (str/includes? text "65615")))))

(deftest t2-empty-selection-is-refused-not-exercised
  (testing "a selecting decision with no retained evidence measures four refused nodes"
    (let [decision {:status "selected" :selection-certificate {} :selection-law {}}
          r (lne/receipt decision)]
      (is (= :incomplete (:status r)))
      (is (= {:present 0 :bypassed 0 :refused 4} (:counts r))))
    (let [annotated (lne/require-complete! {:status "selected" :selection-certificate {} :selection-law {}})]
      (is (map? annotated))
      (is (= :incomplete (:status (lne/receipt annotated)))))))

(deftest t3-repaired-retention-passes-exercise
  (testing "strategic-selection! with retained evidence yields a complete exercise receipt"
    (let [result (flr/strategic-selection!
                  {:strategic-selection-invoke-fn (fn [_request] {:ok true :selection repaired-selection})
                   :strategic-selection-sleep-fn (fn [_ms] nil)}
                  {:kind :strategic-selection})
          decision (or (:selection result) result)
          r (lne/receipt decision)]
      (is (map? decision))
      (is (= :complete (:status r)))
      (is (= 4 (:present (:counts r))))
      (is (= 0 (:refused (:counts r))))
      (is (= :complete (:status (lne/receipt (lne/require-complete! decision))))))))

(deftest t4-terminal-routing-follows-selection-state
  (testing "the injured record terminal-receipts as the typed close failure"
    (let [r (tr/terminal-receipt (injured-record))]
      (is (= :failure (:kind r)))
      (is (= :invalid-close-outcome (:failure-kind r)))))
  (testing "a repaired record with a real selection terminal-receipts as an action"
    (let [r (tr/terminal-receipt {:selection-event {:event :cascade-selected :target "M-aif-policy-conditioned-eig"}})]
      (is (= :action-receipt (:kind r))))))
