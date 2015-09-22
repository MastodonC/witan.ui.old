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
  [cursor]
  (let [all-expanded (-> cursor :expanded)
        filter (-> cursor :filter)
        toggled-on (mapv #(vector :db/id (first %)) all-expanded)]
    (venue/request! cursor :service/data :fetch-forecasts
                    {:expand toggled-on
                     :filter filter})))

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
  (s/validate s/Str filter-text)
  (om/update! cursor :filter (not-empty filter-text)))

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
