(ns probe
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.observation-checks :as observation]))
(defn files [root]
  (set (map str (filter #(.isFile %) (file-seq (io/file root))))))
(defn -main [& _]
  (let [root repair/default-root before (files root)
        findings (repair/open-obligations root)
        tickets (filterv #(str/starts-with? (:path %) "/home/joe/code/futon2/holes/tickets/")
                         (:tickets (registry/load-tickets)))
        declarations (for [f (file-seq (io/file "resources/wm/cascade-sources"))
                           :when (and (.isFile f) (str/ends-with? (str f) ".edn"))]
                       {:file (str f) :target (:target (edn/read-string (slurp f)))})
        targets (mapv :id tickets)
        result {:base "b188bcc2" :captured-at (str (java.time.Instant/now))
                :tickets tickets
                :ticket-declarations (filterv #(contains? (set targets) (:target %)) declarations)
                :ticket-first-refusals (problems/assemble {:targets targets :sources {:horizon-steps 2}})
                :done-declaration-controls
                {:done (observation/decl-present? "**Status:** DONE\n" "**Status:** DONE")
                 :open (observation/decl-present? "**Status:** OPEN\n" "**Status:** DONE")
                 :embedded (observation/decl-present? "Example: **Status:** DONE\n" "**Status:** DONE")
                 :registry-status (registry/classify-ticket-status
                                   (registry/ticket-status-text ["**Status:** DONE"]))}
                :store-file-count-before (count before)
                :open-count (count findings)
                :class-counts (frequencies (map :repair/class findings))
                :status-counts (frequencies (map :repair/status findings))
                :failure-kind-counts (frequencies (map :failure-kind findings))
                :findings (mapv #(select-keys % [:repair/id :repair/schema-version :repair/status :repair/class
                                               :failure-kind :failure-stage :target :opened-at :discharge-contract]) findings)
                :store-file-count-after (count (files root))
                :same-store-files? (= before (files root))}]
    (prn result)))
(apply -main *command-line-args*)
