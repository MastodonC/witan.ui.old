(ns ^:figwheel-always witan.ui.fixtures.dashboard.view-model
    (:require [om.core :as om :include-macros true]
              [schema.core :as s :include-macros true]
              [witan.schema.core :refer [Forecast]]
              [venue.core :as venue])
    (:require-macros [cljs-log.core :as log]))

(defn update-forecasts!
  []
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti handler
  (fn [event args cursor ctx] event))

(defmethod handler
  :event/filter-forecasts
  [_ filter-text cursor _]
  (s/validate s/Str filter-text)
  (om/update! cursor :filter (not-empty filter-text))
  (update-forecasts!))

(defn view-model
  [ctx]
  (reify
    venue/IHandleEvent
    (handle-event [_ event args cursor]
      (handler event args cursor ctx))
    venue/IActivate
    (activate [_ args cursor])))
