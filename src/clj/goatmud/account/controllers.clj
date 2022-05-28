(ns goatmud.account.controllers
  (:require
   [ring.util.http-response :as response]
   [goatmud.account.web-schema :as schema]
   [goatmud.account.core :as core]))

(defn create-account
  [{{:keys [username email password] :as params} :body-params}]
  (try
    (let [account (core/create-account! username email password)]
      (response/ok {:data account :message "Account creation successful.  Please login."}))
    (catch Exception e
      (if-let [errors (:errors (ex-data e))]
        (response/bad-request {:errors errors})
        (throw e)))))

(def create-account-spec
  {:parameters
   {:body schema/create-account-schema
    :responses {200 {:body {:message string? :data map?}}
                400 {:body {:message string? :errors map?}}
                500 {:errors map?}}}
   :handler create-account})
