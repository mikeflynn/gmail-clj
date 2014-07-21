(ns gmail-clj.core
  (:require [org.httpkit.client :as http]
            [cheshire.core :as json]
            [clojure.walk :as walk]
            [clojure.data.codec.base64 :as b64])
  (:import (javax.mail Session Address Message Message$RecipientType)
           (javax.mail.internet MimeMessage InternetAddress)
           (java.net URLEncoder)))

(def base-url "https://www.googleapis.com/gmail/v1")
(def scopes ["https://www.googleapis.com/auth/gmail.modify"])

(def #^{:dynamic true} *client-id* nil)
(def #^{:dynamic true} *client-secret* nil)
(def #^{:dynamic true} *refresh-token* nil)
(def #^{:dynamic true} *access-token* nil)

(defn- now [] (quot (System/currentTimeMillis) 1000))

(defn set-client-id!
  "Set the client ID for the library."
  [id]
  (alter-var-root (var *client-id*) (fn [_] id)))

(defn set-client-secret!
  "Set the client secret for the library."
  [secret]
  (alter-var-root (var *client-secret*) (fn [_] secret)))

(defn set-refresh-token!
  "Set the refresh token for the library."
  [token]
  (alter-var-root (var *refresh-token*) (fn [_] token)))

(defn set-access-token!
  "Set the temp access token and it's expires time."
  [token expires]
  (alter-var-root (var *access-token*) (fn [_] {:token token :expires (+ (now) expires)})))

(defn prepend-url [url]
  (if (.startsWith url "http")
      url
      (str base-url url)))

(defmulti send-http (fn [method url params headers] method))

(defmethod send-http :get [method url params headers]
  (http/get (prepend-url url) {:timeout 2000
                                :headers headers
                                :query-params params}))

(defmethod send-http :post [method url params headers]
  (http/post (prepend-url url) {:timeout 2000
                                 :headers headers
                                 :form-params params}))

(defmethod send-http :post-json [method url params headers]
  (http/post (prepend-url url) {:timeout 2000
                                 :headers (assoc headers "Content-Type" "application/json")
                                 :body (json/generate-string params)}))

(defn- check-auth
  "Checks for auth token."
  [token]
  (if (empty? token)
      (throw (Exception. "Missing authentication token!"))
      true))

(defn- clean-params
  "Removes any nil params."
  [params]
  (->> params
       (filter #(not (nil? (val %))))
       (into {})))

(defn- api-request
  "Wrapper to HTTP client that makes the request to the API."
  [method url params & {:keys [forget resp headers auth]
                        :or {resp :json}}]
  (let [headers (if (not headers) {} headers)
        headers (if auth (assoc headers "Authorization" (str "Bearer " auth)) headers)
        response (send-http method url (clean-params params) headers)]
    (if (not forget)
        (if (not= (get @response :status) 200)
            (throw (Exception. (str "Received " (get @response :status) " response code. With response: " @response)))
            (try
              (case resp
                :json (let [body (->> (:body @response)
                                      json/parse-string
                                      walk/keywordize-keys)]
                        (if (:error body)
                            (throw (Exception. (if (:message body) (:message body)
                                                                   "Error or blank response from Beats Platform.")))
                            body))
                :raw @response
                :url {:url (get-in @response [:opts :url])})
              (catch Exception e {:error (.getMessage e)}))))))

(defn- request-auth
  "Get an authorization token"
  []
  (when (not *client-id*) (throw (Exception. "Missing client id!")))
  (when (not *client-secret*) (throw (Exception. "Missing client secret!")))
  (when (not *refresh-token*) (throw (Exception. "Missing refresh token!")))
  (if-let [resp (api-request :post
                             "https://accounts.google.com/o/oauth2/token"
                             {:client_id *client-id*
                              :client_secret *client-secret*
                              :refresh_token *refresh-token*
                              :grant_type "refresh_token"})]
    (set-access-token! (:access_token resp) (:expires_in resp))
    false))

(defn- get-token
  "Returns the current active auth token."
  []
  (when (or (nil? *access-token*)
          (> (:expires *access-token*) (now)))
    (request-auth))
  (:token *access-token*))

(defn bytes? [x]
  (= (Class/forName "[B")
     (.getClass x)))

(defn str->b64 [input]
  (let [input (if (bytes? input) input (.getBytes input))]
    (String. (b64/encode input) "UTF-8")))

(defn b64->str [input]
  (let [input (if (bytes? input) input (.getBytes input))]
    (String. (b64/decode input))))

(defmacro address [& args]
  `(new InternetAddress ~@args))

(defn force-address [addrs]
  (let [addrs (if (not (vector? addrs))
                  (vector addrs)
                  addrs)]
    (map #(if (string? %) (address %) %) addrs)))

(defn mime->map
  "Takes a mime-formated message and returns a map of the data."
  [msg]
  (let [session (Session/getDefaultInstance (java.util.Properties.))
        msg (if (string? msg)
              (->> (.getBytes msg)
                   clojure.java.io/input-stream
                   (MimeMessage. session))
              msg)]
    {:headers (.getAllHeaders msg)
     :to (InternetAddress/toString (.getRecipients msg Message$RecipientType/TO))
     :cc (InternetAddress/toString (.getRecipients msg Message$RecipientType/CC))
     :bcc (InternetAddress/toString (.getRecipients msg Message$RecipientType/BCC))
     :from (InternetAddress/toString (.getFrom msg))
     :date (.getSentDate msg)
     :subject (.getSubject msg)
     :content (.getContent msg)}))

(defn map->mime
  "Takes a map and returns a mime-formatted object."
  [msg]
  (let [session (Session/getDefaultInstance (java.util.Properties.))]
    (doto (MimeMessage. session)
          (.setFrom (address (:from msg)))
          (.addRecipient Message$RecipientType/TO (address (:to msg)))
          ;(when (:bcc msg) (.addRecipient Message$RecipientType/CC (address (:cc msg)))
          ;(when (:bcc msg) (.addRecipient Message$RecipientType/BCC (address (:bcc msg))))
          (.setSubject (:subject msg))
          (.setText (:body msg)))))

; Users.drafts

; Users.history

; Users.labels

; Users.messages

(defn messages-list
  "Returns a list of messages based on a given query."
  [query & {:keys [labels max-results page] :or {max-results 10}}]
  (let [params {:includeSpamTrash false
                :labelIds labels
                :maxResults max-results
                :pageToken page
                :q query}]
    (api-request :get "/users/me/messages/" params :auth (get-token))))

(defn messages-trash
  "Moves a specific message to the trash can."
  [message-id]
  (api-request :post (str "/users/me/messages/" message-id "/trash") {} :auth (get-token)))

(defn messages-get
  "Returns the record for a given message id."
  [message-id & {:keys [format]
                 :or {format :full}}]
  (api-request :get (str "/users/me/messages/" message-id) {:format (name format)} :auth (get-token)))

(defn messages-send
  "Sends an email to a specified user."
  [message & {:keys [thread-id]}]
  (let [message (if (map? message) (map->mime message) message)
        bytes (with-open [os (java.io.ByteArrayOutputStream.)]
                (.writeTo message os)
                (.toByteArray os))
        b64 (str->b64 bytes)]
    (api-request :post-json "/users/me/messages/send" {:raw (URLEncoder/encode b64 "UTF-8")} :auth (get-token) :resp :raw)))

; Users.threads

; Testing code...
(set-client-id! "1027354928765-67in4888cilm4r81ek4s110asr59abnp.apps.googleusercontent.com")
(set-client-secret! "qFt_o9ekrr7_OH2CH-ZlZLyx")
(set-refresh-token! "1/SG6x5odD0CwNQ_JfYFO9MFK_gKRbC96YRPBaMm43Wp8")
