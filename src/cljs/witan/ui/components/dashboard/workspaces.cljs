(ns witan.ui.components.dashboard.workspaces
  (:require [witan.ui.components.shared  :as shared]
            [witan.ui.components.dashboard.shared  :as shared-dash]
            [witan.ui.utils   :as utils]
            [witan.ui.data :as data]
            [witan.ui.components.icons   :as icons]
            [witan.ui.route   :as route]
            [witan.ui.strings :refer [get-string]])
  (:require-macros [cljs-log.core :as log]))

(defmulti button-press
  (fn [_ event] event))

(defmethod button-press :view
  [selected-id _]
  (route/navigate! :app/workspace {:id selected-id}))

(defmethod button-press :create
  [_ _]
  (route/navigate! :app/create-workspace))

(defn view
  [this]
  (let [{:keys [wd/selected-id wd/workspaces]} this
        icon-fn #(vector :div.text-center (icons/workspace :dark))
        buttons (concat (when selected-id [{:id :view :icon icons/open :txt :string/view :class "workspace-view"}])
                        [{:id :create :icon icons/plus :txt :string/create :class "workspace-create"}])]
    [:div.dashboard
     (shared-dash/header {:title :string/workspace-dash-title
                          :filter-txt :string/workspace-dash-filter
                          :filter-fn nil
                          :buttons buttons
                          :on-button-click (partial button-press selected-id)})
     [:div.content
      (shared/table {:headers [{:content-fn icon-fn               :title ""              :weight 0.03}
                               {:content-fn :workspace/name       :title "Name"          :weight 0.57}
                               {:content-fn :workspace/owner-name :title "Owner"         :weight 0.2}
                               {:content-fn :workspace/modified   :title "Last Modified" :weight 0.2}]
                     :content workspaces
                     :selected?-fn #(= (:workspace/id %) selected-id)
                     :on-select #(data/transact! this 'wd/select-row! {:id (:workspace/id %)})
                     :on-double-click #(route/navigate! :app/workspace {:id (:workspace/id %)})})]]))
