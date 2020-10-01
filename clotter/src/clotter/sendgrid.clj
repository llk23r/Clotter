(ns clotter.sendgrid
  (:require [clj-http.client :as http]
            [clojure.string :as str]))

(def ^:const SENDGRID-ENDPOINT "https://api.sendgrid.com/v3/mail/send")
(def ^:const ENV-SENDGRID-TOKEN (str "BEARER " (System/getenv "SENDGRID_API_KEY")))
(def ^:const ENV-VERIFIED-SINGLE-SENDER-EMAIL (System/getenv "SENDGRID_FROM_EMAIL"))
(def ^:const ENV-DEFAULT-TO-EMAIL (System/getenv "DEFAULT_TO_EMAIL"))

(defn escape-csv-value [value]
   (-> (str/replace value #"\"" "")
      (as-> rdt (str "\"" rdt "\""))))

(defn row->csv-row [row]
  (->> (map escape-csv-value row)
       (str/join ",")))

(defn ms->csv-string [ms]
  (let [columns (keys (first ms))
        headers (map name columns)
        rows    (map #(map % columns) ms)]
    (->> (into [headers] rows)
         (map row->csv-row)
         (str/join "\n"))))

(defn encode-string-to-base64 [string]
  (.encodeToString (java.util.Base64/getEncoder) (.getBytes string)))

(defn send-email-with-csv [to-email csv-string]
  (http/post SENDGRID-ENDPOINT
             {:headers      {:authorization ENV-SENDGRID-TOKEN}
              :content-type :json
              :form-params
              {:personalizations [{:to      [{:email to-email}]
                                   :subject "Your Tweets Feed Is Here"}]
               :from             {:email ENV-VERIFIED-SINGLE-SENDER-EMAIL}
               :content          [{:type  "text/plain"
                                   :value "Clotter Report!"}]
               :attachments
               [{:filename "clotter.csv"
                 :content  (encode-string-to-base64 csv-string)}]}}))

(defn send-email [to-email data]
  (println "INNNN\n")
  (->> data
       ms->csv-string
       (send-email-with-csv (or to-email ENV-DEFAULT-TO-EMAIL))))