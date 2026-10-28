(ns futon2.aif.outer-state-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-state :as state]))

(def valid-makeup
  {:schema :wm/outer-makeup-v1
   :c-kit {:focused 55/100 :related 35/100 :unrelated 5/100
           :stop-the-line 5/100 :not-yet-evaluated 0}
   :kappa 100
   :state-kit {:ready {:doable 3 :blocked 1}
               :not-ready {:doable 1 :blocked 3}
               :undetermined {:doable 1 :blocked 1}}
   :gain-kit {:none 1 :one 2 :several 3}
   :eta-c 1 :omega-c 9/10
   :eta-state 1 :omega-state 9/10
   :eta-gain 1 :omega-gain 9/10
   :default-reach 1/2})

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "outer-state-test" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn write-edn! [file value]
  (spit file (str (pr-str value) "\n"))
  file)

(deftest init-from-kits
  (testing "Lean init shape has c = kappa * kit"
    (let [dir (temp-dir)
          makeup (state/load-makeup (write-edn! (io/file dir "makeup.edn") valid-makeup))
          initialized (state/init-state makeup)]
      (is (= {:focused 55 :related 35 :unrelated 5 :stop-the-line 5
              :not-yet-evaluated 0}
             (:c initialized)))
      (is (= (:state-kit valid-makeup) (:state-counts initialized)))
      (is (= (:gain-kit valid-makeup) (:gain-counts initialized)))
      (is (= 0 (:click-count initialized))))))

(deftest write-read-round-trip
  (let [dir (temp-dir)
        makeup (state/load-makeup (write-edn! (io/file dir "makeup.edn") valid-makeup))
        path (io/file dir "state.edn")
        expected (assoc (state/init-state makeup) :click-count 4)]
    (state/write-state! path expected)
    (is (= (assoc expected :source :durable) (state/read-state makeup path)))))

(deftest absent-state-is-init
  (let [dir (temp-dir)
        makeup (state/load-makeup (write-edn! (io/file dir "makeup.edn") valid-makeup))]
    (is (= :init (:source (state/read-state makeup (io/file dir "absent.edn")))))))

(deftest changed-makeup-refuses
  (let [dir (temp-dir)
        makeup-file (io/file dir "makeup.edn")
        state-file (io/file dir "state.edn")
        original (state/load-makeup (write-edn! makeup-file valid-makeup))]
    (state/write-state! state-file (state/init-state original))
    (let [changed (state/load-makeup
                   (write-edn! makeup-file (assoc valid-makeup :default-reach 3/4)))]
      (is (= :makeup-changed
             (:kind (ex-data (try (state/read-state changed state-file)
                                  (catch clojure.lang.ExceptionInfo e e)))))))))

(deftest invalid-makeup-refuses
  (let [dir (temp-dir)
        load-kind (fn [value]
                    (:kind (ex-data
                            (try (state/load-makeup
                                  (write-edn! (io/file dir (str (random-uuid) ".edn")) value))
                                 (catch clojure.lang.ExceptionInfo e e)))))]
    (is (= :invalid-makeup (load-kind (assoc valid-makeup :omega-c 0))))
    (is (= :invalid-makeup
           (load-kind (assoc-in valid-makeup [:state-kit :ready :doable] -1))))))

(deftest failed-write-preserves-previous-state
  (let [dir (temp-dir)
        makeup (state/load-makeup (write-edn! (io/file dir "makeup.edn") valid-makeup))
        path (io/file dir "state.edn")
        before (state/init-state makeup)
        after (assoc before :click-count 1)]
    (state/write-state! path before)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"injected write failure"
                          (binding [state/*before-state-move*
                                    (fn [_ _] (throw (ex-info "injected write failure" {})))]
                            (state/write-state! path after))))
    (is (= (assoc before :source :durable) (state/read-state makeup path)))))
