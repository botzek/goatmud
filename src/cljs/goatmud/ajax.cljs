(ns goatmud.ajax
  (:require [ajax.core :refer [GET POST]]
            [re-frame.core :as rf]))

(rf/reg-fx
 :ajax/get
 (fn [{:keys [url success-event error-event success-handler error-handler]}]
   (GET url
        (cond-> {:headers {"Accept" "application/transit+json"}}
          success-handler (assoc :handler success-handler)
          error-handler (assoc :error-handler error-handler)
          success-event (assoc :handler
                               #(rf/dispatch (conj success-event %)))
          error-event (assoc :error-handler
                             #(rf/dispatch (conj error-event %)))))))

(rf/reg-fx
 :ajax/post
 (fn [{:keys [url params success-event error-event success-handler error-handler]}]
   (POST url
         (cond-> {:headers {"Accept" "application/transit+json"}}
           params (assoc :params params)
           success-handler (assoc :handler success-handler)
           error-handler (assoc :error-handler error-handler)
           success-event (assoc :handler
                                #(rf/dispatch (conj success-event %)))
           error-event (assoc :error-handler
                              #(rf/dispatch (conj error-event %)))))))
