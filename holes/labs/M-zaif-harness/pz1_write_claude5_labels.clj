#!/usr/bin/env bb
;; H3b — claude-5's independent blind labels for the PZ1 sheet.
;; Labels were assigned by claude-5 reading pz1-labeling-sheet.edn contexts
;; ONLY (zai-1's labels file untouched). This script zips the judgment
;; vector against the sheet in order, asserting each id prefix, so a
;; transcription slip fails loudly instead of mislabeling a row.
;;
;; Judgment rule used (recorded for the mission doc):
;;   correction? = the turn pushes back on / negates / redirects something
;;   the agent did, claimed, planned, or believed inside the loop. Fresh
;;   requests, approvals, idea development, and self-directed musing are NOT
;;   corrections even when a lexicon marker fires.
;;   Route: :gamma = redirects the agent's approach/plan/action;
;;   :c-channel = repairs the agent's model of Joe's intent/meaning/prefs;
;;   :actand = corrects a factual belief about world/artifact state.
(require '[clojure.edn :as edn]
         '[clojure.string :as str])

(def lab-dir "/home/joe/code/futon2/holes/labs/M-zaif-harness/")

(def sheet (edn/read-string (slurp (str lab-dir "pz1-labeling-sheet.edn"))))

;; [id-prefix route-or-nil] in sheet order; nil = not a correction.
(def my-labels
  [;; hits 1-60
   ["e-0cae94f2" nil] ["e-81ccb710" nil] ["e-0ca30163" :gamma] ["e-de53a3ec" nil] ["e-e7b6cc25" nil]
   ["e-bdbf86b2" :gamma] ["e-24f5b8b3" :gamma] ["e-5a7ff7f7" nil] ["e-0ca30163" :gamma] ["e-c95388ea" :gamma]
   ["e-e425ece0" :gamma] ["e-3a5430d8" :gamma] ["e-fcd4a574" nil] ["e-56a2aec4" nil] ["e-db4b8c90" nil]
   ["e-a295670e" :gamma] ["e-3ec1a55c" nil] ["e-190e6fad" nil] ["e-ce907fcf" nil] ["e-fb961ac3" nil]
   ["e-79af5df3" :gamma] ["e-de6a0d84" nil] ["e-11f65574" nil] ["e-b77c3ab7" nil] ["e-cfa92a42" :c-channel]
   ["e-a6e400e6" nil] ["e-f24e518b" nil] ["e-4cf4bc93" nil] ["e-3f46c2a9" :actand] ["e-7007baf8" nil]
   ["e-fdbd1fc7" nil] ["e-7007baf8" nil] ["e-190e6fad" nil] ["e-515e1983" nil] ["e-5900e426" :gamma]
   ["e-c903cf11" nil] ["e-024e0e2b" nil] ["e-308c2ce9" :actand] ["e-566bf6f5" :gamma] ["e-190e6fad" nil]
   ["e-1a35ec6d" nil] ["e-5ed146a9" :gamma] ["e-906d06c9" nil] ["e-fe9c4c7f" nil] ["e-251899fa" :c-channel]
   ["e-0d3c4a1c" :c-channel] ["e-1791adb9" :c-channel] ["e-e25bd803" :c-channel] ["e-1118b60f" :c-channel] ["e-abc95709" :c-channel]
   ["e-0d1d4d3c" nil] ["e-6acf6b98" :c-channel] ["e-c903cf11" nil] ["e-f2e7f7ff" nil] ["e-b78c3d3b" :actand]
   ["e-7aee053c" nil] ["e-5c6c3bb9" nil] ["e-e969095d" nil] ["e-f48e640a" nil] ["e-0cae94f2" nil]
   ;; probes 61-120
   ["e-2ee5e6ec" nil] ["e-ce11db09" nil] ["e-0d743563" nil] ["e-6c85ce9b" nil] ["e-b1ccb9ee" nil]
   ["e-b4acb8ae" nil] ["e-24b74c4f" nil] ["e-ef904b47" nil] ["e-d6b5fce3" nil] ["e-52a3d918" :gamma]
   ["e-7b72a825" :actand] ["e-8c3705a5" nil] ["e-8ae3d162" :gamma] ["e-7f4fc9e6" nil] ["e-be6780c3" :gamma]
   ["e-81b212c2" nil] ["e-f32294be" nil] ["e-1c1496ef" nil] ["e-6eef5ea1" nil] ["e-5bc3b2d5" nil]
   ["e-d46c3031" nil] ["e-fa5c4ca5" nil] ["e-d9fb728d" :actand] ["e-a73a5403" nil] ["e-793a8557" nil]
   ["e-4f23fd65" nil] ["e-62dfc418" nil] ["e-8abc97a6" :gamma] ["e-9dcfe258" nil] ["e-b98b042d" :gamma]
   ["e-6c18cf96" nil] ["e-b9413902" nil] ["e-b3161ed3" nil] ["e-ea316241" nil] ["e-b754bfdf" :gamma]
   ["e-b8de3bbe" nil] ["e-08e7b27f" :c-channel] ["e-c9325128" nil] ["e-74ff0c19" nil] ["e-2c4df309" nil]
   ["e-6dabb568" nil] ["e-4d475963" nil] ["e-d26cc8c4" nil] ["e-cd45abcd" nil] ["e-05c61b68" nil]
   ["e-b4129a5b" nil] ["e-084e6549" nil] ["e-acddcbaf" nil] ["e-cc5a3f6d" nil] ["e-c1958636" nil]
   ["e-4c2ea0a1" nil] ["e-ec3f39b5" nil] ["e-8335f157" nil] ["e-cd951905" nil] ["e-6eb41001" nil]
   ["e-1c6fb64c" nil] ["e-aa885b26" :gamma] ["e-3bc603e7" nil] ["e-45dbfd75" nil] ["e-1719b198" :actand]])

(let [items (:items sheet)]
  (assert (= 120 (count items) (count my-labels))
          (str "count mismatch: items=" (count items) " labels=" (count my-labels)))
  (doseq [[i [item [prefix _]]] (map-indexed vector (map vector items my-labels))]
    (assert (str/starts-with? (:id item) prefix)
            (str "id mismatch at index " i ": sheet=" (:id item) " expected-prefix=" prefix)))
  (let [labels (mapv (fn [item [_ route]]
                       {:id (:id item)
                        :correction? (some? route)
                        :route route})
                     items my-labels)
        out {:labeler "claude-5"
             :method "blind, context-only; zai-1 labels not consulted; markers/route-claimed ignored for the correction? judgment"
             :labels labels}]
    (spit (str lab-dir "pz1-labels-claude5.edn")
          (with-out-str (clojure.pprint/pprint out)))
    (println "wrote pz1-labels-claude5.edn |"
             "trues:" (count (filter :correction? labels))
             "| routes:" (frequencies (keep :route labels)))))
