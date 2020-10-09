(ns clotter.tweet
  (:require [schema.core :as s]
            [clotter.string-util :as str]))

(defn valid-tweet? [name]
  (str/non-blank-with-max-length? 280 name))

(s/defschema TweetSchema
  {:id   s/Str
   :text s/Str})