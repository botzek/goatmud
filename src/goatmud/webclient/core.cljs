(ns goatmud.webclient.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [re-frame.core :as rf]
      [goatmud.webclient.websocket :as ws]
      [goatmud.webclient.ajax :as ajax]
      [goatmud.webclient.account :as account]))

(rf/reg-event-fx
 :app/initialize
 (fn [_ _]
   {:dispatch [:ws/initialize]}))

(rf/reg-event-fx
 :message/send!
 (fn [{:keys [db]} [_ message]]
   {:ws/send! {:socket (:ws/socket db)
               :message [:click/button message]}}))

(defn message-form []
  (let [msg (r/atom nil)]
    [:div
     [:input {:type :text
              :name "message"
              :on-change #(reset! msg (.. % -target -value))}]
     [:input {:type :button
              :value "Send!"
              :on-click #(rf/dispatch [:message/send! @msg])}]]))

;; -------------------------
;; Views

(defn home-page []
  [:div
   [ws/connecting-dialog]
   [account/create-account-dialog]
   #_[message-form]])

;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export ^:dev/once init! []
  (rf/dispatch [:app/initialize])
  (mount-root))
