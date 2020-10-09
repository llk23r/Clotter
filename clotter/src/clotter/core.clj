(ns clotter.core
  (:require [toucan.db :as db]
            [cheshire.core :as json]
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
            [java-time :as t]
            [honeysql.format :as hformat]
            [clotter.sendgrid :as sendgrid]
            [clotter.string-util :as sutil]
            compojure.api.async)
  (:gen-class :main true))


(defmethod hformat/fn-handler "ilike"
  [_ lhs rhs]
  (format "(%s ilike %s)" (hformat/to-sql lhs) (hformat/to-sql rhs)))

(def db-spec
  {:dbtype   "postgres"
   :dbname   "clotter"
   :user     "ach"
   :password "postgres"})


(def swagger-config
  {:ui   "/"
   :spec "/swagger.json"
   :data {:info {:title       "Clotter"
                 :description "Compojure Api Twitter Example"}
          :tags [{:name        "api"
                  :description "some apis"}]}})


(defn formatted-response [user-name max-results twitter-bearer-token]
  (-> (handler/fetch-recent-tweets user-name max-results twitter-bearer-token)
      (get "data")
      (as-> parsed-data (map #(rename-keys % {"id" :tweet_id "text" :tweet_text}) parsed-data))))

(defn tweet-ids [formatted-resp]
  (map :tweet_id formatted-resp))

(defn existing-tweets [tweet-ids]
  (db/select Tweet :tweet_id [:in tweet-ids]))

(defn existing-tweet-ids [existing-tweets]
  (map :tweet_id existing-tweets))

(defn tweet->response [tweet_ids max to-email sendgrid-bearer-token sendgrid-verified-email]
  (if tweet_ids
    (let [db-tweets (db/select [Tweet :id :tweet_id :tweet_text :created_at :user_name] :tweet_id [:in tweet_ids] {:limit max})
          send-email? (if (cls/blank? to-email) false true)
          response-map {:total-tweets (count db-tweets)
                        :result       db-tweets}]
      (if send-email?
        (-> db-tweets
            (as-> t (map #(dissoc % :id (:id %)) t))
            (as-> dt (map #(assoc % :tweet_link (str "https://twitter.com/" (:user_name %) "/status/" (:tweet_id %))) dt))
            (as-> tweets-data (println (str "\n" tweets-data "\n" "Email Triggered!\n\nEMAIL RESPONSE: \n" (sendgrid/send-email to-email tweets-data sendgrid-bearer-token sendgrid-verified-email)))))
        nil)
      (ok response-map))
    (not-found)))

(defn max-results-resolver [max-results]
  (if (or (cls/blank? max-results) (> (Integer. max-results) 100))
    100
    (if (< (Integer. max-results) 10)
      10
      (Integer. max-results))))

(defn error-responders [responder-map]
  (let [{:keys [response message context]} responder-map]
    (json/generate-string {:response response})))

(defn blank-tweets-responder [ctx-map]
  (let [{:keys [context]} ctx-map]
    (error-responders {:response {:errorCode  "CLT-1000"
                                  :statusCode 404
                                  :message    (str "Possible reason: An account with the provided handle @" (:user-name context) " doesn't exist.")}})))

(defn decode-base64 [encoded-base64-string]
  (String. (.decode (java.util.Base64/getDecoder) (.getBytes (str encoded-base64-string)))))

(defn decode-bearer [encoded-base64-string]
  (str "BEARER " (decode-base64 encoded-base64-string)))

(defn internal-server-error-responder [excp]
  (println (str "Caught Exception: " (.toString excp)))
  (error-responders {:response {:errorCode  "CLT-9999"
                                :statusCode 500
                                :message    "Please ensure the input fields are valid."}}))

(defn invalid-email-responder [email]
  (println (str "Invalid Email: " email))
  (error-responders {:response {:errorCode "CLT-1001"
                                :statusCode 422
                                :message (str "Please ensure the email " email " is valid")}}))

(def fetch-tweets-route
  (GET "/tweets" []
    :query-params [user-name :- s/Str
                   {max-results :- s/Str ""}
                   {twitter-bearer-token :- s/Str handler/ENV-BEARER-TOKEN}
                   {sendgrid-bearer-token :- s/Str sendgrid/ENV-SENDGRID-TOKEN}
                   {sendgrid-verified-email :- s/Str sendgrid/ENV-VERIFIED-SINGLE-SENDER-EMAIL}
                   {to-email :- s/Str ""}]
    :summary "Fetch New Tweets For The Given UserName"
    (try
      (let [max (max-results-resolver max-results)
            decoded-twitter-bearer (decode-bearer twitter-bearer-token)
            decoded-sendgrid-bearer (decode-bearer sendgrid-bearer-token)
            formatted-response (vec (formatted-response user-name max decoded-twitter-bearer))]
        (cond
          (and (not (cls/blank? to-email)) (not (sutil/email? to-email))) (invalid-email-responder {:email to-email})
          (not (sutil/email? sendgrid-verified-email)) (invalid-email-responder {:email sendgrid-verified-email})
          (empty? formatted-response) (blank-tweets-responder {:context {:user-name user-name}})
          :else (let [tweet-ids (tweet-ids formatted-response)
                      existing-tweets (existing-tweets tweet-ids)
                      existing-tweet-ids (set (existing-tweet-ids existing-tweets))]
                  (-> (remove #(existing-tweet-ids (:tweet_id %)) formatted-response)
                      (as-> new-tweets (map #(assoc % :user_name (str user-name)) new-tweets))
                      (as-> tweets (db/insert-many! Tweet tweets)))
                  (tweet->response tweet-ids max to-email decoded-sendgrid-bearer sendgrid-verified-email))))
      (catch Exception e
        (internal-server-error-responder e)))))

(defn string-to-date
  [date-string]
  (t/local-date "yyyy-MM-dd" date-string))

(def today (t/plus (t/local-date) (t/days 1)))
(def yesterday (t/minus today (t/days 2)))

(defn start-date-resolver [start-date]
  (if (cls/blank? start-date)
    yesterday
    (string-to-date start-date)))

(defn end-date-resolver [end-date]
  (if (cls/blank? end-date)
    today
    (string-to-date end-date)))

(defn resolved-query [user-name sdate edate word max-tweets]
  (if (and (cls/blank? word) (cls/blank? user-name))
    []
    (if (or (cls/blank? word) (cls/blank? user-name))
      (if (cls/blank? word)
        (db/select [Tweet :id :tweet_id :tweet_text :created_at] :created_at [:between sdate edate]
                   :user_name [:ilike user-name] {:limit max-tweets})
        (db/select [Tweet :id :tweet_id :tweet_text :created_at] :created_at [:between sdate edate]
                   :tweet_text [:ilike (str "%" word "%")] {:limit max-tweets}))
      (db/select [Tweet :id :tweet_id :tweet_text :created_at] :created_at [:between sdate edate]
                 :user_name [:ilike user-name], :tweet_text [:ilike (str "%" word "%")] {:limit max-tweets}))))

(def search-tweets-route
  (GET "/tweets-search" []
    :query-params [{user-name :- s/Str ""}
                   {start-date :- s/Str ""}
                   {end-date :- s/Str ""}
                   {contains-word :- s/Str ""}
                   {max-tweets :- s/Str 100}]
    :summary "Search for Tweets Data From the DB"
    (try
      (let [sdate (start-date-resolver start-date)
            edate (end-date-resolver end-date)
            word contains-word
            max (Integer. max-tweets)
            db-query-response (resolved-query user-name sdate edate word max)]
        (ok {:total-tweets (count db-query-response)
             :result       db-query-response}))
      (catch Exception e
        (internal-server-error-responder e)))))


(def app
  (api {:swagger swagger-config}
       (context "/api" []
         :tags ["api"]
         (routes fetch-tweets-route search-tweets-route))))

(defn init-db []
  (db/set-default-db-connection! db-spec)
  (models/set-root-namespace! 'clotter.models))

(defn -main
  [& args]
  (init-db)
  (run-jetty app {:port 4000}))


