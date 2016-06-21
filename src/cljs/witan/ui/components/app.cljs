(ns witan.ui.components.app
  (:require [reagent.core :as r]
            [sablono.core :as sab]
            ;;
            [witan.ui.components.dashboard.workspaces :as workspace-dash]
            [witan.ui.components.dashboard.data :as data-dash]
            [witan.ui.components.split :as split]
            [witan.ui.components.create-workspace :as createws]
            [witan.ui.utils :as utils]
            [witan.ui.data :as data]
            [witan.ui.route :as route])
  (:require-macros [cljs-log.core :as log]
                   [cljs.core.async.macros :as am :refer [go-loop]]))

(defn path
  []
  (.. js/document -location -pathname))

(def route->component
  {:app/workspace-dash   workspace-dash/view
   :app/data-dash        data-dash/view
   :app/workspace        split/view
   :app/create-workspace createws/view})

(defn root-view
  []
  (r/create-class
   {:reagent-render
    (fn [this]
      (let [{:keys [app/route route/data]} this
            active-component (get route->component route)]
        (active-component data)))}))
