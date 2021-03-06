(ns witan.ui.services.api
  (:require [ajax.core :as ajax]
            [cljs.core.async :refer [put! take! chan <! close!]]
            [venue.core :as venue]
            [goog.net.cookies :as cookies]
            [witan.ui.util :as util])
  (:require-macros [cljs-log.core :as log]
                   [cljs.core.async.macros :refer [go]]
                   [witan.ui.env :as env :refer [cljs-env]]))

(def ^:private api-token (atom nil))
(def token-name "tkn")

(defn save-token!
  [token]
  (reset! api-token token)
  (.set goog.net.cookies token-name token -1))

(defn logout!
  ([]
   (logout! "/"))
  ([next-url]
   (log/info "Logging out...")
   (save-token! nil)
   (venue/publish! :api/user-logged-out)
   (set! (.. js/document -location -href) next-url)))

(defmulti response-handler
  (fn [result response cursor] result))

(defmulti service-m
  (fn [event args result-ch] event))

(defmulti api-response
  (fn [event-status response] event-status))

(defn local-endpoint
  [method]
  (let [api-url (cljs-env :witan-api-url)] ;; 'nil' is a valid api-url (will default to current hostname)
    (str api-url "/api" method)))

(defn- handle-response
  [status event result-ch response]
  (when (and (= status :failure) (not= event :token-test))
    (log/severe "An API error occurred: " status event response)
    (when (and @api-token (= (:status response) 401))
      (log/info "Logging out due to 401.")
      (logout!)))
  (let [result (api-response [event status] (clojure.walk/keywordize-keys response))]
    (when result-ch
      (put! result-ch [status result]))))

(defn upload
  [event method params result-ch]
  (log/debug "POST (upload) " method params)
  (let [form-data (js/FormData.)]
    (doseq [[k v] params]
      (.append form-data (name k) v))
    (ajax/POST (local-endpoint method)
               {:params form-data
                :handler (partial handle-response :success event result-ch)
                :error-handler (partial handle-response :failure event result-ch)
                :headers {"Authorization" (str "Token " @api-token)}})))

(defn POST
  [event method params result-ch]
  (log/debug "POST" method params)
  (ajax/POST (local-endpoint method)
             {:params params
              :handler (partial handle-response :success event result-ch)
              :error-handler (partial handle-response :failure event result-ch)
              :format :json
              :headers {"Authorization" (str "Token " @api-token)}}))

(defn GET
  [event method params result-ch]
  (log/debug "GET" method params)
  (ajax/GET (local-endpoint method)
            {:params params
             :handler (partial handle-response :success event result-ch)
             :error-handler (partial handle-response :failure event result-ch)
             :format :json
             :headers {"Authorization" (str "Token " @api-token)}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-initialise
  []
  (if-let [token (.get goog.net.cookies token-name)]
    (do
      (reset! api-token token)
      (GET :token-test "/" nil nil))
    (log/debug "No existing token was found.")))

(defn request-handler
  [event args result-ch]
  (if (or (contains? #{:login
                       :logout
                       :sign-up
                       :start-password-reset
                       :complete-password-reset} event) @api-token)
    (service-m event args result-ch)
    (do
      (log/warn "An API request was received but there is no token so the outbound call will not be made and we'll log out..." event )
      (logout!)
      (put! result-ch [:failure :no-token]))))

(defn service
  []
  (reify
    venue/IHandleRequest
    (handle-request [owner request args response-ch]
      (request-handler request args response-ch))
    venue/IHandleResponse
    (handle-response [owner outcome event response cursor]
      (response-handler [event outcome] response cursor))
    venue/IInitialise
    (initialise [owner _]
      (on-initialise))))

(defmethod service-m
  :login
  [event [email pass] result-ch]
  (POST event "/login" {:username email :password pass} result-ch))

(defmethod service-m
  :get-forecasts
  [event _ result-ch]
  (GET event "/forecasts" nil result-ch))

(defmethod service-m
  :get-forecast-versions
  [event id result-ch]
  (GET event (util/str-fmt-map "/forecasts/{{id}}" {:id id}) nil result-ch))

(defmethod service-m
  :logout
  [event {:keys [next-url] :or {next-url "/"}} result-ch]
  (logout! next-url)
  (put! result-ch [:success nil]))

(defmethod service-m
  :get-models
  [event id result-ch]
  (GET event "/models" nil result-ch))

(defmethod service-m
  :get-forecast
  [event {:keys [id version]} result-ch]
  (GET event (util/str-fmt-map "/forecasts/{{id}}/{{version}}" {:id id :version version}) nil result-ch))

(defmethod service-m
  :create-forecast
  [event args result-ch]
  (POST event "/forecasts" args result-ch))

(defmethod service-m
  :create-forecast-version
  [event forecast result-ch]
  (let [inputs (hash-map :inputs (into {} (map (fn [{:keys [category selected]}]
                                                 (let [selected-req (select-keys selected [:data-id])]
                                                   (when (not-empty selected-req)
                                                     (hash-map category selected-req))))
                                               (:forecast/inputs forecast))))]
    (POST event (util/str-fmt-map "/forecasts/{{id}}/versions" {:id (:forecast/forecast-id forecast)}) inputs result-ch)))

(defmethod service-m
  :get-model
  [event {:keys [id]} result-ch]
  (GET event (util/str-fmt-map "/models/{{id}}" {:id id}) nil result-ch))

(defmethod service-m
  :get-data-items
  [event category result-ch]
  (GET event (util/str-fmt-map "/data/{{category}}" {:category category}) nil result-ch))

(defmethod service-m
  :create-data-item
  [event {:keys [id version category] :as args} result-ch]
  (POST event (util/str-fmt-map "/forecasts/{{id}}/{{version}}/input/{{category}}" args) (dissoc args :id :version :category) result-ch))

(defmethod service-m
  :upload-data
  [event args result-ch]
  (upload event "/data" args result-ch))

(defmethod service-m
  :download-file
  [event url result-ch]
  (let [scrubbed-url (if-let [remove (let [idx (.indexOf url "?")]
                                       (when (>= idx 0)
                                         (.substring url idx)))]
                       (.replace url remove "") url)
        new-url (str scrubbed-url "?redirect=false")]
    (GET event new-url nil result-ch)))

(defmethod service-m
  :sign-up
  [event args result-ch]
  (POST event "/user" args result-ch))

(defmethod service-m
  :start-password-reset
  [event email result-ch]
  (POST event "/request-password-reset" {:username email} result-ch))

(defmethod service-m
  :complete-password-reset
  [event args result-ch]
  (POST event "/complete-password-reset" args result-ch))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- identify-user
  []
  (let [result-ch (chan)]
    (go
      (GET :post-login-get-user "/me" nil result-ch)
      (let [[result response] (<! result-ch)]
        (if (= result :success)
          ;; id success
          (do
            (log/info "User was identified:" (:name response) " - " (:username response))
            (venue/publish! :api/user-identified response))
          ;; id fail
          (do
            (log/severe "Failed to identify this user - logging out...")
            (logout!)))))))

(defn- login!
  [response]
  (if-let [token (:token response)]
    ;; login success
    (do
      (log/info "Login success.")
      (save-token! token)
      (venue/publish! :api/user-logged-in {:id (:id response)})
      (identify-user)
      true)
    ;; login fail
    (do
      (log/info "Login failed.")
      (log/debug "Response:" response)
      false)))

(defmethod api-response
  [:login :success]
  [_ response]
  (login! response))

(defmethod api-response
  [:sign-up :success]
  [_ response]
  (login! response))

(defmethod api-response
  [:token-test :success]
  [_ response]
  (login! {:token @api-token}))

(defmethod api-response
  :default
  [event response] response)
