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
       (fact (email? "randomemail@gmail.com") => true))

(fact "Invalid twitter handle returns empty collection"
      (empty? (vec (formatted-response invalid-twitter-handle max-results (decode-bearer base64-encoded-twitter-bearer)))) => true)

(fact "Invalid twitter handle API response returns response map with error code CLT-1000"
      (let [response (app (-> (mock/request :get (str "/api/tweets?user-name=" invalid-twitter-handle "&max-results=" max-results "&twitter-bearer-token=" base64-encoded-twitter-bearer))))
            body (parse-body (:body response))]
        (get-in body [:response :errorCode])) => "CLT-1000")
