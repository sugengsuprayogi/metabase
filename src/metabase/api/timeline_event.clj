(ns metabase.api.timeline-event
  "/api/timeline-event endpoints."
  (:require [compojure.core :refer [DELETE GET POST PUT]]
            [metabase.analytics.snowplow :as snowplow]
            [metabase.api.common :as api]
            [metabase.models.collection :as collection]
            [metabase.models.timeline :refer [Timeline]]
            [metabase.models.timeline-event :as timeline-event :refer [TimelineEvent]]
            [metabase.util :as u]
            [metabase.util.date-2 :as u.date]
            [metabase.util.i18n :refer [tru]]
            [metabase.util.schema :as su]
            [schema.core :as s]
            [toucan.db :as db]))

(api/defendpoint POST "/"
  "Create a new [[TimelineEvent]]."
  [:as {{:keys [name description timestamp time_matters timezone icon timeline_id source question_id archived] :as body} :body}]
  {name         su/NonBlankString
   description  (s/maybe s/Str)
   timestamp    su/TemporalString
   time_matters (s/maybe s/Bool)
   timezone     s/Str
   icon         (s/maybe timeline-event/Icons)
   timeline_id  su/IntGreaterThanZero
   source       (s/maybe timeline-event/Sources)
   question_id  (s/maybe su/IntGreaterThanZero)
   archived     (s/maybe s/Bool)}
  ;; deliberately not using api/check-404 so we can have a useful error message.
  (let [timeline (Timeline timeline_id)]
    (when-not timeline
      (throw (ex-info (tru "Timeline with id {0} not found" timeline_id)
                      {:status-code 404})))
    (collection/check-write-perms-for-collection (:collection_id timeline))
    ;; todo: revision system
    (let [parsed (if (nil? timestamp)
                   (throw (ex-info (tru "Timestamp cannot be null") {:status-code 400}))
                   (u.date/parse timestamp))
          tl-event (merge (dissoc body :source :question_id)
                          {:creator_id api/*current-user-id*
                           :timestamp parsed})]
      (snowplow/track-event! ::snowplow/new-event-created
                             api/*current-user-id*
                             (cond-> {:time_matters time_matters
                                      :collection_id (:collection_id timeline)}
                               (boolean source) (assoc :source source)
                               (boolean question_id) (assoc :question_id question_id)))
      (db/insert! TimelineEvent tl-event))))

(api/defendpoint GET "/:id"
  "Fetch the [[TimelineEvent]] with `id`."
  [id]
  (api/read-check TimelineEvent id))

(api/defendpoint PUT "/:id"
  "Update a [[TimelineEvent]]."
  [id :as {{:keys [name description timestamp time_matters timezone icon timeline_id archived]
            :as   timeline-event-updates} :body}]
  {name         (s/maybe su/NonBlankString)
   description  (s/maybe s/Str)
   timestamp    (s/maybe su/TemporalString)
   time_matters (s/maybe s/Bool)
   timezone     (s/maybe s/Str)
   icon         (s/maybe timeline-event/Icons)
   timeline_id  (s/maybe su/IntGreaterThanZero)
   archived     (s/maybe s/Bool)}
  (let [existing (api/write-check TimelineEvent id)
        timeline-event-updates (cond-> timeline-event-updates
                                 (boolean timestamp) (update :timestamp u.date/parse))]
    (collection/check-allowed-to-change-collection existing timeline-event-updates)
    ;; todo: if we accept a new timestamp, must we require a timezone? gut says yes?
    (db/update! TimelineEvent id
      (u/select-keys-when timeline-event-updates
        :present #{:description :timestamp :time_matters :timezone :icon :timeline_id :archived}
        :non-nil #{:name}))
    (TimelineEvent id)))

(api/defendpoint DELETE "/:id"
  "Delete a [[TimelineEvent]]."
  [id]
  (api/write-check TimelineEvent id)
  (db/delete! TimelineEvent :id id)
  api/generic-204-no-content)

(api/define-routes)
