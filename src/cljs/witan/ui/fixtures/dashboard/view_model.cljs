(ns ^:figwheel-always witan.ui.fixtures.dashboard.view-model
    (:require [cljs.core.async :refer [<! chan]]
              [om.core :as om :include-macros true]
              [schema.core :as s :include-macros true]
              [witan.schema.core :refer [Forecast]]
              [witan.ui.util :as util]
              [witan.ui.services.data :as data]
              [venue.core :as venue :include-macros true])
    (:require-macros [cljs.core.async.macros :as am :refer [go go-loop alt!]]
                     [cljs-log.core :as log]))

(def mb-chan (chan))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-activate
  [args cursor]
  (log/debug "ACTIVE AND " data/logged-in?)
  (when data/logged-in?
    (om/update! cursor :refreshing? true)
    (venue/request! cursor :service/api :refresh-forecasts)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti event-handler
  (fn [event args cursor] event))

(defmulti response-handler
  (fn [result response cursor] result))

(defn view-model
  []
  (reify
    venue/IHandleEvent
    (handle-event [owner event args cursor]
      (event-handler event args cursor))
    venue/IHandleResponse
    (handle-response [owner outcome event response cursor]
      (response-handler [event outcome] response cursor))
    venue/IActivate
    (activate [owner args cursor]
      (on-activate args cursor))))

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
  (om/update! cursor :refreshing? false))

(defmethod response-handler
  [:refresh-forecasts :failure]
  [_ response cursor]
  (api-failure!))
