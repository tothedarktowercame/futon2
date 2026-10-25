(ns futon2.aif.mission-wants-view
  "Read-only: the wants the War Machine would compute for one mission,
  without flying it.  Joe wants to see what the WM sees when it selects
  a mission.

  The derivation is flight's own, through the same functions — this
  namespace builds the same want source a flight would carry and calls
  `futon2.aif.flight/source-wants` on it — but NOTHING IS OBSERVED: the
  observe function records each locator it is handed and returns false,
  so no check ever runs and no universe value is claimed.  The recorded
  locators are returned as :observed-locators with :observation
  :not-run, never as verdicts.

  Routes (flight's two; a committed seam document wins):
  :primary-seam — the mission's seam document
    `holes/missions/seam/<stem>.primary-wants.edn` (E-象-2000-wm-seam
    S2 convention; override with OPTS :seam-repo/:seam-path), read
    through flight's git-primary-seam; a typed absence
    (:no-seam-document, …) falls through to
  :a-exits — the mission doc's completion criteria plus checkbox
    tasks (flight's :a-exits want source).

  OPTS: :repo :path (the mission file), :code-root (default
  /home/joe/code), :store, :lifecycle {:repo :path}, :seam-repo
  (default :repo), :seam-path (default the seam convention above),
  :read-text (test seam: mission text in place of git), :document
  (test seam: a seam document in place of git)."
  (:require [clojure.string :as str]
            [futon2.aif.flight :as flight]
            [futon2.aif.mission-hole-wants :as hole-wants]))

(defn seam-path-for
  "The committed seam document path for MISSION-ID (E-象-2000-wm-seam S2:
  one file per mission, in git, under the mission repo's
  holes/missions/seam/)."
  [mission-id]
  (str "holes/missions/seam/" mission-id ".primary-wants.edn"))

(defn- recording-observe
  "An observe function that observes nothing: each locator handed to it
  is recorded and false is returned (a boolean, as outcome-wants/wants
  demands).  The recordings are the view's :observed-locators; the
  booleans are discarded, so the view never states a verdict."
  []
  (let [seen (atom [])]
    {:observe (fn [locator] (swap! seen conj locator) false)
     :recorded seen}))

(defn- stated-of
  "The first line of TOKEN's stated text in the source's
  :criteria-by-token, when the source read the criterion."
  [criteria-by-token token]
  (some-> (get criteria-by-token token) :stated str
          (str/split-lines) first))

(defn mission-wants-view
  "What the WM would see, selecting MISSION-ID, as data.  Returns
  {:mission … :route … :source-read … :outcomes … :admitted … :waiting …
   :coverage-questions … :out-of-view … :observed-locators …
   :observation :not-run} — read-only; no click, no check run, no
  record written.  A seam document that is absent or unreadable is a
  typed absence on :route under :seam-absent, and the mission is read
  through :a-exits, exactly as a flight configured without a seam
  source would read it."
  [mission-id {:keys [repo path code-root store lifecycle read-text document
                      seam-repo seam-path sources status-class]
               :or {code-root "/home/joe/code"}
               :as _opts}]
  (let [{:keys [observe recorded]} (recording-observe)
        seam-path (or seam-path (seam-path-for mission-id))
        seam-repo (or seam-repo repo)
        ;; the tick's checkbox source assembly (mission-hole-wants),
        ;; the same one a flight's sources carry; its own observation
        ;; booleans are discarded — the view observes nothing
        sources (or sources
                    (when-let [s (and repo path
                                      (hole-wants/mission-source
                                       code-root
                                       (cond-> {:id mission-id
                                                :path (str code-root "/" repo "/" path)}
                                         status-class (assoc :status-class status-class))))]
                      {:wants {mission-id (:want s)}
                       :locators {mission-id (:locators s)}}))
        seam-source (merge {:kind :primary-seam :code-root code-root
                            :observe observe}
                           (cond document {:document document}
                                 seam-repo {:repo seam-repo :path seam-path}))
        seam-result (when (or document seam-repo)
                      (flight/source-wants seam-source {:target mission-id} (or sources {})))
        seam-absence (or (:absent (:source seam-result))
                         (:absence (:source seam-result)))
        source (if seam-absence
                 (cond-> {:kind :a-exits :repo repo :path path
                          :code-root code-root :observe observe}
                   read-text (assoc :read-text (if (ifn? read-text)
                                                 read-text
                                                 (constantly (str read-text))))
                   store (assoc :store store)
                   lifecycle (assoc :lifecycle lifecycle))
                 seam-source)
        result (if seam-absence
                 (flight/source-wants source {:target mission-id} (or sources {}))
                 seam-result)
        src (:source result)
        route (:kind src)
        admitted-tokens (:wants result)
        ;; click-wants merges the tick's sources locators beside the want
        ;; source's own (flight.clj); the view shows the same union so an
        ;; admitted checkbox want carries its C4 class
        locators (merge (get-in sources [:locators mission-id])
                        (:locators result))
        criteria-by-token (:criteria-by-token src)
        waiting (vec (for [u (:unlocated src)]
                       (cond-> (select-keys u [:token :reason :would-be :why :line :decline])
                         (nil? (:reason u)) (assoc :reason :no-admitted-locator)
                         (nil? (:stated u)) (assoc :stated (stated-of criteria-by-token (:token u))))))
        every-token (distinct (concat admitted-tokens (map :token waiting)
                                      (keys criteria-by-token)))]
    {:mission mission-id
     :route (cond-> {:kind route :via (:via src)}
              (= :primary-seam route) (assoc :read (:read src)
                                             :document-source (:document-source src))
              (and (= :a-exits route) (some? seam-absence))
              (assoc :seam-absent {:path seam-path :absence seam-absence})
              :always (into (select-keys src [:repo :path :text-read? :criteria-from
                                              :machine-located])))
     :source-read (if (= :primary-seam route)
                    (:read src)
                    {:repo repo :path path
                     :sha256 (get-in src [:readings-needed :mission-sha])})
     :outcomes (vec (for [t every-token]
                      (cond-> {:token t :stated (stated-of criteria-by-token t)}
                        (contains? criteria-by-token t)
                        (merge (select-keys (get criteria-by-token t) [:kind :phase :line])))))
     :admitted (vec (for [t admitted-tokens]
                      {:token t :stated (stated-of criteria-by-token t)
                       :locator-class (:class (get locators t))
                       :locator (get locators t)}))
     :waiting waiting
     :coverage-questions (:coverage-questions src)
     :out-of-view (:out-of-view src)
     :observed-locators (vec (distinct (concat (map #(get locators %) admitted-tokens)
                                               @recorded)))
     :observation :not-run}))
