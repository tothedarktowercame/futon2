(ns futon2.aif.want-interpretation
  "D11 / H-interp: the machine asks for the interpretation a want lacks.

  A flight's want that is not yet true and that no admitted interpretation
  produces cannot be constructed for; before this seam the target was
  refused (:no-admitted-interpretation, or the constructor's :no-producer).
  The seam has three parts, each reviewable on its own (FIELD-D's split):

    1. REQUEST (this namespace, `unproduced-wants`, `request`, `request!`):
       for one want, the exit token and the criterion text it was read from,
       the target's current facts and existing interpretations, and a pinned
       library retrieval whose query is that criterion.
    2. VALIDATION of a response (loader + constructor + admission).
    3. PUBLICATION into the sources the tick reads.

  Parts 2 and 3 are separate commits. Nothing here interprets: retrieval
  candidates are unjudged, and the response is the answerer's."
  (:require [clojure.string :as str]
            [futon2.aif.interpretation-request :as ireq]))

(defn unproduced-wants
  "Wants that are not true in UNIVERSE and that no pattern in PATTERNS
  produces, in WANTS order. These are the wants a constructor cannot plan
  for, whatever else is admitted."
  [wants universe patterns]
  (let [produced (set (mapcat :produces (vals patterns)))]
    (vec (remove #(or (true? (get universe %)) (contains? produced %)) wants))))

(defn citation-for
  "The citation of CRITERION ({:line n :stated text}) in TEXT, checked
  against the text: the stated span must be the text at those lines, or the
  mission has moved since the want was read and the request refuses."
  [source-id text {:keys [line stated]}]
  (let [lines (vec (str/split-lines text))
        n (count (str/split-lines stated))
        a line b (+ line n -1)
        at (when (<= 1 a b (count lines)) (str/join "\n" (subvec lines (dec a) b)))]
    (when-not (and at (str/starts-with? at stated))
      (throw (ex-info "criterion is not at its recorded lines"
                      {:interpretation/refusal :want/criterion-moved
                       :line line :stated stated :at at})))
    {:source source-id :lines [a b] :quote at}))

(defn request
  "The request for one WANT of TARGET. CRITERION is the reader's record of
  it ({:kind :line :phase :stated}); FACTS the target's current universe;
  PATTERNS the admitted interpretations. RETRIEVAL, when given, is the
  pinned library retrieval (from `request!`)."
  [{:keys [target want criterion facts patterns retrieval]}]
  (cond-> {:schema :wm/want-interpretation-request-v1
           :target target
           :want {:token want
                  :observed (get facts want)
                  :criterion (select-keys criterion [:kind :line :phase :stated])}
           :context {:facts facts
                     :interpretations (into (sorted-map-by #(compare (str %1) (str %2)))
                                            (for [[id p] patterns]
                                              [id (select-keys p [:guard :produces])]))}
           :asks {:what "one interpretation of a futon3/library pattern whose :produces contains the want token"
                  :answer-shape {:pattern "family/name, a file under futon3/library"
                                 :guard {:needs "#{token} from the facts or other interpretations' :produces"
                                         :forbids "#{token}"}
                                 :produces (str "#{" want " ...}")
                                 :receipt {:source {:path "futon3/library/<family>/<name>.flexiarg"
                                                    :sha256 "of the bytes read"}
                                           :reading "how the pattern applies to this criterion"
                                           :scope-limit "what of the pattern does not transfer"
                                           :by "answering agent id"}}
                  :or "a typed decline naming why no library pattern produces this want"}}
    retrieval (assoc :retrieval retrieval)))

(defn request!
  "`request` plus a pinned retrieval over the library whose query is the
  criterion's text at its recorded lines (interpretation-request's pinned
  path; hermetic in tests through OPTIONS). ROOT holds the evidence."
  [{:keys [target criterion] :as m} root options]
  (let [r (ireq/prepare-want-proposal! target :mission root
                                       (fn [src text] [(citation-for src text criterion)])
                                       options)]
    (request (assoc m :retrieval (select-keys r [:target :sources :retrieval])))))
