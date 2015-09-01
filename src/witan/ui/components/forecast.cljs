(ns ^:figwheel-always witan.ui.components.forecast
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent defcomponentmethod]]
            [sablono.core :as html :refer-macros [html]]
            [inflections.core :as i]
            [schema.core :as s :include-macros true]
              ;;
            [witan.ui.widgets :as widgets]
            [witan.ui.components.model-diagram :as model-diagram]
            [witan.schema.core :refer [Forecast]]
            [witan.ui.async :refer [raise!]]
            [witan.ui.refs :as refs]))

(def valid-actions
  #{:input
    :output
    :model})

(defcomponent
  header
  [forecast owner]
  (render [_]
          (html
           [:div.witan-pw-header
            [:h1 (:name forecast)]])))

(defmulti action-view
  (fn [[action forecast] owner] action))

(defcomponentmethod action-view
  :input
  [[action forecast] owner]
  (render [_]
          (html
           [:div "Input view"])))

(defcomponentmethod action-view
  :output
  [[action forecast] owner]
  (render [_]
          (html
           [:div "Output view"])))

(defcomponentmethod action-view
  :model
  [[action forecast] owner]
  (render [_]
          (html
           [:div "Model view"])))

(defcomponent view
  [cursor owner & opts]
  (render [_]
          (let [{:keys [id action]} (first opts)
                kaction (keyword action)
                forecast (some #(if (= (:id %) id) %) (:forecasts cursor))
                ;; this is directly included in the forecast's data for now. More realistically
                ;; it would be derived from input and output information in the forecast.
                model-shape (select-keys forecast [:n-inputs :n-outputs])]
            (html
             [:div
              (om/build header forecast)
              [:div.pure-g
               [:div.pure-u-1.witan-model-diagram
                (om/build model-diagram/diagram model-shape)]]
              (if (not (contains? valid-actions kaction))
                [:span "Unknown forecast action"]
                [:div
                 [:div.pure-g.witan-pw-area-header
                  [:div.pure-u-1
                   {:class action}
                   [:h2 (i/capitalize action)]]]
                 (om/build action-view [kaction forecast])])]))))
