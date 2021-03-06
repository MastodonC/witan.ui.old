(ns witan.ui.services.data
  (:require [cljs.core.async :refer [put! take! chan <! close!]]
            [witan.ui.util :as util]
            [venue.core :as venue]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [datascript :as d])
  (:require-macros [cljs-log.core :as log]
                   [witan.ui.macros :as wm]))

(defonce state (atom {:logged-in? false}))
(defonce db-schema {})
(defonce db-conn (d/create-conn db-schema))
(defonce id-lookup (atom {}))
(defonce id-counter (atom 0))


(defn reset-db!
  []
  (reset! db-conn (d/empty-db)))

(defn logged-in? [] (:logged-in? @state))

(defn find-or-add-lookup
  "We're looking for a :db/id stored for this id. If we don't find one, add one. Return the :db/id either way."
  [ns id lookup counter]
  (let [kid (util/add-ns ns (keyword id))]
    (if-let [existing-id (get @lookup kid)]
      existing-id
      (let [new-id (swap! counter inc)]
        (swap! lookup assoc kid new-id)
        new-id))))

(defn put-item-into-db!
  ([item ns]
   (put-item-into-db! item ns :version-id))
  ([item ns id-fn]
   (log/debug "Adding item to db: " ns item id-fn)
   (let [id (id-fn item)
         db-id (find-or-add-lookup ns id id-lookup id-counter)
         cleaned (->> item
                      (filter (fn [[k v]] (if (coll? v) (-> v empty? not) true)))
                      (util/map-add-ns ns))
         with-db-id (assoc cleaned :db/id db-id)]
     (d/transact! db-conn [with-db-id])
     with-db-id)))

(defn s3-key-from-url
  [url]
  (.substring url (inc (.lastIndexOf url "/"))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-filter-pred
  [filter]
  (fn [n] (if-not filter
            true
            (util/contains-str n filter))))

(defn build-descendant-list
  [id]
  (apply concat (d/q '[:find ?e
                       :in $ ?i
                       :where [?e :forecast/descendant-id ?i]] @db-conn id)))

(defn filter-forecasts
  [{:keys [expand filter] :or {expand []
                               filter nil}}] ;; filter is only applied to top-level forecasts.
  (let [pred      (create-filter-pred filter)
        top-level (apply concat (d/q '[:find (pull ?e [*])
                                       :in $ ?pred
                                       :where
                                       [?e :forecast/version-id _]
                                       [?e :forecast/name ?n]
                                       [(get-else $ ?e :forecast/descendant-id nil) ?u]
                                       [(nil? ?u)]
                                       [(?pred ?n)]]
                                     @db-conn
                                     pred))]
    (if (not-empty expand)
      (mapcat (fn [forecast]
                (if (some (fn [[ck cv]] (if (= (ck forecast) cv) [ck cv])) expand)
                  (let [id (:forecast/version-id forecast)
                        desc-list (build-descendant-list id)]
                    (apply conj [forecast] (mapv #(merge {} (d/pull @db-conn '[*] %)) desc-list)))
                  [forecast])) top-level)
      top-level)))

(defn fetch-forecasts-by-forecast-id
  [forecast-id]
  (apply concat (d/q '[:find (pull ?e [*])
                       :in $ ?forecast-id
                       :where
                       [?e :forecast/forecast-id ?forecast-id]]
                     @db-conn
                     forecast-id)))

(defn fetch-models
  []
  (apply concat (d/q '[:find (pull ?e [*])
                       :where [?e :model/version-id _]]
                     @db-conn)))

(defn find-model-id-by-name-and-version
  [name version]
  (d/q '[:find ?e ?model-id ?id
         :in $ ?name ?version
         :where [?e :model/version-id ?id]
         [?e :model/model-id ?model-id]
         [?e :model/version ?version]
         [?e :model/name ?name]]
       @db-conn
       name version))

(defn format-model-prop
  [[k v]]
  {:name k :value v})

(defn fetch-data-items
  [{:keys [filter category] :or {filter nil
                                 category nil}}]
  (let [pred (create-filter-pred filter)
        category-pred (fn [c] (if-not category
                                true
                                (= c category)))]
    (apply concat (d/q '[:find (pull ?e [*])
                         :in $ ?pred ?category-pred
                         :where
                         [?e :data/data-id _]
                         [?e :data/name ?n]
                         [?e :data/category ?c]
                         [(?pred ?n)]
                         [(?category-pred ?c)]]
                       @db-conn
                       pred
                       category-pred))))

(defn fix-forecast-inputs
  [forecast]
  (let [inputs (->> forecast
                    :inputs
                    (mapv #(first
                            (map
                             (fn [[k v]] (hash-map :category (:category v) :selected v)) %))))]
    (assoc forecast :inputs inputs)))

(defn filter-top-level-forecasts-with-ancestors
  [forecasts]
  (->> forecasts
       (filter :forecast/latest?)
       (filter #(> (:forecast/version %) 1))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(wm/create-standard-service!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod request-handler
  :filter-forecasts
  [owner event args result-ch]
  (let [forecasts (filter-forecasts (select-keys args [:expand :filter]))]
    (put! result-ch [:success {:forecasts (->> forecasts
                                               (sort-by :forecast/version #(compare %2 %1))
                                               (sort-by :forecast/name))
                               :has-ancestors (->>
                                               (filter-top-level-forecasts-with-ancestors forecasts)
                                               (map #(vector (:db/id %) (:forecast/version-id %)))
                                               set)}])))

(defmethod request-handler
  :fetch-forecast-versions
  [owner event forecast-id result-ch]
  (venue/request! {:owner   owner
                   :service :service/api
                   :request :get-forecast-versions
                   :args    forecast-id
                   :context result-ch}))

(defmethod request-handler
  :fetch-forecasts
  [owner event id result-ch]
  (venue/request! {:owner   owner
                   :service :service/api
                   :request :get-forecasts
                   :args    id
                   :context result-ch}))

(defmethod request-handler
  :fetch-forecast
  [owner event id result-ch]
  (venue/request! {:owner   owner
                   :service :service/api
                   :request :get-forecast
                   :args    id
                   :context result-ch}))

(defmethod request-handler
  :fetch-model
  [owner event id result-ch]
  (venue/request! {:owner   owner
                   :service :service/api
                   :request :get-model
                   :args    id
                   :context result-ch}))

(defmethod request-handler
  :fetch-models
  [owner event _ ch]
  (venue/request! {:owner   owner
                   :service :service/api
                   :request :get-models
                   :context ch}))

(defmethod request-handler
  :add-forecast
  [owner event {:keys [model-name model-version model-props name description public?]} result-ch]
  (let [model-id (find-model-id-by-name-and-version model-name (js/parseInt model-version))]
    (if (not-empty model-id)
      (let [payload {:model-id (-> model-id first second)
                     :name name
                     :public? public?}
            payload (if (not-empty description) (assoc payload :description description) payload)
            payload (if (not-empty model-props) (assoc payload :model-properties (mapv format-model-prop  model-props)) payload)]
        (venue/request! {:owner   owner
                         :service :service/api
                         :request :create-forecast
                         :args    payload
                         :context result-ch}))
      (log/severe "Unable to locate a model with this name and version: " model-name model-version))))

(defmethod request-handler
  :upload-data
  [owner event args result-ch]
  (venue/request! {:owner owner
                   :service :service/api
                   :request :upload-data
                   :args args
                   :context result-ch
                   :timeout? false}))

(defmethod request-handler
  :fetch-data-items
  [owner event args result-ch]
  (venue/request! {:owner   owner
                   :service :service/api
                   :request :get-data-items
                   :args    args
                   :context {:result-ch result-ch
                             :category args}}))

(defmethod request-handler
  :filter-data-items
  [owner event args result-ch]
  (let [result (fetch-data-items args)]
    (log/debug result)
    (put! result-ch [:success result])))

(defmethod request-handler
  :add-forecast-version
  [owner event forecast result-ch]
  (log/info "Creating a new version of forecast " (:forecast/name forecast))
  (venue/request! {:owner owner
                   :service :service/api
                   :request :create-forecast-version
                   :args forecast
                   :context result-ch
                   :timeout 20000}))

(defmethod request-handler
  :download-file
  [owner _ url result-ch]
  (venue/request! {:owner   owner
                   :service :service/api
                   :request :download-file
                   :args    url
                   :context result-ch}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod response-handler
  [:get-forecast-versions :success]
  [owner _ forecast-versions result-ch]
  (log/debug "Received" (count forecast-versions) "forecast versions.")
  (let [latest-forecast (first forecast-versions)
        older-forecasts (map #(assoc % :descendant-id (:version-id latest-forecast)) (rest forecast-versions))]
    (put-item-into-db! latest-forecast :forecast)
    (doseq [f older-forecasts]
      (put-item-into-db! f :forecast)))

  (put! result-ch [:success nil]))

(defmethod response-handler
  [:get-forecast-versions :failure]
  [owner _ msg result-ch]
  (log/debug "get-forecast-versions failure" msg))

(defmethod response-handler
  [:get-forecasts :success] ;;plural
  [owner _ forecasts result-ch]
  (reset-db!)
  (log/debug "Received" (count forecasts) "forecasts.")
  (doseq [f forecasts]
    (put-item-into-db! f :forecast))
  (put! result-ch [:success nil]))

(defmethod response-handler
  [:get-forecasts :failure]
  [owner _ _ result-ch]
  (put! result-ch [:failure nil]))

(defmethod response-handler
  [:get-models :success]
  [owner _ models result-ch]
  (log/debug "Received" (count models) "models.")
  (doseq [m models]
    (put-item-into-db! m :model))
  (put! result-ch [:success (fetch-models)]))

(defmethod response-handler
  [:get-models :failure]
  [owner _ _ result-ch]
  (put! result-ch [:failure nil]))

(defmethod response-handler
  [:create-forecast :success]
  [owner _ new-forecast result-ch]
  (put-item-into-db! new-forecast :forecast)
  (venue/request! {:owner   owner
                   :service :service/analytics
                   :request :track-create-forecast
                   :args {:status :success
                          :name (:name new-forecast)
                          :version-id (:version-id new-forecast)
                          :forecast-id (:forecast-id new-forecast)}})
  (put! result-ch [:success (select-keys new-forecast [:forecast-id :version])]))

(defmethod response-handler
  [:create-forecast :failure]
  [owner _ _ result-ch]
  (venue/request! {:owner   owner
                   :service :service/analytics
                   :request :track-create-forecast
                   :args {:status :failure}})
  (put! result-ch [:failure nil]))

(defmethod response-handler
  [:get-forecast :success]
  [owner _ forecast result-ch]
  (let [fixed (fix-forecast-inputs forecast)]
    (put! result-ch [:success (put-item-into-db! fixed :forecast)])))

(defmethod response-handler
  [:get-forecast :failure]
  [owner _ {:keys [status]} result-ch]
  (put! result-ch [:failure status]))

(defmethod response-handler
  [:get-model :success]
  [owner _ model result-ch]
  (put! result-ch [:success (put-item-into-db! model :model)]))

(defn data-id-db-workaround
  [{:keys [data-id version]}]
  (str data-id "-" version))

(defmethod response-handler
  [:upload-data :success]
  [owner _ raw-data-item result-ch]
  ;; doesn't hear about it until we send the key as part of a forecast update
  (let [data-item (assoc raw-data-item :local? true)]
    (log/debug "Upload succeeded.")
    (venue/request! {:owner   owner
                     :service :service/analytics
                     :request :track-upload
                     :args (merge raw-data-item {:status :success})})
    (let [new-data-item (put-item-into-db! data-item :data data-id-db-workaround)]
      (put! result-ch [:success {:data-items (fetch-data-items {:category (:category data-item)})
                                 :new-data-item new-data-item}]))))

(defmethod response-handler
  [:upload-data :failure]
  [owner _ response result-ch]
  (venue/request! {:owner   owner
                   :service :service/analytics
                   :request :track-upload
                   :args (merge response {:status :failure})})
  (put! result-ch [:failure (:response response)]))

(defmethod response-handler
  [:get-data-items :success]
  [owner _ data-items {:keys [result-ch category]}]
  (log/debug "Received" (count data-items) "data items.")
  (doseq [d data-items]
    (put-item-into-db! d :data data-id-db-workaround))
  (put! result-ch [:success (fetch-data-items {:category category})]))

(defmethod response-handler
  [:get-data-items :failure]
  [owner _ response {:keys [result-ch]}]
  (put! result-ch [:failure nil]))

(defmethod response-handler
  [:create-forecast-version :success]
  [owner _ forecast result-ch]
  (let [fixed (fix-forecast-inputs forecast)
        ancestors (fetch-forecasts-by-forecast-id (:forecast-id fixed))]
    (doseq [f ancestors]
      (d/transact! db-conn [[:db/add (:db/id f) :forecast/descendant-id (:version-id fixed)]]))
    (venue/request! {:owner   owner
                     :service :service/analytics
                     :request :track-create-forecast-version
                     :args {:status :success
                            :name (:name forecast)
                            :forecast-id (:forecast-id forecast)
                            :version-id (:version-id forecast)
                            :version (:version forecast)}})
    (put! result-ch [:success (put-item-into-db! fixed :forecast)])))

(defmethod response-handler
  [:create-forecast-version :failure]
  [owner _ _ result-ch]
  (venue/request! {:owner   owner
                   :service :service/analytics
                   :request :track-create-forecast-version
                   :args {:status :failure}})
  (put! result-ch [:failure nil]))

(defmethod response-handler
  [:download-file :success]
  [owner _ response result-ch]
  (log/info "Received redirect for download:" response)
  (venue/request! {:owner   owner
                   :service :service/analytics
                   :request :track-public-download
                   :args (merge response {:status :success})})
  (put! result-ch [:success (:location response)]))

(defmethod response-handler
  [:download-file :failure]
  [owner _ _ result-ch]
  (venue/request! {:owner   owner
                   :service :service/analytics
                   :request :track-public-download
                   :args {:status :failure}})
  (put! result-ch [:failure nil]))

;;;;;;;;;;;;;;;;;;;;;

(defn- do-login!
  [logged-in?]
  (swap! state assoc :logged-in? logged-in?)
  (if logged-in?
    (venue/reactivate!)))

(util/inline-subscribe!
 :api/user-logged-in
 #(do-login! true))

(util/inline-subscribe!
 :api/user-logged-out
 #(do-login! false))

;;;;;;;;;;;;;;;;;;;;;
;; Empties - we don't want to handle a response
;;;;;;;;;;;;;;;;;;;;

(defmethod response-handler
  [:track-create-forecast :success]
  [_ _ _ _])

(defmethod response-handler
  [:track-create-forecast-version :success]
  [_ _ _ _])

(defmethod response-handler
  [:track-public-download :success]
  [_ _ _ _])

(defmethod response-handler
  [:track-upload :success]
  [_ _ _ _])
