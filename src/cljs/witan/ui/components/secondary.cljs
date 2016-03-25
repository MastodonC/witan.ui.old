(ns witan.ui.components.secondary
  (:require [om.next :as om :refer-macros [defui]]
            [sablono.core :as sab]
            [witan.ui.strings :refer [get-string]]))

(defn switcher
  [{:keys [titles selected-idx on-select]}]
  [:div.secondary-switcher
   (for [[idx title] (map-indexed vector titles)]
     [:button.pure-button
      {:style {:width (str (/ 100 (count titles)) "%")}
       :class (when (= selected-idx idx) "selected")
       :on-click #(when on-select (on-select idx))}
      title])])

(defui Main
  static om/IQuery
  (query [this]
         [:secondary/view-selected])
  Object
  (render [this]
          (let [{:keys [secondary/view-selected]
                 :or {secondary/view-selected 0}} (om/props this)]
            (sab/html
             [:div#secondary-container
              [:div#switcher
               (switcher {:titles [(get-string :string/workspace-data-view)
                                   (get-string :string/workspace-config-view)
                                   (get-string :string/workspace-history-view)]
                          :selected-idx view-selected
                          :on-select #(om/transact! this `[(change/secondary-view! {:idx ~%})])})]
              [:div#secondary-mock
               [:div#settings-box
                [:h4 "Borough Projection Model"]
                [:div#content
                 [:form.pure-form.pure-form-stacked
                  [:field-set
                   [:legend "Adjustments to the model will cause visualisations to refresh"]
                   [:div.pure-g
                    [:div.pure-u-md-1-3
                     [:label {:for "a"} "Variant"]
                     [:select.pure-u-23-24 {:id "a"}
                      [:option "Capped Household Size"]]]
                    [:div.pure-u-md-1-3
                     [:label {:for "b"} "Cap Size (2.95)"]
                     [:input.pure-u-23-24 {:id "b" :type "range"}]]
                    [:div.pure-u-md-1-6
                     [:label {:for "c"} "Fertility Assumption"]
                     [:select.pure-u-23-24 {:id "c"}
                      [:option "High Fertility"]]]
                    [:div.pure-u-md-1-6
                     [:label {:for "d"} "Migration Assumption"]
                     [:select.pure-u-23-24 {:id "d"}
                      [:option "Low Migration"]]]]]]]]]]))))

(def secondary-split-view (om/factory Main))
