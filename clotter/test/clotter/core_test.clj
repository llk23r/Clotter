(ns clotter.core-test
  (:require [cheshire.core :as json]
            [midje.sweet :refer :all]
            [clotter.core :refer :all]
            [clotter.handler :refer :all]
            [clotter.sendgrid :refer :all]
            [clotter.string-util :refer :all]
            [ring.mock.request :as mock]))

(def invalid-twitter-handle "1theoatmeal1")
(def valid-twitter-handle "oatmeal")
(def base64-encoded-twitter-bearer ENV-BEARER-TOKEN)
(def max-results 10)
(def invalid-twitter-handle-response (vec (formatted-response invalid-twitter-handle max-results (decode-bearer base64-encoded-twitter-bearer))))
(def valid-twitter-handle-response (vec (formatted-response valid-twitter-handle max-results (decode-bearer base64-encoded-twitter-bearer))))
(def invalid-emails
  ["ab@cd" "" 12345 "abcdefg123" "12345@cd" "randomemail@domain" "randomemail@.extn" "randomemail@."])

(def base-query-params
  (str "&max-results=" max-results "&twitter-bearer-token=" base64-encoded-twitter-bearer))

(def base-query-params-w-invalid-handle
  (str "user-name=" invalid-twitter-handle base-query-params))

(def base-query-params-w-valid-handle
  (str "user-name=" valid-twitter-handle base-query-params))

(defn parse-body [body]
  (json/parse-string body true))

(facts "emails must conform to a structure"
       (fact (email? "ab@c.d") => true)
       (fact (email? "ab@cd") => false)
       (fact (email? "ab+x@c.d") => true)
       (fact (email? "") => false)
       (fact (email? nil) => false)
       (fact (email? 12345) => false)
       (fact (email? "abcdefg123") => false)
       (fact (email? "randomemail@gmail.com") => true)
       (fact (email? "123456789@outlook.com") => true))

(fact "Invalid twitter handle returns empty collection"
      (empty? invalid-twitter-handle-response) => true)

(fact "Valid twitter handle returns non-empty collection"
      (empty? valid-twitter-handle-response) => false)

(fact "Valid twitter handle returns a vector of maps with :id and :text keys which are transformed to :tweet_id and :tweet_text"
      (let [truthy-collection (map #(not (and (empty? (:tweet_id %)) (empty? (:tweet_text %)))) valid-twitter-handle-response)
            distinct-truthys (distinct truthy-collection)]
        (and (first distinct-truthys) (empty? (rest distinct-truthys)))) => true)

(fact "Invalid twitter handle API response returns response map with error code CLT-1000"
      (let [response (app (-> (mock/request :get (str "/api/tweets?" base-query-params-w-invalid-handle))))
            body (parse-body (:body response))]
        (get-in body [:errors :code])) => "CLT-1000")

(fact "Invalid email API response returns response map with error code CLT-1001"
      (let [rand-email (rand-nth invalid-emails)
            response (app (-> (mock/request :get (str "/api/tweets?" base-query-params-w-invalid-handle "&to-email=" rand-email))))
            body (parse-body (:body response))]
        (println "Random Invalid Email:" rand-email)
        (get-in body [:errors :code])) => "CLT-1001")

(fact "Invalid inputs causing exceptions return error code CLT-9999"
      (let [response (app (-> (mock/request :get (str "/api/tweets-search?user-name=" valid-twitter-handle "&start-date=" 0))))
            body (parse-body (:body response))]
        (get-in body [:errors :code])) => "CLT-9999")

(fact "/tweets returns a non-empty collection for fetched tweets"
      (let [response (app (-> (mock/request :get (str "/api/tweets?" base-query-params-w-valid-handle))))
            body (parse-body (:body response))]
        (get-in body [:success])) => true)

(fact "/tweets-search returns a non-empty collection for fetched tweets"
       (let [_ (init-db)
             clotter-api-response (app (-> (mock/request :get (str "/api/tweets-search?user-name=" valid-twitter-handle))))
             body (parse-body (:body clotter-api-response))]
         (get-in body [:success])) => true)