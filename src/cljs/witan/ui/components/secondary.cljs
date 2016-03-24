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
                [:h4 "Average Household Size Projection Model"]
                [:div#content
                 [:form.pure-form.pure-form-stacked
                  [:field-set
                   [:legend "Adjustments to the model will cause any visualisations to refresh"]
                   [:div.pure-g
                    [:div.pure-u-md-1-3
                     [:label {:for "assumption"} "Migration Assumption"]
                     [:select.pure-u-23-24 {:id "assumption"}
                      [:option "High"]]]
                    [:div.pure-u-md-1-3
                     [:label {:for "gva-range"} "GVA% (50%)"]
                     [:input.pure-u-23-24 {:id "gva-range" :type "range"}]]
                    [:div.pure-u-md-1-6
                     [:label {:for "ltrr"} "Long-term Rotation Rate"]
                     [:select.pure-u-23-24 {:id "ltrr"}
                      [:option "10% - 50%"]]]
                    [:div.pure-u-md-1-6
                     [:label {:for "lm"} "Latent Mortalilty"]
                     [:select.pure-u-23-24 {:id "lm"}
                      [:option "> 65 yrs"]]]]]]]]]]))))

(def secondary-split-view (om/factory Main))
