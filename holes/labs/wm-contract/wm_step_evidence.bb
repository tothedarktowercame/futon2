#!/usr/bin/env bb
;; U55 -- THE EVIDENCE CASSETTE: the part of the world a pin could not hold
;; until now.
;;
;;   bb wm_step_evidence.bb serve <cassette.edn> <port> [--frozen] [--log <path>]
;;
;; C509's finding was that the tick's evidence input has NO CURSOR -- the query
;; is `:limit` + `:since <date>` (war_machine.clj:2817-2840,6968-6972), a
;; sliding wall-clock window over a store that grows -- and that "services
;; cannot be pinned at all". They cannot be pinned; their ANSWERS can. This is
;; a recording HTTP seam in front of the evidence base: the first step through
;; it fills the cassette from the real store, and every later step from the same
;; pin is served the same bytes, so two steps differ only where the machine
;; differs.
;;
;; It is reached by an EXISTING configuration seam, not a new one:
;; `FUTON3C_EVIDENCE_BASE` is the first entry in the stack-wide resolution order
;; (pattern_registry.clj:33-38), which is also how the tests point at a store.
;;
;; ONLY GET IS SERVED. A POST arrives as 405 and is logged as a refusal rather
;; than proxied: the pre-flight's whole claim is that a real tick issues zero
;; POSTs (r6_zero_post_preflight.clj), so a POST reaching here is a fact the
;; step should fail on, not one it should forward.
;;
;; --frozen REFUSES to reach upstream. A frozen step whose miss list is empty is
;; the positive control that the cassette covers everything the tick asked for;
;; without it "the evidence was pinned" would rest on the absence of a symptom.

(require '[babashka.http-client :as http]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[org.httpkit.server :as srv])

(def args *command-line-args*)
(def cmd (first args))
(def cassette-path (second args))
(def port (Long/parseLong (or (nth args 2 nil) "7099")))
(def frozen? (boolean (some #{"--frozen"} args)))
(def log-path (second (drop-while #(not= "--log" %) args)))

(def upstream
  (or (not-empty (System/getenv "FUTON_WM_STEP_UPSTREAM"))
      "http://127.0.0.1:7070"))

(def read-opts {:default (fn [t v] {:unread-tag t :value v})})

(def state
  (atom (if (.exists (io/file cassette-path))
          (edn/read-string read-opts (slurp cassette-path))
          {:cassette/upstream upstream
           :cassette/created-at (str (java.time.Instant/now))
           :cassette/entries {}})))

(def hits (atom 0))
(def fills (atom 0))
(def misses (atom []))
(def refusals (atom []))

(defn- key-of [req]
  (str (:uri req) (when-let [q (not-empty (str (:query-string req)))] (str "?" q))))

(defn- save!
  "Write the cassette ONLY when something was recorded. A serve that filled
   nothing must not rewrite the file: the cassette is part of the pin, and a
   timestamp rewritten on every replay would make the pin's content hash flap
   for a step that changed nothing. (It did, before this: two frozen steps that
   served identical answers left two different cassette shas in their step
   records.)"
  []
  (when (pos? @fills)
    (let [s (assoc @state
                   :cassette/saved-at (str (java.time.Instant/now))
                   :cassette/entry-count (count (:cassette/entries @state)))]
      (spit cassette-path (with-out-str (pp/pprint s))))))

(defn- fetch! [k]
  (let [url (str upstream k)
        resp (http/get url {:headers {"Accept" "application/json"}
                            :timeout 30000 :throw false})]
    {:status (:status resp)
     :content-type (get-in resp [:headers "content-type"] "application/json")
     :body (str (:body resp))
     :recorded-at (str (java.time.Instant/now))
     :url url}))

(defn handler [req]
  (if (not= :get (:request-method req))
    (do (swap! refusals conj {:method (:request-method req) :uri (:uri req)})
        {:status 405 :headers {"Content-Type" "text/plain"}
         :body "wm_step_evidence: only GET is served; a tick that POSTs here is a defect"})
    (let [k (key-of req)]
      (if-let [e (get-in @state [:cassette/entries k])]
        (do (swap! hits inc)
            {:status (:status e) :headers {"Content-Type" (:content-type e)} :body (:body e)})
        (if frozen?
          (do (swap! misses conj k)
              {:status 599 :headers {"Content-Type" "text/plain"}
               :body "wm_step_evidence: frozen cassette has no recording for this request"})
          (let [e (fetch! k)]
            (swap! state assoc-in [:cassette/entries k] e)
            (swap! fills inc)
            (save!)
            {:status (:status e) :headers {"Content-Type" (:content-type e)} :body (:body e)}))))))

(defn- report! []
  (let [r {:cassette/path cassette-path
           :cassette/upstream upstream
           :cassette/frozen? frozen?
           :cassette/entries (count (:cassette/entries @state))
           :cassette/hits @hits
           :cassette/fills @fills
           :cassette/misses @misses
           :cassette/miss-count (count @misses)
           :cassette/non-get-refusals @refusals}]
    (when log-path (spit log-path (with-out-str (pp/pprint r))))
    (binding [*out* *err*]
      (println (format "wm_step_evidence: %d entr(ies), %d hit, %d filled, %d miss, %d non-GET"
                       (count (:cassette/entries @state)) @hits @fills (count @misses) (count @refusals))))))

(case cmd
  "serve"
  (do
    (io/make-parents cassette-path)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (fn [] (save!) (report!))))
    (srv/run-server handler {:port port :ip "127.0.0.1"})
    (println (format "wm_step_evidence: serving %s on 127.0.0.1:%d (frozen? %s, %d recorded)"
                     cassette-path port frozen? (count (:cassette/entries @state))))
    (flush)
    @(promise))
  (do (binding [*out* *err*]
        (println "usage: wm_step_evidence.bb serve <cassette.edn> <port> [--frozen] [--log <path>]"))
      (System/exit 2)))
