(ns witan.ui.fixtures.forecast.input-view
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent defcomponentmethod]]
            [sablono.core :as html :refer-macros [html]]
            [inflections.core :as i]
            [clojure.string :as str]
            ;;
            [witan.ui.util :as util]
            [witan.ui.widgets :as widgets]
            [witan.ui.strings :refer [get-string]]
            [venue.core :as venue])
  (:require-macros [cljs-log.core :as log]))


;; header
(def header-select-width  "3%")
(def header-name-width    "auto")
(def header-version-width "10%")
(def header-lm-width      "21%")

;; row
(def row-select-width  "3%")
(def row-name-width    "auto")
(def row-version-width "10%")
(def row-lm-width      "20%")

;; browser
(def browser-height "375px")

(defcomponent
  upload-widget
  [{:keys [cursor browsing?]} owner]
  (render [_]
          (html
           (let [{:keys [upload-filename
                         upload-file
                         upload-type
                         uploading?
                         upload-error?
                         upload-success?
                         upload-feedback
                         last-upload-filename
                         data-items]} cursor
                         public-forecast? (-> @cursor :forecast :forecast/public?)]
             [:div.container
              [:h3 {:key "subtitle"}
               (get-string :upload-new-data)]
              (cond

                ;;;;;;;;;;;;;;;
                ;; ERROR MESSAGE
                ;;;;;;;;;;;;;;;
                upload-error?
                [:div
                 {:key "upload-error"}
                 [:h4
                  {:key "upload-error-title"}
                  (get-string :browser-upload-error)]
                 [:h4.text-error
                  (when-not (str/blank? upload-feedback) upload-feedback)]
                 [:button.pure-button.button-warning
                  {:key "upload-error-reset"
                   :on-click (fn [e]
                               (venue/raise! owner :error-reset)
                               (.preventDefault e))}
                  (get-string :back)]]

                ;;;;;;;;;;;;;;;
                ;; UPLOADING SPINNER
                ;;;;;;;;;;;;;;;
                uploading?
                [:div
                 {:key "uploading"
                  :style {:text-align :center}}
                 [:h4
                  {:key "uploading-title"}
                  (get-string :browser-upload-completes)]
                 [:div
                  {:key "uploading-spinner"}
                  [:i.material-icons.md-xl.anim-spin "settings"]]]

                ;;;;;;;;;;;;;;;
                ;; FORM
                ;;;;;;;;;;;;;;;
                :else
                [:div
                 {:key "upload-form-container"}
                 [:div
                  {:key "upload-form"}
                  [:button.pure-button.button-secondary
                   {:key "button"
                    :style {:margin "0" :padding "0" :height "2em"}
                    :on-click (fn [e]
                                (.click (.getElementById js/document "upload-filename"))
                                (.preventDefault e))}
                   [:label {:for "upload-filename" :style {:padding "2em"}}
                    [:span (get-string :choose-file)]]]
                  [:input.hidden-file-input {:key "input"
                                             :id "upload-filename"
                                             :type "file"
                                             :on-change #(venue/raise! owner
                                                                       :pending-upload
                                                                       (first (array-seq (.. % -target -files))))}]
                  [:div
                   {:key "filename"}
                   [:small (if upload-file upload-filename (get-string :browser-no-file-selected))]]]

                 (let [data-item-names (->> data-items (map :data/name) (set))
                       [upload-type lock-selector] (if (empty? data-item-names) [:new true] [upload-type false])]
                   [:form.pure-form.pure-form-stacked
                    {:key "upload-form-submit"
                     :on-submit (fn [e]
                                  (let [node (om/get-node owner "upload-data-name")
                                        idx (.-selectedIndex node) ;; if it has a selectedIndex it's a select input
                                        name (if idx (.-value (aget (.-options node) idx)) (.-value node))
                                        result {:name name
                                                :public? (if public-forecast?
                                                           true
                                                           (if idx
                                                             (boolean (some #(when (= (:data/name %) name) (:data/public? %)) data-items))
                                                             (.-checked (om/get-node owner "upload-data-public"))))}]
                                    (venue/raise! owner :upload-file result))
                                  (.preventDefault e))}

                    ;;;;;;;;;;;;;;;
                    ;; UPLOAD TYPE SELECTOR
                    ;;;;;;;;;;;;;;;
                    [:select {:key "upload-select"
                              :disabled (if (or lock-selector (not upload-file)) "disabled")
                              :value upload-type
                              :on-change #(venue/raise! owner :pending-upload-type (.. % -target -value))}
                     [:option {:key "existing" :value "existing"} (get-string :browser-upload-option-existing) ]
                     [:option {:key "new" :value "new" } (get-string :browser-upload-option-new)]]

                    (cond
                      (and upload-file browsing?)
                      [:div
                       {:key "upload-options"}
                       (condp = upload-type
                         :existing [:div
                                    {:key "upload-options-div"}
                                    [:label {:key "label"} (get-string :browser-upload-select-existing)]
                                    [:select.full-width
                                     {:key "input":ref "upload-data-name"}
                                     (for [name data-item-names]
                                       [:option {:key (str "upload-data-option-" name) :value name} name])]]
                         :new      [:div
                                    {:key "upload-options-div"}
                                    [:label {:key "label"} (get-string :browser-upload-select-new)]
                                    [:input.full-width {:key "input"
                                                        :ref "upload-data-name"
                                                        :type "text"
                                                        :required true}]
                                    (if public-forecast?
                                      [:small [:strong (get-string :upload-data-public-warning)]]
                                      [:small {} [:input.pure-input {:type "checkbox"
                                                                     :ref "upload-data-public"}] " " (get-string :upload-data-public-explain)])])

                       [:button.pure-button.button-primary.upload-button
                        {:key "button"}
                        [:i.material-icons.md-s "file_upload"]
                        [:span (get-string :upload)]]]

                      ;;;;;;;;;;;;;;;
                      ;; SUCCESS MESSAGE
                      ;;;;;;;;;;;;;;;
                      upload-success?
                      [:div
                       {:key "upload-success"}
                       [:div.spacer
                        {:key "spacer"}]
                       [:p
                        {:key "paragraph"}
                        [:i.material-icons.md-l.text-success
                         {:key "upload-tick" :style {:margin-right "0.5em"}} "done"]
                        [:span {:key "label"} (get-string :upload-success ":" last-upload-filename)]]])])])]))))

(defcomponent
  input-browser
  [{:keys [cursor browsing?] :as state} owner]
  (render [_]
          (html
           (let [{:keys [data-items
                         selected-data-item]} cursor
                         has-selected? (contains? (set data-items) selected-data-item)]
             [:div.witan-pw-input-browser
              {:key "outer"}
              [:div.witan-pw-input-browser-content
               {:key "content"}
               [:strong
                {:key "title"}
                (get-string :browser-choose-data)]
               [:div.spacer
                {:key "spacer"}]

               ;;;;;;;;;;;;;;;
               ;; SEARCH
               ;;;;;;;;;;;;;;;
               [:div.pure-u-1.pure-u-md-3-5.witan-pw-input-browser-content-search
                {:key "search"}
                [:h3
                 {:key "subtitle"}
                 [:span
                  {:key "text"}
                  (str (get-string :search) " " (get-string :data-items))]
                 [:small
                  {:key "public-reminder"}
                  (when (-> cursor :forecast :forecast/public?) (str " (" (get-string :public-only) ")"))]]
                [:div
                 {:key "search-input-container"}
                 [:div.search-input
                  {:key "search-input"}
                  [:div.search-input-inner
                   {:style {:display "inline-block"} :key "search-input-form"}
                   (om/build widgets/search-input
                             (str (get-string :filter) " " (get-string :data-items))
                             {:opts {:on-input #(venue/raise! %1 :filter-data-items %2)}})]
                  [:button.pure-button.button-success
                   {:key "use-button"
                    :disabled (not has-selected?)
                    :on-click (fn [e]
                                (venue/raise! owner :select-input)
                                (.preventDefault e))}
                   [:i.material-icons.md-s
                    {:key "check"}
                    "done"]
                   [:span
                    {:key "text"}
                    (str " " (get-string :use-data-item))]]
                  (let [url       (:data/s3-url selected-data-item)
                        disabled? (or (not has-selected?) (not url))]
                    [:a
                     {:key "download-button"
                      :href (when-not disabled? url)}
                     [:button.pure-button.button-primary
                      {:disabled disabled?}
                      [:i.material-icons.md-s
                       {:key "download"}
                       "file_download"]
                      [:span
                       {:key "text"}
                       (str " " (get-string :download))]]])]]
                [:div.spacer
                 {:key "spacer"}]
                [:div.headings.pure-g
                 {:key "headings"}
                 [:span.pure-u-1-3
                  {:key "heading-name"}
                  (get-string :forecast-name)]
                 [:span.pure-u-1-4
                  {:key "heading-publisher"}
                  (get-string :model-publisher)]
                 [:span.pure-u-1-6
                  {:key "heading-version"}
                  (get-string :forecast-version)]
                 [:span.pure-u-1-4
                  {:key "heading-date"}
                  (get-string :created)]]
                [:div.list
                 {:key "data-item-list"}
                 (for [{:keys [data/data-id data/version data/name data/public? data/created data/publisher-name] :as data-item} data-items]
                   [:div.data-item.pure-g
                    {:key (str "data-item-" data-id "-" version)
                     :class (when (= data-item selected-data-item) "selected")
                     :on-click (fn [e]
                                 (venue/raise! owner :select-data-item data-item)
                                 (.preventDefault e))
                     :on-double-click (fn [e]
                                        (venue/raise! owner :select-input)
                                        (.preventDefault e))}
                    [:span.pure-u-1-3
                     {:key "data-item-name"}
                     (str name (when public? (str " (" (-> :public get-string str/lower-case) ")")))]
                    [:span.pure-u-1-4
                     {:key "data-item-publisher"} publisher-name]
                    [:span.pure-u-1-6
                     {:key "data-item-version"} version]
                    [:span.pure-u-1-4
                     {:key "data-item-date"} (util/humanize-time created)]])]]

               ;;;;;;;;;;;;;;;
               ;; UPLOAD
               ;;;;;;;;;;;;;;;
               [:div.pure-u-1.pure-u-md-2-5.witan-pw-input-browser-content-upload
                {:key "upload"}
                (om/build upload-widget state)]]]))))

(defn hook-links
  [owner]
  (let [links (. js/document getElementsByTagName "a")]
    (doseq [i (range (.-length links))]
      (let [link (aget links i)
            url (.-href link)
            url-minus-host (str/replace url (.. js/document -location -origin) "")
            requires-auth? (= (.indexOf url-minus-host "/data/public") 0)]
        (when requires-auth?
          (log/debug "Hooking link: " link)
          (set! (.-onclick link)
                (fn [e]
                  (venue/raise! owner :download-file url-minus-host)
                  (.preventDefault e))))))))

(defcomponent
  data-item-input-table-row
  [{:keys [locked? data-item default? input browsing? forecast]} owner]
  (did-mount [_]
             (hook-links owner))
  (did-update [_ _ _]
              (hook-links owner))
  (render [_]
          (let [processed-item (util/map-remove-ns data-item)
                {:keys [name version created edited?]} processed-item
                key-prefix (partial str (i/hyphenate name) "-")
                prep-property-values (->> (:forecast/property-values forecast)
                                          (map #((juxt :name :value) %))
                                          (map #(update % 0 keyword))
                                          (into {}))]
            (html
             [:table.pure-table.pure-table-horizontal.full-width
              {:key (key-prefix "table-body")}
              [:tbody.witan-pw-input-data-row.text-left
               [:td {:key (key-prefix "name") :style {:width row-name-width}}
                [:strong.category
                 {:key (str (key-prefix "name-") "category")}
                 (-> input :category i/capitalize)]
                [:span.description
                 {:key (str (key-prefix "name-") "description")
                  :dangerouslySetInnerHTML
                  {:__html (or (util/str-fmt-map (:description input)
                                                 {:model-properties prep-property-values}) (get-string :no-description-provided))}}]
                [:div
                 {:key (str (key-prefix "name-") "button")}
                 [:button.pure-button.witan-pw-browse-toggle
                  {:on-click (fn [e]
                               (venue/raise! owner :toggle-browse-input input)
                               (.preventDefault e))
                   :disabled locked?}
                  (cond
                    locked?   [:i.material-icons "lock"]
                    browsing? [:i.material-icons "arrow_drop_up"]
                    :else     [:i.material-icons "arrow_drop_down"])]
                 [:div
                  {:class (if edited? "edited" (when-not data-item "not-specified"))}
                  (or name [:i (get-string :no-input-specified)])
                  (cond
                    (and name default?) [:small.text-gray (get-string :default-brackets)]
                    (not name) [:small.text-gray
                                (get-string :please-select-data-input)]
                    :else [:small (get-string :forecast-version " " version)])]]]
               [:td {:key (key-prefix "version") :style {:width row-version-width} :class (when edited? "edited")} version]
               [:td {:key (key-prefix "lastmodified") :style {:width row-lm-width} :class (when edited? "edited")} (util/humanize-time created)]]]))))

(defcomponent
  data-item-input-table
  [{:keys [input top? browsing-input cursor locked?]} owner]
  (render [_]
          (let [category (:category input)
                prefix (partial str category)
                browsing? (= browsing-input input)]
            (html
             [:div
              {:key (prefix "-input-div")}
              [:div.pure-u-4-5
               {:key (prefix "-input-table-container")}
               [:table.pure-table.pure-table-horizontal.full-width
                {:key (prefix "-input-table")}
                [:thead
                 {:key (prefix "-input-table-head")}
                 [:th {:key (prefix "-input-collapser") :style {:width header-select-width}}] ;; empty, for the tree icon
                 [:th {:key (prefix "-input-name") :style {:width header-name-width}}]
                 [:th {:key (prefix "-input-version") :style {:width header-version-width}} (when top? (get-string :forecast-version))]
                 [:th {:key (prefix "-input-lastmodified") :style {:width header-lm-width}} (when top? (get-string :forecast-lastmodified))]]]
               [:hr {:key (prefix "-input-hr")}]
               (om/build data-item-input-table-row
                         {:data-item (or (:selected input) (:default input))
                          :default? (nil? (:selected input))
                          :input input
                          :browsing? browsing?
                          :locked? locked?
                          :forecast (:forecast cursor)} {:key :name})]

              [:div.pure-u-1.witan-pw-input-browser-container
               {:style {:height (if browsing? browser-height "0px")}
                :key (prefix "-input-browser-container")}
               (om/build input-browser {:cursor cursor :browsing? browsing?})]]))))

(defcomponent view
  [[action {:keys [edited-forecast forecast model browsing-input] :as cursor}] owner]
  (render [_]
          (html
           (let [current-forecast-inputs (or (:forecast/inputs edited-forecast)
                                             (:forecast/inputs forecast))
                 model-inputs            (sort-by :category (:model/input-data model))
                 squashed-inputs         (sort-by :category
                                                  (util/squash-maps (:model/input-data model) current-forecast-inputs :category))
                 inputs                  (map #(assoc %1 :description (:description %2)) squashed-inputs model-inputs)
                 first-input             (first inputs)
                 rest-inputs             (rest inputs)
                 locked?                 (or (-> cursor :forecast :forecast/in-progress?)
                                             (-> cursor :forecast :forecast/latest? not))]
             [:div#witan-pw-action-body

              ;; first row
              [:div
               {:key "input-first-row"}
               (om/build data-item-input-table {:input first-input
                                                :top? true
                                                :browsing-input browsing-input
                                                :cursor cursor
                                                :locked? locked?})]

              ;; other rows
              (for [input rest-inputs]
                [:div
                 {:key (str (:category input) "-input-row")}
                 (om/build data-item-input-table {:input input
                                                  :top? false
                                                  :browsing-input browsing-input
                                                  :cursor cursor
                                                  :locked? locked?})])]))))
