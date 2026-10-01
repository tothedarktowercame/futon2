(ns futon2.aif.cascade-revision-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-feedback :as feedback]
            [futon2.aif.cascade-revision :as revision]))

(defn- candidate [id patterns]
  {:kind :cascade-candidate :id id :target "M-current"
   :want [["M-current" :done]]
   :precedence (mapv (fn [p] {:id p :target "M-current"}) patterns)
   :construction-receipt {:kind :machine-constructed}
   :interpretation-receipts {}})

(def original (candidate :C1 [:patterns/a]))
(def blocker
  (revision/blocker "M-current" :reviewer-verdict
                    :review-request-changes {:review-job "review-1"}))

(def metadata
  {:schema feedback/metadata-schema :target "M-current"
   :global-patterns
   {:patterns/b {:successful-applications 0 :incomplete-applications 3}
    :patterns/c {:successful-applications 2 :incomplete-applications 0}}})

(deftest blocker-broadens-from-head-and-preserves-both-identities
  (let [whole {:schema revision/context-schema :scope :whole-mission
               :target "M-current" :path "/mission.md" :sha256 "whole"
               :byte-count 100 :content "HEAD plus all lifecycle sections"}
        result (revision/revise
                {:original original
                 :head-context (revision/head-seed original)
                 :whole-context whole
                 :blocker blocker
                 :proposals [(candidate :C2 [:patterns/a :patterns/b])
                             (candidate :C3 [:patterns/a :patterns/c])]
                 :pattern-feedback metadata})]
    (is (= :revised (:status result)))
    (is (= [:patterns/c] (get-in result [:delta :added])))
    (is (= 2 (count (:history result))))
    (is (not= (get-in result [:history 0 :identity])
              (get-in result [:history 1 :identity])))
    (is (= [:head :whole-mission] (mapv :scope (:history result))))
    (is (not (contains? (:whole-context result) :content))
        "whole mission bytes feed the skim but do not bloat the receipt")
    (is (= [":patterns/a" ":patterns/c"]
           (:shown (revision/apply-to-construction
                    {:selected-action original :shown [":patterns/a"]}
                    result))))))

(deftest failed-extension-is-typed-repair-evidence
  (let [result (revision/revise
                {:original original
                 :head-context (revision/head-seed original)
                 :whole-context {:schema revision/context-schema
                                 :scope :whole-mission :target "M-current"
                                 :path "/mission.md" :sha256 "whole"}
                 :blocker blocker
                 :proposals [original]
                 :pattern-feedback metadata})]
    (is (= :refused (:status result)))
    (is (= :no-distinct-whole-mission-proposal (:kind result)))
    (is (= :present (get-in result [:repair-evidence :status])))
    (is (nil? (get-in result [:repair-evidence :positive-reinforcement])))))

(deftest runner-adapter-pins-the-whole-mission-before-proposing
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "cascade-revision-test"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        mission-file (io/file dir "M-current.md")
        seen (atom nil)]
    (try
      (spit mission-file "# Mission\n\n## HEAD\nInitial.\n\n## DERIVE\nBroader context.\n")
      (let [result
            (revision/revise-from-blocker
             {:mission {:id "M-current" :path (.getPath mission-file)}
              :construction {:selected-action original}
              :blocker blocker
              :pattern-feedback metadata
              :proposals-fn
              (fn [{:keys [whole-mission]}]
                (reset! seen whole-mission)
                [(candidate :C3 [:patterns/a :patterns/c])])})]
        (is (= :revised (:status result)))
        (is (= :whole-mission (:scope @seen)))
        (is (.contains (:content @seen) "Broader context."))
        (is (= (:sha256 @seen) (get-in result [:whole-context :sha256]))))
      (finally
        (doseq [file (reverse (file-seq dir))]
          (io/delete-file file true))))))

(deftest producer-refusal-remains-a-typed-no-extension-repair
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "cascade-revision-refusal-test"
                      (make-array java.nio.file.attribute.FileAttribute 0)))
        mission-file (io/file dir "M-current.md")]
    (try
      (spit mission-file "# Mission\n")
      (let [result
            (revision/revise-from-blocker
             {:mission {:id "M-current" :path (.getPath mission-file)}
              :construction {:selected-action original}
              :blocker blocker
              :proposals-fn
              (fn [_] {:status :refused
                       :kind :raw-or-uninterpreted-revision-response})})]
        (is (= :refused (:status result)))
        (is (= :raw-or-uninterpreted-revision-response (:kind result)))
        (is (= :present (get-in result [:repair-evidence :status])))
        (is (= :raw-or-uninterpreted-revision-response
               (get-in result [:proposal-production :kind]))))
      (finally
        (doseq [file (reverse (file-seq dir))]
          (io/delete-file file true))))))
