(ns witan.ui.components.login
  (:require [om.next :as om :refer-macros [defui]]
            [sablono.core :as sab]
            ;;
            [witan.ui.components.primary :as primary]
            [witan.ui.components.secondary :as secondary]
            [witan.ui.utils :as utils]
            [witan.ui.components.icons :as icons]
            [witan.ui.strings :refer [get-string]]
            ;;
            [witan.ui.controller :as controller])
  (:require-macros [cljs-log.core :as log]
                   [devcards.core :as dc :refer [defcard]]))

(defmulti login-state-view
  (fn [phase owner data] phase))

(defmethod
  login-state-view
  :waiting
  [_ owner data]
  [:div
   [:h3 (get-string :string/waiting-msg)]
   [:div#loading
    [:i.material-icons.anim-spin "settings"]]])

(defmethod
  login-state-view
  :sign-up
  [_ owner data]
  [:div.sub-page-div
   [:h3 (get-string :string/create-account)]
   [:span#error-message (:message data)]
   [:form {:class "pure-form pure-form-stacked"
           :on-submit (fn [e]
                        #_(venue/raise! owner :event/attempt-sign-up {:email [(.-value (om/get-node owner "email"))
                                                                              (.-value (om/get-node owner "confirm-email"))]
                                                                      :password [(.-value (om/get-node owner "password"))
                                                                                 (.-value (om/get-node owner "confirm-password"))]
                                                                      :invite-token (.-value (om/get-node owner "token"))
                                                                      :name (.-value (om/get-node owner "name"))})
                        (.preventDefault e))}
    [:input {:tab-index 1
             :ref "token"
             :type "text"
             :id "token"
             :placeholder (get-string :string/sign-up-token)
             :required :required}]
    [:input {:tab-index 2
             :ref "name"
             :type "text"
             :id "name"
             :placeholder (get-string :string/name)
             :required :required}]
    [:input {:tab-index 3
             :ref "email"
             :type "email"
             :id "login-email"
             :placeholder (get-string :string/email)
             :required :required}]
    [:input {:tab-index 4
             :ref "confirm-email"
             :type "email"
             :id "confirm-email"
             :placeholder (get-string :string/confirm-email)
             :required :required}]
    [:input {:tab-index 5
             :ref "password"
             :type "password"
             :id "password"
             :placeholder (get-string :string/password)
             :required :required}]
    [:input {:tab-index 6
             :ref "confirm-password"
             :type "password"
             :id "confirm-password"
             :placeholder (get-string :string/confirm-password)
             :require :required}]
    [:div [:button {:tab-index 7
                    :type "submit"
                    :class "pure-button pure-button-primary"} (get-string :string/create-account)]
     [:button {:id "back-button"
               :class "pure-button"
               :on-click (fn [e]
                           (om/transact! owner '[(login/goto-phase! {:phase :prompt}) :login/phase])
                           (.preventDefault e))} (get-string :string/back)]]]])

(defmethod
  login-state-view
  :prompt
  [_ owner {:keys [message]}]
  [:div
   [:h3 (get-string :string/sign-in)]
   (when message
     [:span#error-message (get-string message)])
   [:form {:class "pure-form pure-form-stacked"
           :on-submit (fn [e]
                        (om/transact! owner '[(login/set-message! {:message nil})])
                        (controller/raise! owner :user/login {:email (.-value (. js/document (getElementById "login-email")))
                                                              :pass (.-value (. js/document (getElementById "login-password")))})
                        (.preventDefault e))}
    [:input {:tab-index 1
             :ref "email"
             :type "email"
             :id "login-email"
             :placeholder (get-string :string/email)
             :required :required}]
    [:input {:tab-index 2
             :ref "password"
             :type "password"
             :id "login-password"
             :placeholder (get-string :string/password)
             :require :required
             :pattern ".{8,}"
             :title "8 characters minimum"}]
    [:button {:tab-index 3
              :type "submit"
              :class "pure-button pure-button-primary"} (get-string :string/sign-in)]
    [:a {:id "forgotten-link"
         :on-click (fn [e]
                     (om/transact! owner '[(login/goto-phase! {:phase :reset}) :login/phase])
                     (.preventDefault e))} (str "(" (get-string :string/forgotten-question) ")")]]
   [:h3 (get-string :string/create-account-header)]
   [:p
    [:span.text-white (get-string :string/create-account-info)]]
   [:button.pure-button.pure-button-success
    {:on-click (fn [e]
                 (om/transact! owner '[(login/goto-phase! {:phase :sign-up}) :login/phase])
                 (.preventDefault e))} (get-string :string/create-account)]])

(defmethod
  login-state-view
  :reset
  [_ owner data]
  [:div.sub-page-div
   [:h3 (get-string :string/forgotten-password)]
   [:p
    [:span {:id "reset-instructions"} (get-string :string/forgotten-instruction)]]
   [:form {:class "pure-form"
           :on-submit (fn [e]
                        (comment (set! (.-innerText (. js/document (getElementById "reset-instructions"))) (get-string :string/reset-submitted))
                                 (set! (.-innerText (. js/document (getElementById "reset-button"))) (get-string :string/thanks))
                                 (set! (.-disabled (. js/document (getElementById "reset-button"))) true)
                                 (set! (.-disabled (. js/document (getElementById "reset-input"))) true))
                        (.open js/window
                               (str
                                "mailto:witan@mastodonc.com?subject=[Witan Password Reset Request]"
                                "&body=Please reset the password for the following email address: "
                                (.-value (.getElementById js/document "reset-input"))) "resetEmailWindow" "height=400,width=600,left=10,top=10")
                        #_(venue/raise! owner :event/reset-password (.-value (om/get-node owner "reset-email")))
                        (.preventDefault e))}
    [:input {:tab-index 1
             :ref "reset-email"
             :id "reset-input"
             :type "email"
             :placeholder (get-string :string/email)
             :required :required}]
    [:div
     [:button {:tab-index 2
               :id "reset-button"
               :class "pure-button pure-button-primary"} (get-string :string/reset-password)]
     [:button {:id "back-button"
               :class "pure-button"
               :on-click (fn [e]
                           (om/transact! owner '[(login/goto-phase! {:phase :prompt}) :login/phase])
                           (.preventDefault e))} (get-string :string/back)]]]])


(defui Main
  static om/IQuery
  (query [this]
         [{:app/login [:login/phase :login/success? :login/message]}])
  Object
  (render [this]
          (let [{:keys [login/phase login/message]} (get-in (om/props this) [:app/login])]
            (sab/html [:div
                       [:div#login-bg {:key "login-bg"}
                        [:span#bg-attribution.trans-bg
                         "Photo by "
                         [:a {:href "https://www.flickr.com/photos/fico86/" :target "_blank" :key "photo-attr1"}
                          "Binayak Dasgupta"] " - "
                         [:a {:href "https://creativecommons.org/licenses/by/2.0/" :target "_blank" :key "photo-attr2"} "CC BY 2.0"]]]
                       [:div#content-container {:key "login-content"}
                        [:div#relative-container
                         [:div.login-title.trans-bg {:key "login-title"}
                          [:h1 {:key "login-title-main"} (get-string :string/witan) ]
                          [:h2 {:key "login-title-sub"} (get-string :string/witan-tagline)]]
                         [:div#witan-login.trans-bg {:key "login-state"}
                          (login-state-view phase this {:message message})]]]]))))
