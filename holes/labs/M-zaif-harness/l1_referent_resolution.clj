#!/usr/bin/env bb
;; L1 mark referent resolution for M-marks-to-labels.
;;
;; Read-only against laptop futon1b store (:7073). Reuses z1_views/page-all and
;; fetch/text-search helpers; does not query lucy, so b1-live-marks.edn events
;; are carried as split-store metadata only.

(ns l1-referent-resolution
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]))

(load-file (str (fs/parent (fs/absolutize *file*)) "/z1_views.clj"))
(def page-all* (resolve 'z1-views/page-all))
(def fetch-by-id* (resolve 'z1-views/fetch-by-id))
(def text-search-raw* (resolve 'z1-views/text-search-raw))

(def lab-dir (str (fs/parent (fs/absolutize *file*))))
(def out-path (str (fs/path lab-dir "l1-mark-adjudications.edn")))
(def b1-path (str (fs/path lab-dir "b1-live-marks.edn")))
(def store-name "laptop :7073")
(def glyph-types {"✘" :correction "✓" :approval "💡" :idea})

(declare scan-entry-marks)

(defn body-text [entry]
  (or (get-in entry [:evidence/body :text])
      (get-in entry [:evidence/body :message])
      (get-in entry [:evidence/body :content])
      (pr-str (:evidence/body entry))))

(defn classify-ref [token]
  (cond
    (nil? token) {:kind :unresolved :id nil}
    (str/starts-with? token "ft-") {:kind :fold-deposit :id token}
    (re-find #"^[MEC]-" token) {:kind :mission-doc :id token}
    (str/starts-with? token "e-") {:kind :evidence-turn :id token}
    :else {:kind :unresolved :id token}))

(defn mark-events-from-entry [entry]
  (let [structured (seq (get-in entry [:evidence/body :marks]))
        marks (if structured
                (filter #(= :event (:verdict %)) structured)
                (scan-entry-marks entry))]
    (for [m marks]
      {:source (if structured :l0-tagged-turn :historical-glyph-scan)
       :entry entry
       :mark (assoc m
                    :evidence-id (:evidence/id entry)
                    :at (:evidence/at entry)
                    :author (:evidence/author entry))})))

(defn word-before [text idx]
  (some-> (re-find #"([A-Za-z']+)[^A-Za-z']*$" (subs text 0 idx))
          second
          str/lower-case))

(defn mention-after? [text idx glyph]
  (let [after (subs text (min (count text) (+ idx (count glyph))))]
    (boolean (re-find #"^\s*(?:for\s+(?:example|approval|correction|idea)s?|as\s+(?:an?\s+)?(?:approval|correction|idea)s?)\b"
                      (str/lower-case after)))))

(defn mention? [text idx glyph]
  (or (contains? #{"a" "an" "the" "with" "like"} (word-before text idx))
      (mention-after? text idx glyph)))

(defn scan-entry-marks [entry]
  (let [text (body-text entry)]
    (vec
     (for [[glyph mark-type] glyph-types
           :let [idx (str/index-of text glyph)]
           :when (and idx (not (mention? text idx glyph)))]
       {:glyph glyph
        :verdict :event
        :type mark-type
        :offset idx
        :ref nil
        :payload nil}))))

(defn tagged-mark-events
  [{:keys [since limit] :or {since "2026-07-12T00:00:00Z" limit 200}}]
  (let [{:keys [entries]} (page-all*
                           {:since since
                            :limit limit
                            :extra-params {:author "joe" :type "coordination"}})]
    (vec (mapcat mark-events-from-entry entries))))

(defn b1-events []
  (let [data (edn/read-string (slurp b1-path))]
    (for [e (:events data)]
      {:source :b1-split-store-event
       :entry nil
       :mark {:evidence-id (:id e)
              :at (:at e)
              :author (:author e)
              :type (:mark e)
              :glyph (case (:mark e) :approval "✓" :correction "✘" :idea "💡" nil)
              :verdict :event}
       :mission (:mission e)})))

(defn find-quoted-span
  [payload]
  (let [query {:endpoint "text-search" :q payload :limit 10}
        raw (text-search-raw* payload 10)
        ids (keep #(or (:id %) (:evidence/id %) (get-in % [:entry :evidence/id])
                       (get % "id"))
                  (:results raw))]
    (loop [[eid & more] ids]
      (if-not eid
        (let [{:keys [entries]} (page-all*
                                 {:since "2026-07-12T00:00:00Z"
                                  :limit 200
                                  :extra-params {:author "joe" :type "coordination"}})
              hit (first (filter #(str/index-of (body-text %) payload) entries))]
          (if hit
            (let [text (body-text hit)
                  idx (str/index-of text payload)]
              {:referent {:kind :evidence-turn
                          :id (:evidence/id hit)
                          :span? {:text payload :start idx :end (+ idx (count payload))}}
               :confidence :medium
               :provenance {:queries [query {:endpoint "page-all" :recheck :substring
                                             :since "2026-07-12T00:00:00Z"}]
                            :store store-name
                            :note "text-search produced no rechecked candidate; paged store re-check found span"}})
            {:referent {:kind :unresolved :id nil}
             :confidence :none
             :provenance {:queries [query] :store store-name :note "no rechecked span"}}))
        (let [entry (fetch-by-id* eid)
              text (body-text entry)
              idx (when (and entry payload) (str/index-of text payload))]
          (if idx
            {:referent {:kind :evidence-turn
                        :id eid
                        :span? {:text payload :start idx :end (+ idx (count payload))}}
             :confidence :medium
             :provenance {:queries [query {:endpoint "fetch-by-id" :id eid :recheck :substring}]
                          :store store-name}}
            (recur more)))))))

(defn resolve-event
  [{:keys [entry mark mission source]}]
  (let [base-mark (select-keys mark [:evidence-id :at :author :glyph :type :verdict :ref :payload :offset])
        clocked (or mission
                    (get-in entry [:evidence/body :clocked-mission])
                    (get-in entry [:evidence/body :mission-id]))]
    (cond
      (:ref mark)
      {:mark base-mark
       :referent (classify-ref (:ref mark))
       :mission clocked
       :confidence :high
       :provenance {:queries [{:source source :rung :explicit-ref :ref (:ref mark)}]
                    :store store-name}}

      (seq (:payload mark))
      (merge {:mark base-mark :mission clocked}
             (find-quoted-span (:payload mark)))

      entry
      {:mark base-mark
       :referent {:kind :evidence-turn :id (or (:evidence/in-reply-to entry)
                                               (:evidence/id entry))}
       :mission clocked
       :confidence :low
       :provenance {:queries [(cond-> {:source source :rung :bare-mark}
                                (:evidence/in-reply-to entry)
                                (assoc :in-reply-to (:evidence/in-reply-to entry))

                                (not (:evidence/in-reply-to entry))
                                (assoc :self-turn (:evidence/id entry)))]
                    :store store-name
                    :note (when-not (:evidence/in-reply-to entry)
                            "bare mark had no in-reply-to in laptop store; resolved to marked turn itself")}}

      :else
      {:mark base-mark
       :referent {:kind :unresolved :id (:evidence-id mark)}
       :mission clocked
       :confidence :none
       :provenance {:queries [{:source source :rung :unresolved}]
                    :store (if (= source :b1-split-store-event)
                             "lucy metadata only; not queried"
                             store-name)
                    :note "no explicit ref, payload span, or in-reply-to"}})))

(defn resolve-all
  [opts]
  (mapv resolve-event (concat (tagged-mark-events opts) (b1-events))))

(def synthetic-reply
  {:evidence/id "e-reply"
   :evidence/author "claude-1"
   :evidence/body {:event "chat-turn" :text "the specific quoted fragment is here"}})

(def synthetic-long
  {:source :synthetic
   :entry {:evidence/id "e-mark" :evidence/author "joe"
           :evidence/body {:event "chat-turn" :clocked-mission "M-autoclock-in"}}
   :mark {:evidence-id "e-mark" :glyph "✘" :type :correction :verdict :event
          :ref "ft-autoclock-in-002" :payload "why"}})

(deftest explicit-ref-rung
  (let [r (resolve-event synthetic-long)]
    (is (= {:kind :fold-deposit :id "ft-autoclock-in-002"} (:referent r)))
    (is (= :high (:confidence r)))))

(deftest unresolved-rung
  (let [r (resolve-event {:source :synthetic
                          :entry {:evidence/id "e-x" :evidence/body {:event "chat-turn"}}
                          :mark {:evidence-id "e-x" :glyph "✓" :type :approval :verdict :event
                                 :ref "not-a-known-kind"}})]
    (is (= :unresolved (get-in r [:referent :kind])))))

(deftest bare-rung
  (let [r (resolve-event {:source :synthetic
                          :entry {:evidence/id "e-mark" :evidence/in-reply-to "e-agent"
                                  :evidence/body {:event "chat-turn"}}
                          :mark {:evidence-id "e-mark" :glyph "💡" :type :idea :verdict :event}})]
    (is (= {:kind :evidence-turn :id "e-agent"} (:referent r)))
    (is (= :low (:confidence r)))))

(deftest quoted-fragment-rung-live-store
  (let [r (find-quoted-span "tags that I enter")]
    (is (= :evidence-turn (get-in r [:referent :kind])))
    (is (map? (get-in r [:referent :span?])))
    (is (= :medium (:confidence r)))))

(defn run-live-tests []
  (let [events (tagged-mark-events {:since "2026-07-12T00:00:00Z" :limit 80})
        ideas (filter #(= :idea (get-in % [:mark :type])) events)
        resolved (map resolve-event ideas)]
    (is (seq ideas) "laptop store should contain at least one live idea mark")
    (is (some #(and (= :evidence-turn (get-in % [:referent :kind]))
                    (= :low (:confidence %)))
              resolved)
        "live idea mark resolves bare to in-reply-to at low confidence")))

(deftest live-idea-rung
  (run-live-tests))

(defn write-output! []
  (let [records (resolve-all {:since "2026-07-12T00:00:00Z" :limit 200})
        envelope {:generated-at (str (java.time.Instant/now))
                  :store-scope "laptop :7073 only; lucy not queried"
                  :records records}]
    (spit out-path (with-out-str (pprint/pprint envelope)))
    envelope))

(defn -main [& args]
  (case (first args)
    "test" (let [r (run-tests 'l1-referent-resolution)]
             (println (format "SUMMARY L1 %d tests, %d assertions, %d failures, %d errors"
                              (:test r) (+ (:pass r) (:fail r) (:error r))
                              (:fail r) (:error r)))
             (when (or (pos? (:fail r)) (pos? (:error r))) (System/exit 1)))
    "write" (let [env (write-output!)]
              (println (format "WROTE %s records=%d store=\"%s\""
                               out-path (count (:records env)) (:store-scope env))))
    (do
      (println "Usage: bb l1_referent_resolution.clj test|write")
      (System/exit 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
