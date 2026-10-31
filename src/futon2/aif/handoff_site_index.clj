(ns futon2.aif.handoff-site-index
  "Executable index of independently represented production WM handoffs."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def resource "wm/handoff-sites.edn")

(defn read-index []
  (-> resource io/resource slurp edn/read-string))

(defn audit
  "Check index structure and that every stable source anchor still names one
  real production call site. REPO is the futon2 checkout root."
  [repo]
  (let [{:keys [schema sites]} (read-index)
        ids (map :id sites)
        problems
        (concat
         (when-not (= :wm/handoff-site-index-v1 schema)
           [{:reason :schema-mismatch :actual schema}])
         (when-not (= (count ids) (count (distinct ids)))
           [{:reason :duplicate-site-id}])
         (mapcat
          (fn [{:keys [id source anchor kind contract transport status exception
                       representation decoder retention]}]
            (let [file (io/file repo source)
                  text (when (.isFile file) (slurp file))]
              (cond-> []
                (not (.isFile file))
                (conj {:site id :reason :source-missing :source source})
                (and text (not (str/includes? text anchor)))
                (conj {:site id :reason :anchor-missing :source source :anchor anchor})
                (not (and kind contract transport status))
                (conj {:site id :reason :classification-incomplete})
                (and (= :agent-reply kind)
                     (not (and representation decoder)))
                (conj {:site id :reason :representation-contract-missing})
                (and (= :registered-exception status) (str/blank? exception))
                (conj {:site id :reason :exception-unexplained})
                ;; A declared retention schema must name one durable
                ;; replay-input store, the same shape as :contract. Declaring
                ;; retention is a claim the site's failure boundary retains
                ;; the exact refused input, not a response-format rule.
                (and retention (not (keyword? retention)))
                (conj {:site id :reason :retention-schema-invalid
                       :retention retention}))))
          sites))]
    {:schema :wm/handoff-site-audit-v1
     :status (if (seq problems) :refused :verified)
     :site-count (count sites)
     :problems (vec problems)}))
