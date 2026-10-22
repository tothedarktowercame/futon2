(ns futon2.aif.affective-level-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.affective-level :as sut]
            [futon2.aif.previous-run :as previous-run])
  (:import [java.nio.file Files]))

(defn- close? [x y] (< (Math/abs (- (double x) (double y))) 1.0e-12))
(def half {:pos 0.5 :neg 0.5})

(deftest example-valence-moves
  (let [result (sut/step sut/hesp-level half 1.25 1.0)]
    (is (close? 7/11 (get-in result [:valence-after-update :pos])))
    (is (close? 4/11 (get-in result [:valence-after-update :neg])))
    (is (close? 23/22 (:beta-prior-next result)))
    (is (close? 0.25 (:charge result)))))

(deftest zero-charge-keeps-valence
  (let [q {:pos 0.3 :neg 0.7}
        updated (sut/valence-update sut/hesp-level q 1.25 1.25)]
    (is (= q updated))))

(deftest rising-precision-favours-positive
  (let [pos (sut/affective-evidence sut/hesp-level 1.25 1.0 :pos)
        neg (sut/affective-evidence sut/hesp-level 1.25 1.0 :neg)]
    (is (> pos neg))))

(deftest flat-level-keeps-beta-prior-over-five-steps
  ;; each step's prior is the previous step's :beta-prior-next, as in a run
  (loop [q {:pos 0.2 :neg 0.8}
         beta (sut/descend sut/flat-level {:pos 0.2 :neg 0.8})
         posterior [0.1 0.3 0.7 1.4 3.0]
         seen []]
    (if-let [beta-bar (first posterior)]
      (let [result (sut/step sut/flat-level q beta beta-bar)]
        (is (close? 1.0 (:beta-prior-next result)))
        (recur (:valence-next result) (:beta-prior-next result)
               (next posterior) (conj seen result)))
      (is (= 5 (count seen))))))

(deftest predict-uses-B-k-j-times-q-j
  ;; a non-identity B, so a transposed product would give 6.7/11, not 7.1/11
  (let [level (assoc sut/hesp-level :B {[:pos :pos] 0.9 [:neg :pos] 0.1
                                        [:pos :neg] 0.2 [:neg :neg] 0.8})
        result (sut/step level half 1.25 1.0)]
    (is (close? (/ 7.1 11) (get-in result [:valence-next :pos])))
    (is (close? (+ (* 0.5 (/ 7.1 11)) (* 2.0 (/ 3.9 11))) (:beta-prior-next result)))))

(deftest malformed-inputs-are-typed-absences
  (testing "an absent posterior is never zero charge"
    (is (= :beta-posterior-absent
           (:reason (sut/step sut/hesp-level half 1.25 {:status :absent})))))
  (testing "a missing posterior is never zero charge either"
    (is (= :beta-posterior-absent
           (:reason (sut/step sut/hesp-level half 1.25 nil)))))
  (testing "rate minus charge must stay positive"
    (is (= :affective-evidence-undefined
           (:reason (sut/step sut/hesp-level half 5.0 1.0)))))
  (testing "a valence belief is a probability vector"
    (is (= :valence-not-probability
           (:reason (sut/step sut/hesp-level {:pos 0.6 :neg 0.6} 1.25 1.0)))))
  (testing "each B column is stochastic"
    (is (= :transition-column-not-stochastic
           (:reason (sut/step (assoc-in sut/hesp-level [:B [:pos :pos]] 0.9)
                              half 1.25 1.0))))))

(deftest valence-carry-distinguishes-cold-start-predecessor-absence-and-update
  (let [precision {:beta-posterior {:status :absent :reason :no-precision-update-on-live-route}}
        cold (sut/valence-carry {:status :absent :reason :no-earlier-started-run} precision)
        old (sut/valence-carry {:status :present :previous-run/id "old" :previous {}} precision)
        unclosed (sut/valence-carry {:status :absent :reason :previous-run-not-closed
                                     :previous-run/id "open"} precision)
        performed (sut/valence-carry {:status :present :previous-run/id "one"
                                      :previous {:valence-carry {:q-after half}}}
                                     {:beta-posterior 1.0})]
    (is (= {:pos 0.5 :neg 0.5} (:q-before cold)))
    (is (= 1.25 (:beta-prior cold)))
    (is (= :beta-posterior-absent (get-in cold [:update :reason])))
    (is (= (:q-before cold) (:q-after cold)))
    (is (= {:cold-start :redeclared :after :predecessor-has-no-valence-carry} (:source old)))
    (is (= {:cold-start :redeclared :after :previous-run-not-closed} (:source unclosed)))
    (is (= half (:q-before old)))
    (is (= :performed (get-in performed [:update :status])))
    (is (close? 7/11 (get-in performed [:q-after :pos])))))

(deftest redeclared-pre-change-carry-can-perform-an-update
  (let [c (sut/valence-carry {:status :present :previous-run/id "old" :previous {}}
                             {:beta-posterior 1.0})]
    (is (= {:cold-start :redeclared :after :predecessor-has-no-valence-carry}
           (:source c)))
    (is (= half (:q-before c)))
    (is (= :performed (get-in c [:update :status])))
    (is (close? 7/11 (get-in c [:q-after :pos])))))

(deftest valence-carry-crosses-the-real-sealed-q6-stream
  (let [dir (str (Files/createTempDirectory "r20-valence-carry-" (make-array java.nio.file.attribute.FileAttribute 0)))
        precision {:beta-posterior {:status :absent :reason :no-precision-update-on-live-route}}
        p0 (previous-run/start! dir "one" "2026-10-22T10:00:00Z")
        c1 (sut/valence-carry p0 precision)
        terminal (previous-run/terminal {:run-id "one" :started-at "2026-10-22T10:00:00Z"
                                         :valence-carry c1})
        _ (previous-run/close! dir terminal "run-one.edn")
        p1 (previous-run/start! dir "two" "2026-10-22T11:00:00Z")
        c2 (sut/valence-carry p1 precision)]
    (is (= :no-earlier-started-run (:reason p0)))
    (is (= :declared-cold-start-r20-packet (get-in c1 [:source :provenance])))
    (is (= half (:q-before c1)))
    (is (= :present (:status p1)))
    (is (= c1 (get-in p1 [:previous :valence-carry])))
    (is (= (:q-after c1) (:q-before c2)))
    (is (= "one" (get-in c2 [:source :previous-run/id])))))

(deftest tampered-valence-carry-is-caught-by-the-sealed-terminal
  (let [dir (str (Files/createTempDirectory "r20-valence-tamper-" (make-array java.nio.file.attribute.FileAttribute 0)))
        precision {:beta-posterior {:status :absent :reason :no-precision-update-on-live-route}}
        c1 (sut/valence-carry (previous-run/start! dir "one" "2026-10-22T10:00:00Z") precision)
        terminal (previous-run/terminal {:run-id "one" :started-at "2026-10-22T10:00:00Z"
                                         :valence-carry c1})
        forged (assoc-in terminal [:valence-carry :q-after] {:pos 0.9 :neg 0.1})
        _ (previous-run/close! dir forged "run-one.edn")
        p1 (previous-run/start! dir "two" "2026-10-22T11:00:00Z")
        c2 (sut/valence-carry p1 precision)]
    (is (= :previous-terminal-carrier-tampered (:reason p1)))
    (is (= {:cold-start :redeclared :after :previous-terminal-carrier-tampered} (:source c2)))
    (is (= half (:q-before c2)))
    (is (= :not-performed (get-in c2 [:update :status])))
    (is (= 1.25 (:beta-prior c2)))))

(deftest redeclared-carry-recovers-through-the-next-honest-terminal
  (let [dir (str (Files/createTempDirectory "r20-valence-recovery-" (make-array java.nio.file.attribute.FileAttribute 0)))
        absent-precision {:beta-posterior {:status :absent :reason :no-precision-update-on-live-route}}
        c1 (sut/valence-carry (previous-run/start! dir "one" "2026-10-22T10:00:00Z") absent-precision)
        forged (assoc-in (previous-run/terminal {:run-id "one" :started-at "2026-10-22T10:00:00Z"
                                                  :valence-carry c1})
                          [:valence-carry :q-after] {:pos 0.9 :neg 0.1})
        _ (previous-run/close! dir forged "run-one.edn")
        p2 (previous-run/start! dir "two" "2026-10-22T11:00:00Z")
        c2 (sut/valence-carry p2 {:beta-posterior 1.0})
        _ (previous-run/close! dir (previous-run/terminal {:run-id "two" :started-at "2026-10-22T11:00:00Z"
                                                            :valence-carry c2}) "run-two.edn")
        p3 (previous-run/start! dir "three" "2026-10-22T12:00:00Z")
        c3 (sut/valence-carry p3 absent-precision)]
    (is (= :previous-terminal-carrier-tampered (:reason p2)))
    (is (close? 7/11 (get-in c2 [:q-after :pos])))
    (is (= {:previous-run/id "two"} (:source c3)))
    (is (= (:q-after c2) (:q-before c3)))
    (is (not (contains? (:source c3) :cold-start)))))
