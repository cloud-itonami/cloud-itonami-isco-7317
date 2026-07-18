(ns woodbasketry.advisor
  "Wood & Basketry Handicraft Workshop Advisor — proposing a workshop
  scheduling/logistics coordination operation (log a work record,
  schedule a crew operation, flag a safety concern, coordinate a
  wood/basketry-materials supply order) from a crew roster, workshop
  registration and safety-reporting policy. Swappable mock/llm; the
  advisor ONLY proposes — `woodbasketry.governor` independently gates
  every proposal and always escalates safety concerns and
  above-threshold supply orders. The advisor never proposes to
  directly finalize a craft-fabrication-execution decision (e.g. a
  specific carving, weaving or finishing step) or to override a
  workshop safety officer's judgment — those stay permanently out of
  this actor's scope. Modeled on cloud-itonami-isco-7111's
  housebuilder.advisor (and closely on cloud-itonami-isco-9311's
  mininglabor.advisor for the physical-safety-domain shape).

  A proposal: {:op :log-work-record|:schedule-crew-operation|
               :flag-safety-concern|:coordinate-supply-order
               :effect :propose :worker-id str :workshop-id str
               :cost number :hazard-type kw :task str :stake kw
               :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- rationale-for [op worker-id workshop-id hazard-type]
  (case op
    :log-work-record
    (str "logged work record for worker " worker-id " at workshop " workshop-id)

    :schedule-crew-operation
    (str "scheduled crew operation for basketry-weaving task at workshop " workshop-id)

    :flag-safety-concern
    (str "flagged " (name (or hazard-type :hazard)) " concern for worker "
         worker-id " at workshop " workshop-id " — routed for workshop safety officer review")

    :coordinate-supply-order
    (str "coordinated supply order for worker " worker-id " at workshop " workshop-id)

    (str "proposed " (name op) " for worker " worker-id " at workshop " workshop-id)))

(defn- infer [_store {:keys [op stake worker-id workshop-id cost hazard-type task]
                       :as request}]
  {:op op
   :effect :propose
   :worker-id worker-id
   :workshop-id workshop-id
   :cost cost
   :hazard-type hazard-type
   :task task
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (rationale-for op worker-id workshop-id hazard-type)})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a wood & basketry handicraft workshop scheduling/logistics
   coordination advisor. Given a request, propose an :op (one of
   :log-work-record, :schedule-crew-operation, :flag-safety-concern,
   :coordinate-supply-order), the :worker-id, :workshop-id, and any
   :cost/:hazard-type/:task fields, an honest :confidence and a
   :stake. Never propose an op outside this closed list, and never
   propose to directly finalize a craft-fabrication-execution decision
   (e.g. deciding to proceed with a specific carving, weaving or
   finishing step), or to override a workshop safety officer's
   judgment — those are always out of this actor's scope; it
   coordinates workshop scheduling/logistics only and never performs
   the craft fabrication work or authorizes craft-fabrication
   execution itself. Safety concerns (cut hazards, dust exposure,
   repetitive-strain injury) always require human sign-off regardless
   of confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
