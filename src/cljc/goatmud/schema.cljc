(ns goatmud.schema)

(def username-schema
  [:string {:min 8 :max 50}])

(def password-schema
  [:string {:min 8 :max 50}])

(def confirm-password-schema
  [:string {:min 1}])

(def email-schema
  [:re #"[^@\s]+@[^@\s]+\.[^@\s]+" ])
