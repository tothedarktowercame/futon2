(ns futon2.aif.wm.click-ask
  "PROOF-2b: the ordinary click's interpretation ask step (D11, moved into
  the tick). futon2.aif.flight-runner/ask-fn is the ask step; until now only
  run-flight! called it, so an ordinary tick whose every candidate target was
  refused :no-admitted-interpretation abstained without asking anyone. This
  namespace builds the ask a click performs for ONE refused target, with the
  same wants shape the tick itself derives for checkbox missions
  (mission-hole-wants), exactly as the manual demonstration
  /tmp/claude-1/ask-demo.clj did.

  The full-loop runner calls the fn built by click-ask-fn only when its own
  decision abstained refusing a target :no-admitted-interpretation, at most
  once per click, and re-runs the decision once when the ask published. The
  answer-fn is injectable (:interpretation-answer-fn on the runner opts) so
  tests can stub the seat; production answers through Agency with the seat
  named (:interpretation-seat, default codex-proof2c).

  NOT required by full-loop-runner (flight-runner requires it, so that edge
  would cycle); full-loop-runtime installs it as the :interpretation-ask-fn
  runtime default."
  (:require [futon2.aif.cascade-sources :as cs]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.mission-hole-wants :as mhw]
            [futon2.aif.mission-registry :as reg]
            [futon2.aif.want-interpretation :as wi]))

(def default-ask-seat "codex-proof2c")

(def default-ask-seats
  "Seats a click's asks are spread over. ask-fn issues a target's wants
  concurrently, but Agency queues jobs per seat, so one seat would still
  answer them one after another (click 17: six asks, ~3.5 min of seat time)."
  ["codex-proof2c" "codex-proof2a" "codex-proof2b"])

(defn- mission-hole-pick
  "The mission-hole-wants entry for TARGET from the given candidate PICKS
  (the tick's own derivation by default), else nil."
  [picks target]
  (first (filter #(= target (:target %)) picks)))

(defn wants-for
  "The wants map ask-fn needs for TARGET, in the exact shape the tick derives
  for checkbox missions (ask-demo.clj): the entry's want tokens, locators and
  universe, and per-token criteria built from its holes."
  [pick]
  (let [by-token (into {} (map (fn [tok h] [tok {:kind (:kind h) :line (:line h)
                                                 :stated (:text h)}])
                               (:want pick) (:holes pick)))]
    {:wants (:want pick)
     :locators (:locators pick)
     :universe (:universe pick)
     :source {:criteria-by-token by-token :constraints {:requires []}}}))

(defn click-ask-fn
  "The ordinary click's ask step as a full-loop-runner
  :interpretation-ask-fn: (fn [runner-opts refusal] record). REFUSAL is the
  abstained decision's {:target .. :kind :no-admitted-interpretation ..}.
  Returns the :interpretation-ask record {:target :want :outcome :job-id
  :published} for the run record — the outcome is ask-fn's own (published,
  declined, request-refused, ask-threw ...), whether or not anything was
  published.

  Keys read from the builder's OPTS (the runner's own opts by default):
  :machine-interpretations-dir (the store, default
  want-interpretation/default-store), :interpretation-seat,
  :interpretation-answer-fn (the injectable stub seam),
  :cascade-sources-dir and :mission-code-root for the sources/wants
  derivation. Test seams, never set in production: :mission-hole-sources and
  :cascade-sources replace the registry and declared-source reads with fixed
  values, and :ask-options is merged into ask-fn's options (a pinned
  :code-root and :request-options, as flight-ask-test does)."
  [{:keys [machine-interpretations-dir interpretation-seat interpretation-seats
           interpretation-answer-fn
           cascade-sources-dir mission-code-root mission-hole-sources
           cascade-sources ask-options]
    :or {interpretation-seat default-ask-seat}}]
  (fn [runner-opts refusal]
    (let [target (:target refusal)
          store (or machine-interpretations-dir
                    (:machine-interpretations-dir runner-opts)
                    wi/default-store)
          ;; read only when a test seam does not replace it
          missions (delay (:missions (reg/load-missions)))
          code-root (or mission-code-root reg/default-code-root)
          sources (or cascade-sources
                      (cs/with-context-fn
                       (mhw/merge-into-sources
                        (cs/load-declared (or cascade-sources-dir cs/default-dir))
                        code-root @missions :WM)))
          pick (mission-hole-pick (or mission-hole-sources
                                      (:sources (mhw/mission-sources code-root @missions)))
                                  target)]
      (if-not pick
        {:target target :outcome :no-mission-hole-want :published false}
        (let [;; a stub answers every want; otherwise ask-fn builds one
              ;; Agency answer fn per seat and spreads the wants over them
              answering (if interpretation-answer-fn
                          {:answer-fn interpretation-answer-fn}
                          {:interpretation-seats (or interpretation-seats
                                                     (when (not= interpretation-seat default-ask-seat)
                                                       [interpretation-seat])
                                                     default-ask-seats)
                           :agency-opts (runner/config {})})]
          (try
            (let [res ((fr/ask-fn (merge {:store store} answering
                                         (or ask-options {})))
                       {:target target} (wants-for pick) sources)
                  asked (:asked res)
                  published? (boolean (some #(= :published (:outcome %)) asked))
                  first-asked (first asked)]
              (cond-> {:target target
                       :want (:want first-asked)
                       :outcome (:outcome first-asked)
                       :job-id (:job-id first-asked)
                       :published published?}
                (:reasons first-asked) (assoc :reasons (:reasons first-asked))
                (:decline first-asked) (assoc :decline (:decline first-asked))
                (:refusal first-asked) (assoc :refusal (:refusal first-asked))
                (seq (rest asked))
                (assoc :asked (mapv #(select-keys % [:want :outcome :job-id]) asked))))
            (catch Throwable e
              {:target target :outcome :ask-threw
               :error (ex-message e) :published false})))))))
