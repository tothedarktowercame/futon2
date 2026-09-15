(require '[futon2.aif.repair-obligation :as repair]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pp])
(import '[java.io PushbackReader FileReader])
(def T-id "repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-002-artifact-binding-mismatch")
(def obligation
  (let [finding (with-open [r (PushbackReader. (FileReader. (str "data/wm-repair-obligations/findings/" T-id ".edn")))]
                  (edn/read r))
        impl (with-open [r (PushbackReader. (FileReader. (str "data/wm-repair-obligations/implementations/" T-id ".edn")))]
               (edn/read r))]
    (assoc finding :repair/implementation impl)))
(def result
  (repair/successor-resolution!
   {:obligation obligation
    :repair-close
    {:attempt/id "ea1-6ecd77fa689d4c5adb1e1444d7eba993fc5fb7bb60501b873f0d469c7f745317--attempt-002"
     :run/id "d7eabcf7-b992-435a-ae11-4ef452eee97a"
     :repair/id T-id
     :closed-at "2026-09-14T22:18:57.401728865Z"
     :commit "fba72d0f0683caee55be599874d6550bf2f77244"
     :review-receipt-ids ["invoke-1789424003193-20878-1f0ca626"]
     :review-sha256 "a4ed92e35de1d8e6875a7a6a668536e3401a7c94d2365312f9928b2a4dc7c6ff"
     :grounded? true}
    :successor-close
    {:attempt/id "ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-002"
     :run/id "8872d5ec-5604-4c2a-b393-4c690613f8e6"
     :repair/id T-id
     :closed-at "2026-09-15T01:53:56.367410955Z"
     :witness-ref "data/wm-full-loop-machinery-55/wm-contract-machinery-55-v1/attempt-002/006-adjudication.edn"
     :witness-sha256 "7a67bbf0211d9f21d076c016cc3bfd2abb5aa7a9dd082ae4efffc77096a18ba7"
     :grounded? true
     :production-shaped? true
     :witness {:resolved? true :dial-moved? true}}
    :authority {:decided-by "codex-24"
                :review-job "invoke-1789424003193-20878-1f0ca626"}
    :resolution-read-fn (fn [rid]
                          (let [f (clojure.java.io/file "data/wm-repair-obligations/resolutions" (str rid ".edn"))]
                            (when (.isFile f)
                              (with-open [r (PushbackReader. (FileReader. f))] (edn/read r)))))
    :resolve-fn repair/resolve!}))
(println :status (:status result))
(pp/pprint (select-keys (:relation result) [:schema :repair/id :repair-commit :successor-attempt/id :decided-by]))
