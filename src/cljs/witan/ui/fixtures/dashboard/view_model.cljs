(ns ^:figwheel-always witan.ui.fixtures.dashboard.view-model
    (:require [cljs.core.async :refer [<! chan]]
              [om.core :as om :include-macros true]
              [schema.core :as s :include-macros true]
              [witan.schema.core :refer [Forecast]]
              [witan.ui.util :as util]
              [witan.ui.services.data :as data]
              [venue.core :as venue :include-macros true])
    (:require-macros [cljs.core.async.macros :as am :refer [go go-loop alt!]]
                     [cljs-log.core :as log]
                     [witan.ui.macros :as wm]))


(defn update-forecasts!
  ([cursor]
   (update-forecasts! cursor {}))
  ([cursor {:keys [expanded filter]}]
   (let [toggled-on (mapv #(vector :db/id (first %)) expanded)]
     (log/debug "Using filter:" filter)
     (venue/request! cursor :service/data :fetch-forecasts
                     {:expand toggled-on
                      :filter filter}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(wm/create-standard-view-model!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-initialise
  [cursor]
  (util/inline-subscribe!
   :data/forecasts-updated
   #(update-forecasts! cursor)))

(defn on-activate
  [args cursor]
  (when (data/logged-in?)
    (om/update! cursor :refreshing? true)
    (venue/request! cursor :service/api :refresh-forecasts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn api-failure!
  [msg]
  (log/severe "API failure:" msg))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod event-handler
  :event/filter-forecasts
  [_ filter-text cursor _]
  (let [new-filter (not-empty filter-text)]
    (om/update! cursor :filter new-filter)
    (update-forecasts! cursor {:filter new-filter
                               :expanded (:expanded @cursor)})))

(defmethod event-handler
  :event/toggle-tree-view
  [_ forecast cursor _]
  (let [db-id        (:db/id forecast)
        id           (:id forecast)
        expanded     (:expanded @cursor)
        toggled?     (contains? expanded [db-id id])
        dfn          (if toggled? disj conj)
        new-expanded (dfn expanded [db-id id])]
    (om/update! cursor :expanded new-expanded)
    (update-forecasts! cursor {:filter (:filter @cursor)
                               :expanded new-expanded})))

(defmethod event-handler
  :event/select-forecast
  [_ forecast cursor]
  (om/update! cursor :selected (vector (:db/id forecast) (:id forecast))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod response-handler
  [:fetch-forecasts :success]
  [_ {:keys [forecasts has-ancestors]} cursor]
  (om/update! cursor :forecasts forecasts)
  (om/update! cursor :has-ancestors has-ancestors)
  (om/update! cursor :refreshing? false))

(defmethod response-handler
  :default
  [_ response cursor])
