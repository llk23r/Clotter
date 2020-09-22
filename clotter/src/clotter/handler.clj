(ns clotter.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [cheshire.core :refer :all]
            [clojure.data.json :as json]
            [clj-http.client :as client]))

(def ^:const RECENT-SEARCH-ENDPOINT "https://api.twitter.com/2/tweets/search/recent")

(def ^:const BEARER-TOKEN (str "BEARER " (System/getenv "TWITTER_BEARER")))

(s/defschema Tweet
  {:id s/Str
   :text s/Str})

(defn tweets-collection [query-params max-results]
  (let [response (client/get
                  RECENT-SEARCH-ENDPOINT
                  {:headers      {:authorization BEARER-TOKEN}
                   :query-params {:query query-params
                                  :max_results (Integer. max-results)}})]
    response))

(defn fetch-recent-tweets
  "Fetch tweets by a user in the last 7 days."
  [user-name max-results]
  (println "***************"user-name"!!")
  (let [query-params (str "from:" user-name " -is:retweet")
        response     (tweets-collection query-params max-results)]
    (parse-string (get response :body))))


(def app
  (api
   {:swagger {:ui   "/"
              :spec "/swagger.json"
              :data {:info {:title       "Clotter"
                            :description "Compojure Api example"}
                     :tags [{:name        "api"
                             :description "some apis"}]}}}

   (context "/api" []
     :tags ["api"]

     (GET "/tweets" []
       :query-params [user-name :- s/Str
                      {max-results :- s/Str 20}]
       :summary "success"
       (ok {:result (fetch-recent-tweets user-name max-results)})))))


