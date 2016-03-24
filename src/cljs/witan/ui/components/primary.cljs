(ns witan.ui.components.primary
  (:require [om.next :as om :refer-macros [defui]]
            [sablono.core :as sab]
            ;;
            [witan.ui.components.icons :as icons])
  (:require-macros
   [cljs-log.core :as log]))

(defn switcher
  [{:keys [icon-0 icon-1 selected-idx on-select]}]
  [:div.primary-switcher
   [:div#indicator-container
    [:div#indicator
     {:class (when (= selected-idx 1) "indicator-offset-1")}]]
   [:div.icons
    [:div.icon#icon-0
     {:class (when (= selected-idx 0) "selected")
      :on-click #(when on-select (on-select 0))}
     (icon-0)]
    [:div.icon#icon-1
     {:class (when (= selected-idx 1) "selected")
      :on-click #(when on-select (on-select 1))}
     (icon-1)]]])

(defui Main
  static om/IQuery
  (query [this]
         [:primary/view-selected])
  Object
  (render [this]
          (let [{:keys [primary/view-selected]
                 :or {primary/view-selected 0}} (om/props this)]
            (sab/html
             [:div#primary-container
              [:div#primary-mock
               [:div#map]
               [:div#key
                [:h3 "Key"]
                [:div#content
                 [:img {:src "/img/key.png"}]
                 [:div#legend
                  [:span "1.5"]
                  [:span "3.2"]]]]
               [:div#title
                [:h2 "Average Household Size"]
                [:h3 "2015, London | GLA"]]

               [:div#right-desc
                [:h2 "Brent"]
                [:div#content
                 [:div.info
                  [:span "Avg. Household Size"] [:i "3.78"]]
                 [:hr]
                 [:div.info
                  [:span "Area"] [:i "6.70 sq mi (43.24 km2)"]]
                 [:div.info
                  [:span "Popn."] [:i "320,762"]]
                 [:div.info
                  [:span "ONS#"] [:i "00AE"]]
                 [:div.info
                  [:span "PCs"] [:i "HA, NW, W"]]
                 [:div.info
                  [:span "AC"] [:i "020"]]
                 ]]
               [:div#slider
                [:div#text
                 [:span "2011"]
                 [:span "2041"]]
                (icons/slider :medium)]
               [:div#selector
                [:form.pure-form
                 [:select
                  [:option "Choropleth"]]]
                [:button.pure-button
                 (icons/download :small)]
                [:button.pure-button
                 (icons/share :small)]
                [:button.pure-button
                 (icons/printer :small)]]]
              [:div#overlay
               (switcher {:icon-0 (partial icons/topology :dark :medium)
                          :icon-1 (partial icons/visualisation :dark :medium)
                          :selected-idx view-selected
                          :on-select #(om/transact! this `[(change/primary-view! {:idx ~%})])})]]))))

(def primary-split-view (om/factory Main))
