(ns futon2.aif.meta-dispatch-cause-retention-acceptance-test
  "Acceptance pin for injury/ad038c4804bdd146142db0a9ac74dfe42a863e52.

  The injury: at stage :ticket-readiness-refresh an Agency dispatch came back
  HTTP 500 wrapping the futon1b busy read (503, body text 'futon1b busy (HTTP
  503): the read was not served'). The body is not JSON, so it parses to a
  {:raw ...} fallback, and the production dispatch/selection failure records
  retained ZERO typed causes (causes total-count 0 in the injury record):
  the busy evidence was silently dropped on classification. This test drives
  the real selection entrypoint futon2.aif.full-loop-runner/strategic-selection!
  against a real local HTTP server (com.sun.net.httpserver — a genuine socket
  server, not a stubbed production seam) that returns exactly the injured
  response, and requires that every retained attempt cause carries the
  downstream-busy evidence. On injured code the attempt summaries retain only
  {:attempt .. :status 500 :error ..} and drop the body, so the pin fails;
  after repair each retained cause carries the busy body (or an equivalent
  typed downstream-busy cause containing it), so the pin passes.

  Uses only the production functions named in the bound diagnosis's
  :production-functions: strategic-selection! (selection entrypoint),
  futon2.aif.wm.loop-node-exercise/require-complete! (required loop node
  evaluator), futon2.aif.meta-self-heal-production/route! (terminal routing
  entrypoint). No production seam is stubbed; the only injected value is the
  documented :strategic-selection-sleep-fn option (retry delay elision, a
  timing option, not a dependency) so the retry ladder runs fast."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.loop-node-exercise :as loop-node]
            [futon2.aif.meta-self-heal-production :as production])
  (:import [com.sun.net.httpserver HttpServer HttpHandler HttpExchange]
           [java.net InetSocketAddress]
           [java.nio.charset StandardCharsets]))

(def ^:private injured-busy-body
  ;; Byte-for-byte the injury record's :response {:raw ...} value.
  "futon1b busy (HTTP 503): the read was not served")

(defn- busy-read-server!
  "Real HTTP server on an ephemeral port answering every request with the
  injured response: status 500, non-JSON body. Returns the server."
  []
  (let [server (HttpServer/create (InetSocketAddress. "127.0.0.1" 0) 0)]
    (.createContext server "/"
      (reify HttpHandler
        (handle [_ exchange]
          (let [bytes (.getBytes injured-busy-body StandardCharsets/UTF_8)]
            (.sendResponseHeaders exchange 500 (alength bytes))
            (with-open [os (.getResponseBody exchange)]
              (.write os bytes)))
          (.close exchange))))
    (.start server)
    server))

(deftest downstream-busy-read-retains-its-typed-cause
  (let [server (busy-read-server!)
        port (.getPort (.getAddress server))]
    (try
      (let [opts {:agency-base (str "http://127.0.0.1:" port)
                  ;; Timing option only: collapse the 5s inter-attempt delay.
                  :strategic-selection-sleep-fn (constantly nil)}
            thrown (try
                     (runner/strategic-selection! opts {:request "acceptance-pin"})
                     ::not-thrown
                     (catch clojure.lang.ExceptionInfo e e))]
        (is (instance? clojure.lang.ExceptionInfo thrown)
            "the busy read must surface as the typed strategic-selection throw")
        (when (instance? clojure.lang.ExceptionInfo thrown)
          (let [data (ex-data thrown)
                failures (:attempt-failures data)]
            (is (= :incomplete (:outcome data)))
            (is (= :strategic-selection-unavailable (:failure-kind data)))
            ;; The busy read is a transient class condition: the ladder must
            ;; have retried it rather than failing on the first attempt.
            (is (and (int? (:attempts data)) (> (:attempts data) 1))
                "a 500-wrapped busy read is transient and must be retried")
            ;; THE PIN — the injury's reason: zero retained causes. Every
            ;; attempt failure the production code retains must carry the
            ;; downstream-busy evidence (the raw body, or a typed
            ;; downstream-busy cause that includes it). On injured code the
            ;; summaries drop the unparseable body, this fails; after repair
            ;; it passes.
            (is (pos? (count failures))
                "at least one typed cause must be retained")
            (is (every? #(str/includes? (pr-str %) "futon1b busy")
                        failures)
                "every retained cause must carry the downstream busy evidence")
            (is (= (count failures) (:attempts data))
                "one retained cause per ladder attempt"))))
      (finally
        (.stop server 0)))))

(deftest loop-node-evaluation-and-terminal-routing-pins
  ;; The decision the failed selection leaves behind must still be annotated
  ;; by the real loop-node evaluator, so the failure is attributable to a
  ;; measured node state, not an unmeasured one.
  (let [annotated (loop-node/require-complete! {:selection-certificate {}})
        receipt (get-in annotated [:selection-certificate :loop-node-exercise])]
    (is (map? receipt))
    (is (contains? receipt :status)
        "the loop-node exercise receipt records its completeness status"))
  ;; Terminal routing: with no active durable injury (ledger absent under a
  ;; temp root), the terminal routing entrypoint must return nil — the busy
  ;; read routes back to ordinary selection, never a dedicated META route.
  (let [absent (str (System/getProperty "java.io.tmpdir")
                    "/wm-ad038c4804-absent-ledger-" (gensym) ".edn")]
    (is (nil? (production/route! {:meta-injury-ledger-path absent})))))
