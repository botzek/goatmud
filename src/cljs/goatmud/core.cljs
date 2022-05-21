(ns goatmud.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [re-frame.core :as rf]
      [goatmud.websocket :as ws]))

(rf/reg-event-fx
 :app/initialize
 (fn [_ _]
   (let [socket (ws/make-socket!)
         router (ws/make-router! socket)]
     {:db {:ws/socket socket
           :ws/router router}})))

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
   [:h2 "Welcome to Reagent!"]
   [message-form]])

;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root []
  (d/render [home-page] (.getElementById js/document "app")))

(defn ^:export ^:dev/once init! []
  (rf/dispatch [:app/initialize])
  (mount-root))
