(ns goatmud.webserver.routes.websocket
  (:require
   [clojure.tools.logging :as log]
   [goatmud.webserver.middleware.exception :as exception]
   [goatmud.webserver.middleware.formats :as formats]
   [ring.util.http-response :as http-response]
   [integrant.core :as ig]
   [clojure.core.async :as async  :refer (<! <!! >! >!! put! chan go go-loop alt!)]
   [reitit.coercion.malli :as malli]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.undertow :refer [get-sch-adapter]]
   [reitit.swagger :as swagger]))

(defn ws-routes [{:keys [socket] :as _opts}]
  [["" {:get (:ajax-get-or-ws-handshake-fn socket)
        :post (:ajax-post-fn socket)}]])

(defn route-data
  [opts]
  (merge
    opts {}))

(derive :reitit.routes/ws :reitit/routes)

(defmethod ig/init-key :reitit.routes/ws
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  [base-path (route-data opts) (ws-routes opts)])

(defmethod ig/init-key :ws/socket
  [_ _opts]
  (sente/make-channel-socket!
   (get-sch-adapter)
   {:user-id-fn (fn [ring-req]
                  (get-in ring-req [:params :client-id]))
    :csrf-token-fn nil}))

(defmulti handle-message
  (fn [{:keys [id]} _]
    id))

(defmethod handle-message :chsk/uidport-open
  [{:keys [event]} _]
  (log/info "Connection established: " (pr-str event)))

(defmethod handle-message :chsk/uidport-close
  [{:keys [event]} _]
  (log/info "Disconneted: " (pr-str event)))

(defmethod handle-message :default
  [_ event]
  (log/error "Unhandled event: " (pr-str event)))

(defn receive-message!
  [{:keys [event] :as message}]
  (handle-message message event))

(defmethod ig/init-key :ws/router
  [_ {:keys [socket]}]
  (sente/start-chsk-router!
   (:ch-recv socket)
   #'receive-message!))

(defmethod ig/halt-key! :ws/router
  [_ stop-fn]
  (when stop-fn
    (stop-fn)))

(defmethod ig/init-key :ws/ping
  [_ {:keys [socket]}]
  (let [stop-ch (chan)]
    (go-loop []
      (let [result (alt!
                     (async/timeout 10000) :continue
                     stop-ch :stop)]
        (when (= :continue result)
          (let [connected-uids (:connected-uids socket)
                send-fn (:send-fn socket)]
            (doseq [uid (:ws @connected-uids)]
              (send-fn
               uid
               [:ws/pingging "sup"]))
            (recur)))))
    (fn [] (>!! stop-ch true))))

(defmethod ig/halt-key! :ws/ping
  [_ stop-fn]
  (when stop-fn
    (stop-fn)))
