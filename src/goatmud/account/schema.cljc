(ns goatmud.account.schema
  (:require [goatmud.common.schema :as s]))

(def account-username-schema
  [:string {:min 3 :max 15}])

(def account-password-schema
  s/password-schema)

(def account-email-schema
  s/email-schema)

(def account-confirm-password-schema
  s/confirm-password-schema)
