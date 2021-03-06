(ns ^:figwheel-always witan.ui.fixtures.forecast.view
    (:require [om.core :as om :include-macros true]
              [om-tools.dom :as dom :include-macros true]
              [om-tools.core :refer-macros [defcomponent defcomponentmethod]]
              [sablono.core :as html :refer-macros [html]]
              [inflections.core :as i]
              ;;
              [witan.ui.util :as util]
              [witan.ui.fixtures.forecast.input-view]
              [witan.ui.fixtures.forecast.output-view]
              [witan.ui.widgets :as widgets]
              [witan.ui.strings :refer [get-string]]
              [venue.core :as venue])
    (:require-macros [cljs-log.core :as log]))

(def valid-actions ;; NOTE keep them in this order or weird stuff will 'appen.
  {:input  [nil    :model ]
   :model  [:input :output]
   :output [:model nil    ]})

(def action-strings
  {:input  {:title (-> :input get-string i/plural)
            :desc  (get-string :pw-input-brief)
            :about (get-string :input-intro)}
   :model  {:title (-> :model get-string)
            :desc (get-string :pw-model-brief)
            :about (get-string :model-intro)}
   :output {:title (-> :output get-string i/plural)
            :desc (get-string :pw-output-brief)
            :about (get-string :output-intro)}})

;; There's probably a more elegant way to do this
(defn next-action
  [kaction]
  (-> valid-actions
      (get kaction)
      second))

(defn previous-action
  [kaction]
  (-> valid-actions
      (get kaction)
      first))

(defn model-name-to-display
  [model name]
  (some #(when (= name (:name %)) (:display %)) (:model/properties model)))

(defcomponent
  header
  [{:keys [forecast/name forecast/version forecast/version-id forecast/in-progress? forecast/public? forecast/error edited? old?]} owner]
  (render [_]
          (let [new? (zero? version)]
            (html
             [:div.pure-menu.pure-menu-horizontal.witan-pw-header
              [:div.witan-page-heading
               [:h1 (get-string :forecast " /")]
               [:h1 name]
               [:em {:class (when new? "version-zero") :key "witan-pw-version"}
                (get-string :forecast-version " " version)]
               [:div.labels
                {:key "witan-pw-header-labels"}
                (when in-progress?
                  [:span.label.label-in-progress.label-small (get-string :in-progress " ")
                   [:i.material-icons.md-s.anim-spin "settings"]])
                (when error
                  [:span.label.label-error.label-small (get-string :error " ")])
                (when new?
                  [:span.label.label-new.label-small (get-string :new " ")
                   [:i.material-icons.md-s "star"]])
                (when edited?
                  [:span.label.label-forecast-changed.label-small (get-string :changed " ")
                   [:i.material-icons.md-s "flash_on"]])
                (when old?
                  [:span.label.label-forecast-superseded.label-small (get-string :superseded " ")
                   [:i.material-icons.md-s "report_problem"]])
                (when public?
                  [:span.label.label-forecast-public.label-small (get-string :public " ")
                   [:i.material-icons.md-s "people"]])]]]))))

(defcomponent
  forecast-box
  [{:keys [number text action active? id version]} owner]
  (render [_]
          (html
           [:div.witan-pw-forecast-nav-box
            {:class (str (name action) (when active? " active"))
             :on-click #(do
                          (venue/navigate! :views/forecast {:id id :version version :action (name action)} {:no-history true})
                          (.preventDefault %))}
            [:div.icon
             [:h2 (condp = action
                    :input [:i.material-icons.md-l "play_for_work"]
                    :model [:i.material-icons.md-l "reorder"]
                    :output [:i.material-icons.md-l "assessment"])]]
            [:div.action
             [:h2 (-> action-strings action :title)]]
            [:div.text
             [:h3 text]]])))

(defcomponent
  forecast-nav
  [{:keys [action id version]} owner]
  (render [_]
          (html
           [:div#witan-pw-forecast-nav
            (for [[number faction] [[1 :input] [2 :model] [3 :output]]]
              (om/build forecast-box {:text (-> action-strings faction :desc)
                                      :number number
                                      :action faction
                                      :active? (= faction  action)
                                      :id id
                                      :version version} {:key :number}))])))

(defmulti action-view
  (fn [[action cursor] owner] action))

(defcomponentmethod action-view
  :input
  [[action cursor] owner]
  (render [_]
          (html [:div#witan-pw-action-body-container
                 (om/build witan.ui.fixtures.forecast.input-view/view [action cursor])])))

(defcomponentmethod action-view
  :output
  [[action cursor] owner]
  (render [_]
          (html [:div#witan-pw-action-body-container
                 (om/build witan.ui.fixtures.forecast.output-view/view [action cursor])])))

(defcomponentmethod action-view
  :model
  [[action {:keys [forecast model]}] owner]
  (render [_]
          (html
           [:div#witan-pw-action-body-container
            {:key "model-container"}
            [:div.pure-g#witan-pw-action-body
             {:key "model-intro-body"}
             [:div.pure-u-1-2.text-right
              {:key "model-info-left"}
              [:div.padding-1
               {:key "model-info-left-inner"}
               [:h2
                {:key "model-title"}
                (get-string :model)]
               [:strong.model-value
                {:key "model-title-value"}
                (:model/name model)]
               [:h2
                {:key "model-desc"}
                (get-string :forecast-desc)]
               [:strong.model-value
                {:key "model-desc-value"
                 :dangerouslySetInnerHTML
                 {:__html (:model/description model)}}]
               (when (not-empty (:forecast/property-values forecast))
                 [:div
                  {:key "model-props"}
                  [:h2
                   {:key "model-props-title"}
                   (get-string :properties)]
                  (for [{:keys [name value]} (:forecast/property-values forecast)]
                    [:div {:key (str "model-prop-" name)}
                     [:strong.model-value.property {:key "name"}
                      (model-name-to-display model name) ": " [:span {:key "small" :style {:font-weight "normal"}} value]]])])]]
             [:div.pure-u-1-2
              {:key "model-info-right"}
              [:div.padding-1.text-left
               {:key "model-info-right-inner"
                :style {:border-left "1px solid silver"}}
               [:h2
                {:key "model-publisher"}
                (get-string :model-publisher)]
               [:strong.model-value
                {:key "model-publisher-value"}
                (:model/owner-name model)]
               [:h2 {:key "model-created"}
                (get-string :created)]
               [:strong.model-value {:key "model-created-value"}
                (-> model
                    :model/created
                    util/humanize-time)]
               [:h2 {:key "model-version"}
                (get-string :forecast-version)]
               [:strong.model-value
                {:key "model-version-value"} (:model/version model)]]]]])))

(defcomponent in-progress-message
  [cursor owner]
  (render [_]
          (html
           [:div.witan-pw-message-box#witan-pw-in-prog
            {:key "witan-pw-in-prog"}
            [:div.pure-u-1#witan-pw-in-prog-text
             {:key "witan-pw-in-prog-text"}
             [:span
              {:key "witan-pw-in-prog-text-span"}
              (get-string :forecast-in-progress-text)]
             [:button.pure-button#refresh
              {:key "witan-pw-in-prog-button-refresh"
               :on-click (fn [e]
                           (venue/raise! owner :refresh-forecast)
                           (.preventDefault e))}
              [:span
               [:i.material-icons {:key "witan-pw-in-prog-button-refresh-i"} "refresh"]
               [:span {:key "witan-pw-in-prog-button-refresh-span"}
                (str " " (get-string :refresh-now))]]]]])))

(defcomponent edited-forecast-message
  [cursor owner]
  (render [_]
          (html
           [:div.witan-pw-message-box#witan-pw-edits
            {:key "witan-pw-edits"}
            [:div.pure-u-1#witan-pw-edits-text
             {:key "witan-pw-edits-text"}
             [:span
              {:key "witan-pw-edits-text-span"}
              (get-string :forecast-changes-text)]
             [:button.pure-button#create
              {:key "witan-pw-edits-button-create"
               :on-click (fn [e]
                           (venue/raise! owner :create-forecast-version)
                           (.preventDefault e))}
              [:span
               [:i.material-icons {:key "witan-pw-edits-button-create-i"} "done"]
               [:span {:key "witan-pw-edits-button-create-span"}
                (str " " (get-string :create-new-forecast))]]]
             [:button.pure-button#revert
              {:key "witan-pw-edits-button-revert"
               :on-click (fn [e]
                           (venue/raise! owner :revert-forecast)
                           (.preventDefault e))}
              [:span
               [:i.material-icons {:key "witan-pw-edits-button-revert-i"} "undo"]
               [:span {:key "witan-pw-edits-button-revert-span"}
                (str " " (get-string :revert-forecast))]]]]])))

(defcomponent missing-required-message
  [cursor owner]
  (render [_]
          (html
           [:div.witan-pw-message-box#witan-pw-missing
            {:key "witan-pw-missing"}
            [:div.pure-u-1#witan-pw-missing-text
             {:key "witan-pw-missing-text"}
             [:span (get-string :missing-required-inputs)]]])))

(defcomponent view
  [{:keys [id action forecast version edited-forecast error? model creating? missing-required] :as cursor} owner]
  (render [_]
          (let [kaction      (keyword action)
                model-conf   {:action kaction
                              :n-inputs (-> model :model/input-data count)
                              :n-outputs [(-> model :model/output-data count)];; TODO add grps
                              :stage-names (map (fn [[k v]] (last v)) valid-actions)}
                next-action  (next-action kaction)
                prev-action  (previous-action kaction)
                in-progress? (:forecast/in-progress? forecast)]
            (html
             (if error?
               [:div
                [:h1 "Error"]
                [:h3 error?]]
               (if (or creating? (not (and forecast model)))
                 [:div.view-overlay
                  [:i.material-icons.md-dark.md-xl.anim-spin "settings"]
                  (when creating? [:h2 (get-string :creating-forecast)])]
                 [:div
                  [:div#witan-pw-header-container
                   {:key "witan-pw-header-container"}
                   (om/build header (assoc forecast
                                           :edited? (-> edited-forecast nil? not)
                                           :old?    (-> forecast :forecast/latest? not)))]
                  [:div.pure-g.witan-page-content#witan-pw-body
                   {:key "witan-pw-body"}
                   [:div#container

                    [:div.pure-u-1.witan-model-diagram
                     {:key "forecast-centre"}
                     (when forecast (om/build forecast-nav {:action kaction :id id :version version}))]

                    [:div.pure-u-1#witan-pw-body-content
                     [:div.pure-u-1-12]
                     [:div.pure-u-5-6..text-center#witan-pw-message
                      (if in-progress?
                        (om/build in-progress-message {})

                        ;; only show edited if not in-progress?
                        (if edited-forecast
                          (if (empty? missing-required)
                            (om/build edited-forecast-message {})
                            (om/build missing-required-message {}))))]
                     [:div.pure-u-1-12]
                     [:div.text-center#witan-pw-stage-desc
                      [:div.pure-u-1-2.text-left
                       [:h2 (-> action-strings kaction :desc)]
                       [:p (-> action-strings kaction :about)]]]]

                    [:div.text-center
                     {:key "witan-pw-area-container"}
                     (if-not (contains? (set (keys valid-actions)) kaction)
                       [:div.pure-u-1 [:span "Unknown forecast action"]]
                       [:div.pure-u-3-4#witan-pw-area
                        {:key "witan-pw-area"}
                        [:div
                         (om/build action-view [kaction cursor])]])]]]]))))))
