(ns goatmud.account.web-schema
  (:require [sci.core]
            [malli.core :as m]
            [goatmud.account.schema :as s]))

(def create-account-schema
  [:and [:map [:username s/account-username-schema]
         [:email s/account-email-schema]
         [:password s/account-password-schema]
         [:confirm-password s/account-confirm-password-schema]]
   [:fn {:error/message "passwords don't match"
         :error/path [:confirm-password]}
    '(fn [{:keys [password confirm-password] :as afds} bfds]
       (or (empty? confirm-password)
           (= password confirm-password)))]])
