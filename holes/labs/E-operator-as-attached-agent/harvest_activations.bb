#!/usr/bin/env bb
;; harvest_activations.bb -- page every context-retrieval record out of the
;; evidence store and write a compact activation table.
;;
;; The route the excursion's pilot cited, GET /api/alpha/patterns/activation,
;; does NOT exist on futon1b (:7073) -- it was a futon1a route and returns 404
;; today. This pages GET /api/alpha/evidence?tags=context-retrieval instead,
;; following :next-cursor and honouring :incomplete, per API-CONTRACT section 3.
;;
;;   bb harvest_activations.bb [out-file]
;;
;; Read only. Writes one file: activations.edn

(require '[babashka.process :refer [shell]]
         '[clojure.string :as str]
         '[clojure.edn :as edn]
         '[clojure.pprint])

(def base (or (System/getenv "FUTON1B_URL") "http://localhost:7073"))
(def out (or (first *command-line-args*)
             "/home/joe/code/futon2/holes/labs/E-operator-as-attached-agent/activations.edn"))

(defn- get-edn! [url]
  (loop [attempt 1]
    (let [{:keys [out exit]} (shell {:out :string :continue true}
                                    "curl" "-s" "-m" "300" "-w" "\n%{http_code}" url)
          _ (when-not (zero? exit) (throw (ex-info "curl failed" {:url url})))
          i (str/last-index-of out "\n")
          body (subs out 0 i)
          code (str/trim (subs out (inc i)))]
      (cond
        (= "200" code) (edn/read-string body)
        (and (= "503" code) (< attempt 8)) (do (Thread/sleep (* 1500 attempt)) (recur (inc attempt)))
        :else (throw (ex-info "store read failed" {:url url :http code}))))))

(defn- parse-results
  "One {:id :score} per result map. The title span between them may carry
   escaped quotes, so the id->score hop is non-greedy rather than field-typed."
  [body]
  (mapv (fn [[_ id score]] {:id id :score (Double/parseDouble score)})
        (re-seq #"(?s):id \"([^\"]+)\".*?:score ([0-9.eE+-]+)" body)))

(loop [cursor nil page 0 acc []]
  (let [url (str base "/api/alpha/evidence?tags=context-retrieval&limit=1000"
                 (when cursor (str "&cursor-at=" (:at cursor) "&cursor-id=" (:id cursor))))
        d (get-edn! url)
        entries (:entries d)
        rows (mapv (fn [e]
                     (let [b (str (:evidence/body e))
                           rs (parse-results b)
                           q (second (re-find #"(?s)\"query\" \"(.*?)\", \"results\"" b))]
                       {:at (str (:evidence/at e))
                        :agent (:evidence/author e)
                        :session (:evidence/session-id e)
                        :query-chars (count (str/replace (str q) "\\n" "\n"))
                        :results rs}))
                   entries)
        acc (into acc rows)
        nc (:next-cursor d)]
    (binding [*out* *err*]
      (println (format "page %d: %d entries, %d total, incomplete=%s scanned=%s"
                       page (count entries) (count acc) (:incomplete d) (:scanned d))))
    (if (and nc (< page 400))
      (recur nc (inc page) acc)
      (let [ats (map :at acc)]
        (spit out (with-out-str
                    (clojure.pprint/pprint
                     {:source "GET /api/alpha/evidence?tags=context-retrieval (paged)"
                      :pages (inc page)
                      :exhausted? (nil? nc)
                      :record-count (count acc)
                      :earliest (when (seq ats) (first (sort ats)))
                      :latest (when (seq ats) (last (sort ats)))
                      :records acc})))
        (println "wrote" out "records" (count acc) "exhausted" (nil? nc))))))
