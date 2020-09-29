(ns clotter.core
  (:require [toucan.db :as db]
            [schema.core :as s]
            [toucan.models :as models]
            [clojure.set :refer [rename-keys]]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.api.sweet :refer :all]
            [compojure.api.exception :as ex]
            [ring.util.http-response :refer [ok not-found]]
            [clotter.handler :as handler]
            [clotter.models.tweet :refer [Tweet]])
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
    (ok {:result (db/select Tweet :tweet_id [:in tweet_ids])})
    (not-found)))

(def tweet-routes
  (GET "/tweets" []
    :query-params [user-name    :- s/Str
                   {max-results :- s/Str 10}
                   {bearer-token :- s/Str handler/ENV-BEARER-TOKEN}]
    :summary "success"
    (let [formatted-response (vec (formatted-response user-name max-results bearer-token))
          tweet-ids (tweet-ids formatted-response)
          existing-tweets (existing-tweets tweet-ids)
          existing-tweet-ids (set (existing-tweet-ids existing-tweets))]
      (-> (remove #(existing-tweet-ids (:tweet_id %)) formatted-response)
          (as-> tweets (db/insert-many! Tweet tweets)))
      (tweet->response tweet-ids))))


(def app
  (api {:swagger swagger-config}
       (context "/api" []
         :tags ["api"]
         (routes tweet-routes))))


(defn -main
  [& args]
  (db/set-default-db-connection! db-spec)
  (models/set-root-namespace! 'clotter.models)
  (run-jetty app {:port 3001}))