(ns clotter.core
  (:require [toucan.db :as db]
            [schema.core :as s]
            [toucan.models :as models]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as cls]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [ring.util.http-response :refer [ok not-found]]
            [clotter.handler :as handler]
            [clotter.models.tweet :refer [Tweet]]
            [java-time :as t])
  (:import (java.util Date))
  (:gen-class))

(def db-spec
  {:dbtype "postgres"
   :dbname "clotter"
   :user "ach"
   :password "ach"})


(def swagger-config
  {:ui   "/"
   :spec "/swagger.json"
   :data {:info {:title       "Clotter"
                 :description "Compojure Api Twitter Example"}
          :tags [{:name        "api"
                  :description "some apis"}]}})


(defn formatted-response [user-name max-results bearer-token]
  (-> (handler/fetch-recent-tweets user-name max-results bearer-token)
      (get "data")
      (as-> parsed-data (map #(rename-keys % {"id" :tweet_id "text" :tweet_text}) parsed-data))))

(defn tweet-ids [formatted-resp]
  (map :tweet_id formatted-resp))

(defn existing-tweets [tweet-ids]
  (db/select Tweet :tweet_id [:in tweet-ids]))

(defn existing-tweet-ids [existing-tweets]
  (map :tweet_id existing-tweets))

(defn tweet->response [tweet_ids]
  (if tweet_ids
    (ok {:result (db/select [Tweet :id :tweet_id :tweet_text :created_at] :tweet_id [:in tweet_ids])})
    (not-found)))

(def fetch-tweets-route
  (GET "/tweets" []
    :query-params [user-name    :- s/Str
                   {max-results :- s/Str 10}
                   {bearer-token :- s/Str handler/ENV-BEARER-TOKEN}]
    :summary "Fetch New Tweets For The Given UserName"
    (let [formatted-response (vec (formatted-response user-name max-results bearer-token))
          tweet-ids (tweet-ids formatted-response)
          existing-tweets (existing-tweets tweet-ids)
          existing-tweet-ids (set (existing-tweet-ids existing-tweets))]
      (-> (remove #(existing-tweet-ids (:tweet_id %)) formatted-response)
          (as-> new-tweets (map #(assoc % :user_name (str user-name)) new-tweets))
          (as-> tweets (db/insert-many! Tweet tweets)))
      (tweet->response tweet-ids))))

(defn string-to-date
  [date-string]
  (t/local-date "yyyy-MM-dd" date-string))

(def now (t/local-date))
(def yesterday (t/minus now (t/days 1)))

(defn start-date-resolver [start-date]
  (if (cls/blank? start-date)
    yesterday
    (string-to-date start-date)))

(defn end-date-resolver [end-date]
  (if (cls/blank? end-date)
    now
    (string-to-date end-date)))


(def search-tweets-route
  (GET "/tweets-search" []
    :query-params [user-name :- s/Str
                   {start-date :- s/Str ""}
                   {end-date :- s/Str ""}]
    :summary "Search for Tweets Data From the DB"
    (let [sdate (start-date-resolver start-date)
          edate (end-date-resolver end-date)]
      (ok {:result (db/select [Tweet :id :tweet_id :tweet_text :created_at] :created_at [:between sdate edate],
                              :user_name [:= user-name])}))))


(def app
  (api {:swagger swagger-config}
       (context "/api" []
         :tags ["api"]
         (routes fetch-tweets-route search-tweets-route))))


(defn -main
  [& args]
  (db/set-default-db-connection! db-spec)
  (models/set-root-namespace! 'clotter.models)
  (run-jetty app {:port 3001}))