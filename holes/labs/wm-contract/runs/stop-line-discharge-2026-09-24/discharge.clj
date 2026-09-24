;; Discharge of the repaired-and-never-discharged stop-lines, 2026-09-24 (kimi-3,
;; belled by claude-5). No clicks; findings untouched; two authorized writers only:
;; repair/successor-resolution! (machinery runs, run/ids available — the
;; t-discharge-2026-09-15 route) and repair/resolve! (July runs, no run/id was ever
;; recorded — the codex-18 route used for repair-attempt-029/030).
;; Every R and S below was verified read-only against the retained run records and
;; the live substrate (entity read-back: props implementation/commit == commit).
(require '[futon2.aif.repair-obligation :as repair]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp])
(import '[java.io PushbackReader FileReader])

(def root "data/wm-repair-obligations")

(defn- read-one [p]
  (with-open [r (PushbackReader. (FileReader. p))] (edn/read r)))

(defn- obligation [id]
  (assoc (read-one (str root "/findings/" id ".edn"))
         :repair/implementation
         (read-one (str root "/implementations/" id ".edn"))))

(defn- resolution-read [rid]
  (let [f (io/file root "resolutions" (str rid ".edn"))]
    (when (.isFile f) (read-one (.getPath f)))))

(def closed-machine-failures-before
  (let [open (repair/open-obligations)]
    (println "== board before ==")
    (pp/pprint {:open-total (count open)
                :open-machine-failure (count (filter #(= :machine-failure (:repair/class %)) open))})))

;; ---------- machinery findings: successor-resolution! ----------

(def machinery-discharges
  [;; 7093b8fb--001: R = machinery-50 attempt-002 (closed 17:16), S = machinery-53 attempt-002
   {:id "repair-ea1-7093b8fbb1ca8fc99a18899b69ca39f1e5a1a129e76739ebddbbee974299f94f--attempt-001-untyped-failure"
    :repair-close {:attempt/id "ea1-d991b6a0b13d462645a89c385ca3465f9fdf5bf8bbec27618a4bee6f8dea0a6e--attempt-002"
                   :run/id "9106f5e3-e471-4be5-b52e-14f3870f6c5c"
                   :closed-at "2026-09-14T17:16:17.397664472Z"
                   :commit "642ab3eb77179117a2f2916f68b23cafd7920ade"
                   :review-receipt-ids ["invoke-1789405980746-20803-a81ce2de"]
                   :review-sha256 "329a79172f01c930f49261325e183bddd0b632bfaf96d962600634a673b914de"
                   :grounded? true}
    :successor-close {:attempt/id "ea1-6ecd77fa689d4c5adb1e1444d7eba993fc5fb7bb60501b873f0d469c7f745317--attempt-002"
                      :run/id "d7eabcf7-b992-435a-ae11-4ef452eee97a"
                      :closed-at "2026-09-14T22:18:57.401728865Z"
                      :witness-ref "data/wm-full-loop-machinery-53/wm-contract-machinery-53-v1/attempt-002/006-adjudication.edn"
                      :witness-sha256 "36b92116eeff35709d5042a214c12375a6967f2ae623f749aac7139f91d0d900"
                      :grounded? true :production-shaped? true
                      :witness {:resolved? true :dial-moved? true}}
    :authority {:decided-by "codex-24" :review-job "invoke-1789405980746-20803-a81ce2de"}}
   ;; 3f4cac24--001-untyped: R = machinery-51 attempt-001, S = machinery-53 attempt-002
   {:id "repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-001-untyped-failure"
    :repair-close {:attempt/id "ea1-8ae440e934f3fa2bbce6623211f6f4a2b34a85fe0ec93661da8304799ccdddc8--attempt-001"
                   :run/id "3d9807e0-8292-4691-81d4-c96df6563471"
                   :closed-at "2026-09-14T18:21:44.224375675Z"
                   :commit "5e7708da36dd5f157e97e8da7835bb080c91f650"
                   :review-receipt-ids ["invoke-1789410019408-20809-8cc48df8"]
                   :review-sha256 "4ff35e57ec9444714b7d452e15ac6908dd34d6b92ec7979b4b8ca7db1241cbc1"
                   :grounded? true}
    :successor-close {:attempt/id "ea1-6ecd77fa689d4c5adb1e1444d7eba993fc5fb7bb60501b873f0d469c7f745317--attempt-002"
                      :run/id "d7eabcf7-b992-435a-ae11-4ef452eee97a"
                      :closed-at "2026-09-14T22:18:57.401728865Z"
                      :witness-ref "data/wm-full-loop-machinery-53/wm-contract-machinery-53-v1/attempt-002/006-adjudication.edn"
                      :witness-sha256 "36b92116eeff35709d5042a214c12375a6967f2ae623f749aac7139f91d0d900"
                      :grounded? true :production-shaped? true
                      :witness {:resolved? true :dial-moved? true}}
    :authority {:decided-by "codex-24" :review-job "invoke-1789410019408-20809-8cc48df8"}}
   ;; a9177cab: R = machinery-51 attempt-002, S = machinery-53 attempt-002
   {:id "repair-initialization-a9177cab-e783-464e-83e2-21a6371484a4-initialization-failed"
    :repair-close {:attempt/id "ea1-8ae440e934f3fa2bbce6623211f6f4a2b34a85fe0ec93661da8304799ccdddc8--attempt-002"
                   :run/id "9d3bcd46-7105-41a5-baf9-c33cfa08dd36"
                   :closed-at "2026-09-14T19:10:01.624722757Z"
                   :commit "f06da0fa160bba55c34f8d4983bfd46ace5e9f71"
                   :review-receipt-ids ["invoke-1789412759661-20813-368c5533"]
                   :review-sha256 "91b801400dd4fc792a009b5d8ae5f10373b6aec659d79d925cd3bbfb8cd961d4"
                   :grounded? true}
    :successor-close {:attempt/id "ea1-6ecd77fa689d4c5adb1e1444d7eba993fc5fb7bb60501b873f0d469c7f745317--attempt-002"
                      :run/id "d7eabcf7-b992-435a-ae11-4ef452eee97a"
                      :closed-at "2026-09-14T22:18:57.401728865Z"
                      :witness-ref "data/wm-full-loop-machinery-53/wm-contract-machinery-53-v1/attempt-002/006-adjudication.edn"
                      :witness-sha256 "36b92116eeff35709d5042a214c12375a6967f2ae623f749aac7139f91d0d900"
                      :grounded? true :production-shaped? true
                      :witness {:resolved? true :dial-moved? true}}
    :authority {:decided-by "codex-24" :review-job "invoke-1789412759661-20813-368c5533"}}
   ;; 3f4cac24--003: R = machinery-54 attempt-001. That attempt was orphaned AFTER
   ;; its authoritative-substrate-discharge adjudication (00:10:13Z, grounded
   ;; witness) and never wrote 007-closed; the adjudication recorded-at is the
   ;; durable close timestamp (same evidence codex-18 used as S for attempt-029).
   {:id "repair-ea1-3f4cac241e58afd9b6eae48e78a2ac7f63925aa3fc05c7e3a3fd6d789d4637a9--attempt-003-artifact-binding-mismatch"
    :repair-close {:attempt/id "ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001"
                   :run/id "e91ede9e-de2d-4968-95bf-d03fb5375b19"
                   :closed-at "2026-09-15T00:10:13.577930192Z"
                   :commit "1344347a269620ee207ba5b79736c2c7ddb66d96"
                   :review-receipt-ids ["invoke-1789430904023-20919-0d8a5672"]
                   :review-sha256 "fcf2619ca5d1588f4dcf4e7094f4cb2b23688d49e060952e13bf76ce5c0a4a84"
                   :grounded? true}
    :successor-close {:attempt/id "ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-002"
                      :run/id "8872d5ec-5604-4c2a-b393-4c690613f8e6"
                      :closed-at "2026-09-15T01:53:56.367410955Z"
                      :witness-ref "data/wm-full-loop-machinery-55/wm-contract-machinery-55-v1/attempt-002/006-adjudication.edn"
                      :witness-sha256 "7a67bbf0211d9f21d076c016cc3bfd2abb5aa7a9dd082ae4efffc77096a18ba7"
                      :grounded? true :production-shaped? true
                      :witness {:resolved? true :dial-moved? true}}
    :authority {:decided-by "codex-24" :review-job "invoke-1789430904023-20919-0d8a5672"}}
   ;; b0eeafa0--001: R = machinery-55 attempt-002, S = machinery-59 attempt-002
   {:id "repair-ea1-b0eeafa0e59b4dc0dd9e0abe1cbbed0e687c5827b3ade8320c98c7f82a79032b--attempt-001-machine-repair-lacks-grounded-review-evidence"
    :repair-close {:attempt/id "ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-002"
                   :run/id "8872d5ec-5604-4c2a-b393-4c690613f8e6"
                   :closed-at "2026-09-15T01:53:56.367410955Z"
                   :commit "0476071e5378675124a279491d07c6ad1f2de37a"
                   :review-receipt-ids ["invoke-1789437171438-20933-61b9773c"]
                   :review-sha256 "daf624af35f5df9c1c51dc6eb7adaf713d900c4d888c947a14c825caab041b3d"
                   :grounded? true}
    :successor-close {:attempt/id "ea1-b82bec4361658ac9fea3bf2acf2b155ee690554056cb43982b320c2554a19bb7--attempt-002"
                      :run/id "2026-09-19-1789828750"
                      :closed-at "2026-09-19T15:06:23.169037975Z"
                      :witness-ref "data/wm-full-loop-machinery-59/wm-contract-machinery-59-v1/attempt-002/006-adjudication.edn"
                      :witness-sha256 "dd4c5f15aef7cecb0b5b5263299da5eab07a951ea77562b95a9473bb9d8d4ea4"
                      :grounded? true :production-shaped? true
                      :witness {:resolved? true :dial-moved? true}}
    :authority {:decided-by "codex-24" :review-job "invoke-1789437171438-20933-61b9773c"}}
   ;; 504ad863--001: R = machinery-59 attempt-002, S = machinery-60 attempt-001
   {:id "repair-ea1-504ad8630070adf67604aab739c0c0184b5ccf632f552ec6d2b55fac7ad71c87--attempt-001-feature-card-missing-or-invalid"
    :repair-close {:attempt/id "ea1-b82bec4361658ac9fea3bf2acf2b155ee690554056cb43982b320c2554a19bb7--attempt-002"
                   :run/id "2026-09-19-1789828750"
                   :closed-at "2026-09-19T15:06:23.169037975Z"
                   :commit "9dfdbcac6de7b84d18d493918df425b5ed875d66"
                   :review-receipt-ids ["invoke-1789829936689-22390-50493740"]
                   :review-sha256 "7a5ab01956fedec2339c6e3b785226c86f8ddf49265a75032a1691d63ecf9db1"
                   :grounded? true}
    :successor-close {:attempt/id "ea1-f9a0a2fabd9e1a60116e846608a0e56e0b6f4a8f23cab048bafdde39a10ec0f6--attempt-001"
                      :run/id "2026-09-19-1789835454"
                      :closed-at "2026-09-19T16:42:47.069685693Z"
                      :witness-ref "data/wm-full-loop-machinery-60/wm-contract-machinery-60-v1/attempt-001/006-adjudication.edn"
                      :witness-sha256 "74d67bd33b73c742b9a8b911de62c204cbdb18a4218116dba84c2473f2e2b5b7"
                      :grounded? true :production-shaped? true
                      :witness {:resolved? true :dial-moved? true}}
    :authority {:decided-by "codex-24" :review-job "invoke-1789829936689-22390-50493740"}}])

(doseq [{:keys [id] :as d} machinery-discharges]
  (println "== successor-resolution!" id)
  (try
    (let [result (repair/successor-resolution!
                  {:obligation (obligation id)
                   :repair-close (assoc (:repair-close d) :repair/id id)
                   :successor-close (assoc (:successor-close d) :repair/id id)
                   :authority (:authority d)
                   :resolution-read-fn resolution-read
                   :resolve-fn repair/resolve!})]
      (pp/pprint {:id id :status (:status result)
                  :resolved-at (get-in result [:resolution :resolved-at])}))
    (catch Throwable e
      (pp/pprint {:id id :refused (.getMessage e) :data (ex-data e)}))))

;; ---------- July findings: direct resolve! (no run/id was ever recorded for
;; outer-loop-45/46; route mirrors resolutions/repair-attempt-029|030) ----------

(def july-discharges
  [;; 6d5da36a: implementation close attempt-056 (07-25); successor attempt-059 (07-27)
   {:id "repair-initialization-6d5da36a-04f0-42ee-ba91-55bee5801a01-initialization-failed"
    :commit "087dc4d74471a4e0abc2d2a07ab3e0d64c059eb4"
    :successor {:attempt-id "attempt-059"
                :reviewer "codex-1" :review-job "invoke-1785143161225-195-0900f48e"
                :close "data/wm-full-loop/wm-outer-loop-46-v1/attempt-059/007-closed.edn"}}
   ;; attempt-051: implementation close attempt-059; successor attempt-060
   {:id "repair-attempt-051-feature-card-missing-or-invalid"
    :commit "4110519b146b43880e2c9440f302bfa3009b9f6a"
    :successor {:attempt-id "attempt-060"
                :reviewer "codex-1" :review-job "invoke-1785144011909-199-046dcbc0"
                :close "data/wm-full-loop/wm-outer-loop-46-v1/attempt-060/007-closed.edn"}}
   ;; attempt-052: implementation close attempt-060; successor attempt-061
   {:id "repair-attempt-052-strategic-selection-unavailable"
    :commit "3bdc381e76518e69f90077397fa46495da98e61c"
    :successor {:attempt-id "attempt-061"
                :reviewer "codex-1" :review-job "invoke-1785149828962-203-0a49a45a"
                :close "data/wm-full-loop/wm-outer-loop-46-v1/attempt-061/007-closed.edn"}}])

(doseq [{:keys [id commit successor]} july-discharges]
  (println "== resolve!" id)
  (try
    (let [close-witness (get-in (read-one (:close successor)) [:payload :judgment :witness])
          resolution (repair/resolve!
                      (obligation id)
                      {:attempt-id (:attempt-id successor)
                       :commit commit
                       :reviewer (:reviewer successor)
                       :review-job (:review-job successor)
                       :witness close-witness
                       :validation {:kind :production-shaped-successor
                                    :production-shaped? true
                                    :successor-close (:close successor)}})]
      (pp/pprint {:id id :resolved-at (:resolved-at resolution)
                  :validation-attempt (:validation-attempt resolution)}))
    (catch Throwable e
      (pp/pprint {:id id :refused (.getMessage e) :data (ex-data e)}))))

(println "== board after ==")
(let [open (repair/open-obligations)]
  (pp/pprint {:open-total (count open)
              :open-machine-failure (count (filter #(= :machine-failure (:repair/class %)) open))}))
(shutdown-agents)
