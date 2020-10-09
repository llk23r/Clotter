(ns clotter.handler
  (:require [cheshire.core :as json]
            [clj-http.client :as client]))

(def ^:const RECENT-SEARCH-ENDPOINT "https://api.twitter.com/2/tweets/search/recent")

(def ^:const ENV-BEARER-TOKEN (System/getenv "TWITTER_BEARER"))

(defn tweets-collection [query-params max-results twitter-bearer-token]
  (let [response (client/get
                   RECENT-SEARCH-ENDPOINT
                   {:headers      {:authorization twitter-bearer-token}
                    :query-params {:query       query-params
                                   :max_results (Integer. max-results)}})]
    response))

(defn fetch-recent-tweets
  "Fetch tweets by a user in the last 7 days."
  [user-name max-results twitter-bearer-token]
  (let [query-params (str "from:" user-name " -is:retweet")
        response (tweets-collection query-params max-results twitter-bearer-token)]
    (json/parse-string (get response :body))))
