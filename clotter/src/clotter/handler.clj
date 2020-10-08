(ns clotter.handler
  (:require [cheshire.core :as json]
            [clj-http.client :as client]))

(def ^:const RECENT-SEARCH-ENDPOINT "https://api.twitter.com/2/tweets/search/recent")

(def ^:const ENV-BEARER-TOKEN (str "BEARER " (System/getenv "TWITTER_BEARER")))

(defn tweets-collection [query-params max-results twitter-bearer-token]
  (let [response (client/get
                  RECENT-SEARCH-ENDPOINT
                  {:headers      {:authorization twitter-bearer-token}
                   :query-params {:query query-params
                                  :max_results (Integer. max-results)}})]
    response))

(defn fetch-recent-tweets
  "Fetch tweets by a user in the last 7 days."
  [user-name max-results twitter-bearer-token]
  (let [query-params (str "from:" user-name " -is:retweet")
        response     (tweets-collection query-params max-results twitter-bearer-token)]
    (json/parse-string (get response :body))))


;; (def app
  ;; (api
  ;;  {:swagger {:ui   "/"
              ;; :spec "/swagger.json"
              ;; :data {:info {:title       "Clotter"
                            ;; :description "Compojure Api example"}
                    ;;  :tags [{:name        "api"
                            ;;  :description "some apis"}]}}}
;; 
  ;;  (context "/api" []
    ;;  :tags ["api"]
;; 
    ;;  (GET "/tweets" []
      ;;  :query-params [user-name    :- s/Str
                      ;; {max-results :- s/Str 10}
                      ;; {bearer-token :- s/Str ENV-BEARER-TOKEN}]
      ;;  :summary "success"
      ;;  (ok {:result (fetch-recent-tweets user-name max-results bearer-token)})))))


;; TODO - store data in both the dbs
;; TODO - API to read tweets from the table
