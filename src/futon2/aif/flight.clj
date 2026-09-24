(ns futon2.aif.flight
  "A flight: the clicks on one mission (or other target), ending in closure
  (PROOF-2a, \"A flight: the clicks on one mission, ending in closure\").

  The target is chosen once and stays fixed across the flight's clicks;
  open stop-lines come first (bbae7593: repair fixes go to the front of the
  queue). Each click's wants are the target's wants from a pluggable WANT
  SOURCE, plus every want an earlier click left unreached, so nothing is
  dropped between clicks. Each click records which wants it advanced
  (false before, true after); a click that advances none is recorded as
  :no-progress and ends the flight, because clicks carry a heavy overhead and
  a click that moves nothing is a counterexample, not a step.

  Pure except `run!`, which calls the injected click and observe functions."
  (:refer-clojure :exclude [run!])
  (:require [futon2.aif.mission-criteria :as criteria]
            [futon2.aif.repair-proposals :as repairs])
  (:import [java.util UUID]))

;; ---------------------------------------------------------------------------
;; Want sources

(defmulti source-wants
  "The target's wants from a want source. Returns
  {:wants [token …] :source {:kind … …}}, and, for wants the tick's
  sources do not already locate, :locators {token locator} and
  :universe {token bool}. SOURCES is the tick's assembled
  sources map; FLIGHT the flight record."
  (fn [want-source _flight _sources] (:kind want-source)))

;; The checkbox reader the tick already runs (mission_hole_wants merges one
;; :hole/h<id> want per unchecked `- [ ]` task into :wants).
(defmethod source-wants :checkbox [_ {:keys [target]} sources]
  {:wants (vec (get-in sources [:wants target]))
   :source {:kind :checkbox :via "futon2.aif.mission-hole-wants"}})

;; The mission's completion criteria read from its text (A-exits,
;; futon2.aif.mission-criteria), plus the checkbox wants. The want source
;; names the mission file: {:kind :a-exits :repo … :path … :code-root …}.
;; A criterion with no stated verdict is a want with no locator; assembly
;; then refuses the target naming it, which is the hole to close.
(defmethod source-wants :a-exits [{:keys [repo path code-root read-text observe lifecycle]} {:keys [target]} sources]
  (let [read (or read-text criteria/read-mission)
        root (or code-root "/home/joe/code")
        text (read root repo path)
        ;; a declared lifecycle file: phases judged in data only are named
        ;; out of view, so a closure over the stated criteria is never read
        ;; as the mission's completion
        out-of-view (when lifecycle
                      (some-> (read root (:repo lifecycle) (:path lifecycle))
                              criteria/data-only-phases))
        cs (criteria/criteria target (or text ""))
        w (criteria/wants cs (cond-> {:repo repo :path path} observe (assoc :observe observe)))]
    {:wants (vec (distinct (concat (get-in sources [:wants target]) (:wants w))))
     :locators (:locators w)
     :universe (:universe w)
     :source {:kind :a-exits :via "futon2.aif.mission-criteria"
              :repo repo :path path :text-read? (some? text)
              :criteria (count cs)
              ;; ordering constraints the mission states in its own words
              :constraints (criteria/constraints target (or text ""))
              ;; token -> the criterion it was read from, for the D11 request
              :criteria-by-token (into {} (map (fn [c] [(:token c) (select-keys c [:kind :line :phase :stated])]))
                                       (:criteria w))
              :unlocated (:unlocated w)
              :out-of-view (vec out-of-view)
              :lifecycle lifecycle}}))

;; A hand-declared list, for tests. Typed on every record it reaches, so a
;; reader can never mistake it for wants the machine read from the mission.
(defmethod source-wants :operator-declared [{:keys [wants declared-by]} _ _]
  {:wants (vec wants)
   :source {:kind :operator-declared :declared-by declared-by
            :note "test-only want list; not read from the mission text"}})

;; ---------------------------------------------------------------------------
;; Choosing the target

(defn choose-target
  "The flight's target. The oldest open stop-line's repair target comes
  first; otherwise REQUESTED. Returns {:target … :chosen-because …}, or nil
  when there is neither."
  [{:keys [open-obligations requested]}]
  (if-let [ob (first (sort-by (juxt :opened-at :repair/id) open-obligations))]
    {:target (repairs/target-id (:repair/id ob))
     :chosen-because {:kind :open-stop-line :repair/id (:repair/id ob)
                      :authority "bbae7593: repair fixes go to the front of the queue"
                      :open-stop-lines (count open-obligations)}}
    (when requested
      {:target requested :chosen-because {:kind :requested}})))

(defn start
  "A new flight on CHOSEN (from `choose-target`) with WANT-SOURCE."
  [chosen want-source & [{:keys [id at]}]]
  (merge chosen
         {:flight/id (or id (str "flight-" (UUID/randomUUID)))
          :started-at at
          :want-source want-source
          :carried-wants []
          :needs []
          :clicks []
          :status :open}))

;; ---------------------------------------------------------------------------
;; One click

(defn click-wants
  "The wants for the flight's next click: the want source's wants plus every
  want carried from earlier clicks, in first-seen order."
  [flight sources]
  (let [{:keys [wants source locators universe]} (source-wants (:want-source flight) flight sources)]
    {:wants (vec (distinct (concat wants (:carried-wants flight))))
     :source source
     :locators (or locators {})
     :universe (or universe {})}))

(defn judge-opts
  "What the flight passes the tick's judge: the fixed target and the wants
  to use for it. The judge considers only this target (flight rule 4)."
  [flight wants]
  {:flight {:flight/id (:flight/id flight)
            :target (:target flight)
            :wants (:wants wants)
            :locators (:locators wants)
            :universe (:universe wants)
            :want-source (:source wants)
            :click (inc (count (:clicks flight)))}})

(defn advanced
  "Wants false or unknown BEFORE and true AFTER."
  [wants before after]
  (vec (filter #(and (not (true? (get before %))) (true? (get after %))) wants)))

(defn record-click
  "FLIGHT after one click. CLICK is
    {:click-id … :wants [..] :want-source {..}
     :before {token bool} :after {token bool}     the target's facts
     :unreached-wants [{:token :reason}]            chosen candidate's receipt
     :abstention {:kind :missing …} | nil}          the judge's decline, if any
  Records the wants advanced; carries unreached wants and the abstention's
  missing input forward; closes when every want holds after the click; ends
  :no-progress when nothing advanced."
  [flight {:keys [click-id wants want-source before after unreached-wants abstention]}]
  (let [moved (advanced wants before after)
        open (vec (remove #(true? (get after %)) wants))
        status (cond (empty? open) :closed
                     (empty? moved) :no-progress
                     :else :open)]
    (-> flight
        (update :clicks conj
                (cond-> {:click-id click-id
                         :n (inc (count (:clicks flight)))
                         :wants wants
                         :want-source want-source
                         :advanced moved
                         :open-after open
                         :unreached-wants (vec unreached-wants)
                         :progress? (boolean (seq moved))}
                  abstention (assoc :abstention (select-keys abstention [:kind :missing :declines]))))
        (update :carried-wants #(vec (distinct (concat % (map :token unreached-wants)))))
        (update :needs #(cond-> % (:missing abstention)
                          (conj {:click-id click-id :kind (:kind abstention)
                                 :missing (:missing abstention)})))
        (assoc :status status)
        ;; what a closure covered: the criteria in view, and what was not
        (cond-> (= :closed status)
          (assoc :closure-scope {:criteria-in-view (count wants)
                                 :want-source (:kind want-source)
                                 :out-of-view (vec (:out-of-view want-source))})))))

;; ---------------------------------------------------------------------------
;; The loop

(defn run!
  "Run FLIGHT to closure. CLICK-FN takes the judge-opts and returns
  {:click-id … :unreached-wants … :abstention …}; OBSERVE-FN takes the
  target and the wants' locators {token locator} (the tick's sources'
  locators, overlaid with the want source's own) and returns the facts
  {token bool}. ASK-FN, when given, runs before each click with the flight,
  its wants and the sources, and returns {:asked [...] :needs [...]}: the
  D11 requests made for wants no interpretation produces (futon2.aif.
  flight-runner/ask-fn); its needs join the flight's. SOURCES-FN returns the tick's
  sources (for the want source). Stops when the flight closes, when a click
  advances nothing, or after MAX-CLICKS (then :status :click-limit, with the
  open wants on the last click). Returns the flight record."
  [flight {:keys [click-fn observe-fn sources-fn max-clicks ask-fn]}]
  (loop [f flight]
    (if (or (not= :open (:status f)) (>= (count (:clicks f)) max-clicks))
      (cond-> f (= :open (:status f)) (assoc :status :click-limit))
      (let [sources (sources-fn)
            wants (click-wants f sources)
            locators (select-keys (merge (get-in sources [:locators (:target f)]) (:locators wants))
                                  (:wants wants))
            before (observe-fn (:target f) locators)
            asked (when ask-fn (ask-fn f wants sources))
            f (cond-> f
                asked (-> (update :asks (fnil conj []) (assoc asked :before-click (inc (count (:clicks f)))))
                          (update :needs into (:needs asked))))
            result (click-fn (judge-opts f wants))
            after (observe-fn (:target f) locators)]
        (recur (record-click f (merge result {:wants (:wants wants)
                                              :want-source (:source wants)
                                              :before before
                                              :after after})))))))
