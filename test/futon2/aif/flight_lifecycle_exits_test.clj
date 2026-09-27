(ns futon2.aif.flight-lifecycle-exits-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.lifecycle-exits-test :as fixtures]
            [futon2.aif.mission-criteria :as criteria]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.cascade-problems :as cp]))

(def derive-text "**Status:** DERIVE (2026-09-27)
## MAP
**MAP exit: Met.**
")
(defn with-flight [target text definition f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "flight-exits-" (make-array java.nio.file.attribute.FileAttribute 0)))
        fl (flight/start (flight/choose-target {:requested target})
                         {:kind :a-exits :repo "futon3c" :path "mission.md" :store (str root)
                          :read-text (fn [_ repo _] (if (= repo "futon4") definition text))
                          :observe #(checks/decl-present? text (:decl %))}
                         {:id "fixture"})]
    (try (f fl (flight/click-wants fl {}))
         (finally (doseq [file (reverse (file-seq root))] (io/delete-file file true))))))

(deftest supplied-wants-assembly-and-progress
  (with-flight "fixture" derive-text @fixtures/definition
    (fn [fl w]
      (let [cs (get-in w [:source :criteria-by-token])
            phases (mapv #(get-in cs [% :phase]) (:wants w))
            token (first (:wants w))
            click {:wants (:wants w) :before (:universe w) :after (assoc (:universe w) token true)}
            moved (flight/record-click fl click)
            still (flight/record-click fl (assoc click :after (:universe w)))
            assembled (cp/assemble
                       {:targets ["fixture"]
                        :sources {:universes {"fixture" (:universe w)} :wants {"fixture" (:wants w)}
                                  :locators {"fixture" (:locators w)}
                                  :interpretations {"fixture" {:patterns {:p {:guard {:needs #{} :forbids #{}}
                                                                               :produces (set (:wants w))}}}}
                                  :candidates {"fixture" [{:precedence [:p] :construction-receipt {:kind :fixture}}]}
                                  :horizon-steps 1 :context-of (constantly :WM) :beta-by-context {:WM 1}}})]
        (is (= [:HEAD :IDENTIFY :MAP :DERIVE] phases))
        (is (every? #(= :how (:role %)) (vals cs)))
        (is (= [false false true false] (mapv (:universe w) (:wants w))))
        (is (= [:ARGUE :VERIFY :INSTANTIATE :DOCUMENT]
               (mapv :phase (get-in w [:source :lifecycle-exits :not-started]))))
        (is (empty? (get-in w [:source :readings-needed :locators])))
        (is (empty? (:refusals assembled)))
        (is (= 1 (count (:problems assembled))))
        (is (= :open (:status moved)))
        (is (= [token] (get-in moved [:clicks 0 :advanced])))
        (is (= :no-progress (:status still)))
        (println :supplied (:wants w) :universe (:universe w)
                 :progress (select-keys (first (:clicks moved)) [:advanced :progress?])
                 :statuses [(:status moved) (:status still)])))))

(deftest stated-exits-stay-first
  (let [text (fixtures/mission "M-futon-seams")
        original (:wants (criteria/wants (criteria/criteria "M-futon-seams" text)
                                         {:repo "futon3c" :path "mission.md"
                                          :observe #(checks/decl-present? text (:decl %))}))]
    (with-flight "M-futon-seams" text @fixtures/definition
      (fn [_ w]
        (is (= 6 (count original)))
        (is (= original (subvec (:wants w) 0 6)))
        (is (= 8 (count (:wants w))))
        (is (empty? (get-in w [:source :lifecycle-exits :not-started])))
        (println :seams-pin fixtures/mission-pin :wants (:wants w))))))

(deftest nonlifecycle-and-unreadable-definition
  (let [text "# Ordinary task
- [ ] A task.
" results (atom [])]
    (doseq [definition [@fixtures/definition nil]]
      (with-flight "ordinary" text definition
        (fn [_ w] (swap! results conj w))))
    (is (= (dissoc (first @results) :source) (dissoc (second @results) :source)))
    (is (= (dissoc (:source (first @results)) :lifecycle-exits)
           (dissoc (:source (second @results)) :lifecycle-exits)))
    (is (= {:absent :lifecycle-definition-unreadable}
           (get-in (second @results) [:source :lifecycle-exits]))))
  (with-flight "fixture" derive-text nil
    (fn [_ w]
      (is (empty? (:wants w)))
      (is (= {:absent :lifecycle-definition-unreadable}
             (get-in w [:source :lifecycle-exits]))))))

(deftest misplaced-verdict-is-not-counted
  (with-flight "fixture" "**Status:** DERIVE
## ARGUE
**MAP exit: Met.**
" @fixtures/definition
    (fn [_ w]
      (let [c (first (get-in w [:source :lifecycle-exits :not-counted]))]
        (is (= :MAP (:phase c)))
        (is (= :verdict-line-misplaced (:reason c)))
        (is (= "ARGUE" (:misplaced-under c)))
        (is (false? (get-in w [:universe (:token c)])))
        (println :misplaced c :wants (:wants w))))))
