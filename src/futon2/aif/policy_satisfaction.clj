(ns futon2.aif.policy-satisfaction
  "Durable, producer-qualified evidence that one exact reading policy already holds."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.meta-terminal-injury :as terminal]))

(def default-root "/home/joe/code/futon2/data/wm-policy-satisfaction")

(defn completion-checks [construction]
  (vec (sort-by pr-str
                (map #(get-in % [:clause :completion-check])
                     (get-in construction [:cascade :operational-plan :steps])))))

(defn evidence-record [{:keys [run-record construction mission-source-sha256]}]
  (when (and (terminal/producer-qualified-already-satisfied? run-record)
             (string? mission-source-sha256)
             (seq (completion-checks construction)))
    (let [grounded (get-in run-record [:failure :detail :grounded-commit])
          authoring-resolution (get-in construction [:cascade :selected-action
                                                     :authoring-resolution])]
      (when (and (= (completion-checks construction)
                    (:completion-checks authoring-resolution))
                 (string? (:digest authoring-resolution)))
      {:schema :wm/policy-satisfaction-v1
       :mission-id (:mission construction)
       :mission-source-sha256 mission-source-sha256
       :policy-id (:selected-cascade construction)
       :completion-checks (completion-checks construction)
       :authoring-resolution authoring-resolution
       :grounded-commit (select-keys grounded [:repo :sha])
       :producer {:run-id (:run/id run-record)
                  :classification :producer-qualified-already-satisfied}}))))

(defn retain! [root input]
  (when-let [record (evidence-record input)]
    (let [dir (io/file (or root default-root))
          bytes (.getBytes (str (pr-str record) "\n") "UTF-8")
          file-digest (evidence/sha256 bytes)
          target (io/file dir (str file-digest ".edn"))]
      (.mkdirs dir)
      (with-open [out (java.io.FileOutputStream. target)] (.write out bytes))
      (assoc record :record-pin {:sha256 file-digest :path (.getAbsolutePath target)}))))

(defn records [root]
  (let [dir (io/file (or root default-root))]
    (if (.isDirectory dir)
      (->> (.listFiles dir)
           (filter #(and (.isFile %) (.endsWith (.getName %) ".edn")))
           (sort-by #(.getName %))
           (keep (fn [f]
                   (try (let [bytes (java.nio.file.Files/readAllBytes (.toPath f))
                              digest (evidence/sha256 bytes)
                              expected (subs (.getName f) 0 (- (count (.getName f)) 4))]
                          (when (= expected digest)
                            (assoc (edn/read-string (String. bytes "UTF-8"))
                                   :record-pin {:path (.getAbsolutePath f) :sha256 digest})))
                        (catch Exception _ nil))))
           vec)
      [])))

(defn exclude-satisfied [field root mission-sha-by-target]
  (let [retained (records root)
        match (fn [target policy]
                (some #(when (and (= target (:mission-id %))
                                  (= (:policy-id policy) (:policy-id %))
                                  (= (get mission-sha-by-target target) (:mission-source-sha256 %))
                                  (= (get-in policy [:authoring-resolution :completion-checks])
                                     (:completion-checks %))
                                  (= (get-in policy [:authoring-resolution :digest])
                                     (get-in % [:authoring-resolution :digest]))) %)
                      retained))]
    (update field :families
            (fn [families]
              (mapv (fn [{:keys [target-id policies] :as family}]
                      (let [pairs (mapv (fn [p] [p (match target-id p)]) policies)
                            excluded (keep second pairs)
                            kept (mapv first (remove second pairs))
                            failures (into (vec (:failures family))
                                           (map (fn [r] {:kind :policy-already-satisfied
                                                        :policy-id (:policy-id r)
                                                        :record-pin (:record-pin r)
                                                        :grounded-commit (:grounded-commit r)})) excluded)]
                        (cond-> (assoc family :policies kept :failures failures
                                      :failure-count (count failures))
                          (and (seq policies) (empty? kept)) (assoc :status :failed))))
                    families)))))
