(ns ^:figwheel-always witan.ui.fixtures.forecast.view-model
    (:require [om.core :as om :include-macros true]
              [venue.core :as venue])
    (:require-macros [cljs-log.core :as log]
                     [witan.ui.macros :as wm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(wm/create-standard-view-model!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-initialise
  [cursor])

(defn on-activate
  [{:keys [id action]} cursor]
  (om/update! cursor :id id)
  (om/update! cursor :action action))
