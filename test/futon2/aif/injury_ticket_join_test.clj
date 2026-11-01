(ns futon2.aif.injury-ticket-join-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.finding-ticket :as ticket]
            [futon2.aif.injury-ticket-join :as join]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.mission-registry :as registry]
            [futon2.aif.medic-ticket-closure :as closure]
            [futon2.aif.outer-observation :as outer]
            [futon2.aif.outer-observation-test :as fixture]))

(defn with-fixture [f]
  (let [root (fixture/temp-dir)
        ledger (str (io/file root "ledger.edn"))
        path (str (io/file root "tick-run-record-fixture.edn"))
        record {:run/id "fixture" :click/id "click-fixture" :startedAt "2026-11-01T12:00:00Z"
                :failure {:kind :fold-output-invalid :stage :construction :target "M-pattern-posteriors"}}
        opts {:ticket-dir (str (io/file root "project/holes/tickets"))
              :queue-path (str (io/file root "project/resources/wm/ticket-queue.edn"))}]
    (spit path (pr-str record))
    (with-redefs [ticket/destinations (constantly opts)]
      (f {:root root :ledger ledger :path path :record record
          :store (str (io/file root "repair-obligations")) :opts opts}))))

(deftest ingestion-publishes-once-and-field-admits-ticket
  (with-fixture
    (fn [{:keys [root ledger path store opts]}]
      (let [opened (store/ingest-record-file! ledger path)
            id (get-in opened [:injury-ticket :finding/id])
            finding (edn/read-string (slurp (io/file store "findings" (str id ".edn"))))
            bytes (slurp ledger)
            ticket-path (io/file (:ticket-dir opts) (str "T-" id ".md"))
            text (slurp ticket-path)
            second (store/ingest-record-file! ledger path)
            tickets (:tickets (registry/load-tickets (str root)))
            target (:id (first tickets))
            row {:id (keyword target) :status :ready :cone 0 :unblocks 0 :rank 1
                 :dependency-reading {:record-pin {:sha256 (identity/sha256 (.getBytes text "UTF-8"))}}}
            run ((fixture/actual-var 'futon3c.giraffe.run/write-run!)
                 {:root (str (io/file root "giraffe")) :run-id "ticket-field" :git-sha "fixture"
                  :adapter-result {:input {:as-of "2026-11-01" :items [] :edges []} :rejected []}
                  :pass-output {:as-of "2026-11-01" :order [row]}})
            field (binding [outer/*resolve-var* fixture/actual-var]
                    (outer/giraffe-field (:dir run) {}
                      {:loaded-missions {:missions []} :loaded-excursions {:excursions []}
                       :loaded-tickets {:tickets tickets} :ticket-targets [target] :excursion-targets []}))]
        (is (= :appended (:status opened)))
        (is (= :duplicate (:status second)))
        (is (= bytes (slurp ledger)))
        (is (= text (slurp ticket-path)))
        (is (= 1 (count tickets)))
        (is (= "fixture" (:origin-run/id finding)))
        (is (= path (get-in finding [:cause-record-pin :path])))
        (is (str/includes? text (pr-str (:injury-id finding))))
        (is (str/includes? text ":cause-record-pin"))
        (is (= [target] (mapv :target (:field field))))
        (is (empty? (:outer-field/excluded field)))))))

(deftest attachment-resolves-like-inline-and-refuses-replaced-finding
  (with-fixture
    (fn [{:keys [ledger path store record]}]
      (let [opened (store/ingest-record-file! ledger path)
            entry (first (get-in opened [:ledger :entries]))
            id (get-in opened [:injury-ticket :finding/id])
            inline (join/resolve-join store id)
            other (str store "-existing")
            finding {:repair/id "existing" :repair/status :open :origin-run/id "fixture"
                     :opened-at "2026-11-01T12:00:00Z" :failure-kind :fold-output-invalid}
            file (io/file other "findings/existing.edn")]
        (io/make-parents file) (spit file (pr-str finding))
        (let [bytes (slurp file)
              published (repair/publish-injury-ticket! other entry record)
              replay (repair/publish-injury-ticket! other entry record)]
          (is (= :published (:status published)))
          (is (= (:finding/id published) (:finding/id replay)))
          (is (= bytes (slurp file)))
          (is (= (:join inline) (:join (join/resolve-join other "existing"))))
          (is (= 1 (count (.listFiles (io/file other "injury-joins")))))
          (is (= ["existing"] (mapv :id (closure/own-cause-findings other entry))))
          (let [attachment (first (.listFiles (io/file other "injury-joins")))
                text (slurp attachment)]
            (spit attachment (str text "\n"))
            (is (thrown-with-msg? clojure.lang.ExceptionInfo #"attachment conflicts"
                  (repair/publish-injury-ticket! other entry record)))
            (spit attachment text))
          (spit file (pr-str (assoc finding :changed true)))
          (is (= :injury-join-finding-sha-mismatch
                 (:reason (join/resolve-join other "existing"))))
          (is (empty? (closure/own-cause-findings other entry))))))))

(deftest failed-publication-is-retryable-after-durable-opening
  (with-fixture
    (fn [{:keys [ledger path]}]
      (let [first (with-redefs [repair/publish-injury-ticket!
                               (fn [& _] (throw (ex-info "injected outage" {})))]
                    (store/ingest-record-file! ledger path))
            bytes (slurp ledger)
            retry (store/ingest-record-file! ledger path)]
        (is (= :injury-ticket-publication-failed (:reason first)))
        (is (:ledger-durable? first))
        (is (= :duplicate (:status retry)))
        (is (= :published (get-in retry [:injury-ticket :status])))
        (is (= bytes (slurp ledger)))))))
