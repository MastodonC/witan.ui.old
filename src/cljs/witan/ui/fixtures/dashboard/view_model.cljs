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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;

(wm/create-standard-view-model!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-activate
  [args cursor]
  (when (data/logged-in?)
    (om/update! cursor :refreshing? true)
    (venue/request! cursor :service/api :refresh-forecasts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-filters!
  [cursor]
  (let [all-expanded (-> cursor :expanded)
        filter (-> cursor :filter)
        toggled-on (mapv #(vector :db/id (first %)) all-expanded)]
    (venue/request! cursor :service/data :get-filters
                    {:expand toggled-on
                     :filter filter})))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn api-failure!
  []
  (log/severe "API failure"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod event-handler
  :event/filter-forecasts
  [_ filter-text cursor _]
  (s/validate s/Str filter-text)
  (om/update! cursor :filter (not-empty filter-text)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod response-handler
  [:refresh-forecasts :success]
  [_ response cursor]
  (update-filters! cursor))

(defmethod response-handler
  [:get-filters :success]
  [_ response cursor]
  (om/update! cursor :forecasts response))

(defmethod response-handler
  [:refresh-forecasts :failure]
  [_ response cursor]
  (api-failure!))
