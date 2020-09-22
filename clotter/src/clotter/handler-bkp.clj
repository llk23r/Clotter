(ns clotter.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [cheshire.core :refer :all]
            [clojure.data.json :as json]
            [clj-http.client :as client]))

(s/defschema Pizza
  {:name s/Str
   (s/optional-key :description) s/Str
   :size (s/enum :L :M :S)
   :origin {:country (s/enum :FI :PO)
            :city s/Str}})

(s/defschema Tweet
  {:id s/Str
   :text s/Str})

(defn tweets-collection [twitter-endpoint query]
  (let [bearer-token (str "BEARER "(System/getenv "TWITTER_BEARER"))
        response (client/get twitter-endpoint {:headers      {:authorization bearer-token}
                                               :query-params {:query query}})]
    response))

(defn fetch-tweets [user-name]
  (let [query (str "from:" user-name)
        twitter-endpoint "https://api.twitter.com/2/tweets/search/recent"
        response (tweets-collection twitter-endpoint query)]
    (parse-string (get response :body))))

;; (fetch-tweets "elonmusk")

(def app
  (api
   {:swagger
    {:ui "/"
     :spec "/swagger.json"
     :data {:info {:title "Clotter"
                   :description "Compojure Api example"}
            :tags [{:name "api", :description "some apis"}]}}}

   (context "/api" []
     :tags ["api"]

     (GET "/tweets" []
        ; :return Tweet
       :query-params [user-name :- s/Str]
        ; :body [tweet Tweet]
       :summary "success"
        ; (ok tweet)
       (ok {:result (fetch-tweets user-name)}))

     (GET "/plus" []
       :return {:result Long}
       :query-params [x :- Long, y :- Long]
       :summary "adds two numbers together"
       (ok {:result (+ x y)}))

     (POST "/echo" []
       :return Pizza
       :body [pizza Pizza]
       :summary "echoes a Pizza"
       (ok pizza)))))
