(ns ^:figwheel-always witan.ui.fixtures.login.view-model
  (:require [om.core :as om :include-macros true]
            [venue.core :as venue]
            [witan.ui.services.data :as data]
            [witan.ui.strings :as s]
            [witan.ui.util :as util])
  (:require-macros [cljs-log.core :as log]
                   [witan.ui.macros :as wm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-activate
  [owner {:keys [username token]} cursor]
  (let [frag (.. js/document -location -hash)]
    (when (and username token (re-find #"^[#/]*password-reset" frag))
      (if (data/logged-in?)
        (venue/request! {:owner owner
                         :service :service/api
                         :args {:next-url (.. js/document -location -href)}
                         :request :logout})
        (do
          (log/info "Initiating password reset...")
          (om/update! cursor :reset-args {:password-reset-token token :username username})
          (om/update! cursor :phase :reset-redeem)))))
  (om/update! cursor :logged-in? (data/logged-in?)))

(wm/create-standard-view-model! {:on-activate on-activate})

(defn password-strong?
  [password]
  (>= (count password) 8))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod event-handler
  :event/reset-password
  [owner _ email cursor]
  (venue/request! {:owner owner
                   :service :service/api
                   :request :start-password-reset
                   :args email
                   :context cursor}))

(defmethod event-handler
  :event/attempt-reset-redeem
  [owner _ {:keys [password] :as args} cursor]
  (let [[password confirm-password] password]
    (if-not (= password confirm-password)
      (om/update! cursor :message (s/get-string :password-no-match))
      (if-not (password-strong? password)
        (om/update! cursor :message (s/get-string :password-under-length))
        (do
          (om/update! cursor :phase :waiting)
          (om/update! cursor :waiting-msg :changing-password)
          (venue/request! {:owner owner
                           :service :service/api
                           :request :complete-password-reset
                           :args (merge
                                  {:password password}
                                  (:reset-args @cursor))
                           :context cursor}))))))

(defmethod event-handler
  :event/goto-login
  [owner _ _ cursor]
  (om/update! cursor :phase :prompt))

(defmethod event-handler
  :event/goto-password-reset
  [owner _ _ cursor]
  (om/update! cursor :phase :reset))

(defmethod event-handler
  :event/goto-sign-up
  [owner _ _ cursor]
  (om/update! cursor :phase :sign-up))

(defmethod event-handler
  :event/attempt-login
  [owner _ {:keys [email pass]} cursor]
  (om/update! cursor :phase :waiting)
  (om/update! cursor :waiting-msg :signing-in)
  (om/update! cursor :email email)
  (venue/request! {:owner owner
                   :service :service/api
                   :request :login
                   :args [email pass]
                   :context cursor}))

(defmethod event-handler
  :event/attempt-sign-up
  [owner _ {:keys [email password] :as args} cursor]
  (let [[email confirm-email]       email
        [password confirm-password] password]
    (if-not (= email confirm-email)
      (om/update! cursor :message (s/get-string :email-no-match))
      (if-not (= password confirm-password)
        (om/update! cursor :message (s/get-string :password-no-match))
        (if-not (password-strong? password)
          (om/update! cursor :message (s/get-string :password-under-length))
          (do
            (om/update! cursor :phase :waiting)
            (om/update! cursor :waiting-msg :processing-account)
            (venue/request! {:owner owner
                             :service :service/api
                             :request :sign-up
                             :args (merge {:username email :password password}
                                          (select-keys args [:invite-token :name]))
                             :context cursor})))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod response-handler
  [:login :success]
  [owner _ response cursor]
  (om/update! cursor :phase :prompt)
  (if response
    (do
      (om/update! cursor :message nil)
      (om/update! cursor :logged-in? true))
    (om/update! cursor :message (s/get-string :sign-in-failure))))

(defmethod response-handler
  [:login :failure]
  [owner _ response cursor]
  (om/update! cursor :message (s/get-string :api-failure))
  (om/update! cursor :phase :prompt))

(defmethod response-handler
  [:sign-up :failure]
  [owner _ response cursor]
  (om/update! cursor :message (s/get-string :sign-up-failure))
  (om/update! cursor :phase :sign-up))

(defmethod response-handler
  [:sign-up :success]
  [owner _ response cursor]
  (om/update! cursor :phase :signed-up))

(defmethod response-handler
  [:start-password-reset :failure]
  [owner _ response cursor])

(defmethod response-handler
  [:start-password-reset :success]
  [owner _ response cursor])

(defmethod response-handler
  [:complete-password-reset :failure]
  [owner _ response cursor]
  (om/update! cursor :message (s/get-string :api-failure))
  (om/update! cursor :phase :reset-redeem))

(defmethod response-handler
  [:complete-password-reset :success]
  [owner _ response cursor]
  (venue/request! {:owner owner
                   :service :service/api
                   :request :logout}))

(defmethod response-handler
  [:logout :success]
  [owner _ response cursor]
  (let [login-div (.getElementById js/document "login")]
    (aset login-div "style" "visibility" "visible"))
  (venue/reactivate!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(util/inline-subscribe!
 :api/user-logged-in
 #(let [login-div (.getElementById js/document "login")]
    (aset login-div "style" "visibility" "hidden")))
