(ns futon2.aif.durable-intern-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.durable-intern :as di]))

(defn- big [tag n] (vec (for [i (range n)] {:tag tag :i i :text (apply str (repeat 20 "x"))})))

(defn- printed-round-trip [m]
  (di/hydrate (edn/read-string (pr-str (di/encode m {:min-bytes 64 :min-total-bytes 0})))))

(deftest repeated-subtrees-are-written-once-and-read-back-identical
  (let [shared (big :shared 50)
        m {:judgment {:controller-decision {:certificate shared :n 1}}
           :ground {:kind :x :decision {:certificate shared :n 1}}
           :other [shared shared]}
        enc (di/encode m {:min-bytes 64 :min-total-bytes 0})]
    (is (di/interned? enc))
    (is (< (count (pr-str enc)) (/ (count (pr-str m)) 2)))
    (is (= m (printed-round-trip m)))
    (testing "the top-level keys stay plain"
      (is (= #{:judgment :ground :other :durable/interned} (set (keys enc)))))))

(deftest nested-repeats-inside-table-entries-resolve
  (let [inner (big :inner 30)
        outer-a {:inner inner :a 1 :pad (big :a 10)}
        outer-b {:inner inner :b 2 :pad (big :b 10)}
        m {:x [outer-a outer-a] :y [outer-b outer-b] :z inner}]
    (is (= m (printed-round-trip m)))))

(deftest equal-values-of-different-kinds-are-not-merged
  (let [v (vec (range 200)) l (apply list (range 200))
        m {:v1 v :v2 v :l1 l :l2 l :n1 [1 1 1] :big1 (vec (repeat 100 1N)) :big2 (vec (repeat 100 1N))
           :long1 (vec (repeat 100 1)) :long2 (vec (repeat 100 1))}
        back (printed-round-trip m)]
    (is (= m back))
    (is (vector? (:v1 back)))
    (is (seq? (:l1 back)))
    (is (seq? (:l2 back)))
    (is (instance? clojure.lang.BigInt (first (:big2 back))))
    (is (instance? Long (first (:long2 back))))))

(deftest compound-map-and-set-members-retain-nested-scalar-kinds
  ;; Clojure equality treats [1] and [1N] as the same map key/set member.
  ;; The first production codec compared only the outer collection class and
  ;; silently hydrated both repeated subtrees as the first one encountered.
  (let [pad (vec (range 200))
        long-map {[1] pad :tag :same}
        bigint-map {[1N] pad :tag :same}
        long-set #{[1] [:pad pad]}
        bigint-set #{[1N] [:pad pad]}
        m {:lm1 long-map :lm2 long-map :bm1 bigint-map :bm2 bigint-map
           :ls1 long-set :ls2 long-set :bs1 bigint-set :bs2 bigint-set}
        back (printed-round-trip m)]
    (is (instance? Long (first (ffirst (:lm1 back)))))
    (is (instance? clojure.lang.BigInt (first (ffirst (:bm1 back)))))
    (is (some #(instance? Long (first %)) (:ls1 back)))
    (is (some #(instance? clojure.lang.BigInt (first %)) (:bs1 back)))))

(deftest small-records-are-unchanged
  (let [m {:a (big :a 50) :b (big :a 50)}]
    (is (identical? m (di/encode m)) "below the default total-size threshold")
    (is (identical? m (di/encode m {:min-total-bytes 0 :min-bytes 1000000})) "nothing large enough repeats")))

(deftest hydrate-leaves-plain-values-alone-and-refuses-a-dangling-ref
  (is (= {:a 1} (di/hydrate {:a 1})))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"no table entry"
                        (di/hydrate {:a {:durable/ref "missing"} :durable/interned {}})))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"contain a cycle"
                        (di/hydrate {:a {:durable/ref "a"}
                                     :durable/interned
                                     {"a" {:nested {:durable/ref "b"}}
                                      "b" {:nested {:durable/ref "a"}}}}))))

(deftest only-keys-leaves-other-top-level-values-exactly-as-given
  (let [shared (big :shared 50)
        m {:decision {:a shared :b shared} :route shared :run/id "r1"}
        enc (di/encode m {:min-bytes 64 :min-total-bytes 0 :only-keys [:decision]})]
    (is (identical? (:route m) (:route enc)) "outside the scope nothing is rewritten")
    (is (= "r1" (:run/id enc)))
    (is (di/interned? enc))
    (is (= m (di/hydrate (edn/read-string (pr-str enc)))))))

(deftest repeats-beneath-one-key-maps-and-one-element-vectors-are-found
  (let [shared (big :shared 50)
        m {:x {:only shared} :y [[shared]] :z 1}
        enc (di/encode m {:min-bytes 64 :min-total-bytes 0})]
    (is (di/interned? enc))
    (is (= m (di/hydrate (edn/read-string (pr-str enc)))))))

(deftest sorted-maps-are-rewritten-without-requiring-transients
  (let [shared (big :shared 50)
        ordered (sorted-map :a shared :b shared)
        m {:ordered ordered :again ordered}
        back (printed-round-trip m)]
    ;; EDN has no sorted-map tag, so round-trip equality—not the concrete map
    ;; class—is the codec promise. The encoder must nevertheless not call
    ;; transient on PersistentTreeMap while preparing the printed form.
    (is (= m back))))

(deftest printed-sha256-equals-hashing-the-pr-str-bytes
  (let [v {:a [1 2N 3.5 "é ü 漢 😀 \n\"q\"" #{:k} '(x y)] :b (big :b 5)}
        md (java.security.MessageDigest/getInstance "SHA-256")
        expected (apply str (map #(format "%02x" (bit-and % 0xff))
                                 (.digest md (.getBytes (pr-str v) "UTF-8"))))]
    (is (= expected (di/printed-sha256 v)))))

(defrecord ProbeRecord [a b])

(deftest records-inside-the-scope-are-written-as-they-are
  (let [shared (big :shared 50)
        rec (->ProbeRecord 1 shared)
        m {:decision {:r rec :x shared :y shared}}
        enc (di/encode m {:min-bytes 64 :min-total-bytes 0 :only-keys [:decision]})]
    (is (identical? rec (get-in enc [:decision :r])))
    (is (di/interned? enc))))
