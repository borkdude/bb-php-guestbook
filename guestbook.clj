(ns guestbook
  (:require [cheshire.core :as cheshire]
            [hiccup2.core :as hiccup]
            [clojure.string :as str]))

(require '[babashka.pods :as pods])
(pods/load-pod "./pod-babashka-postgresql")

(def db {:dbtype "postgresql"
         :user "guestbook"
         :password "guestbook"
         :database "guestbook"
         :port 5434})

(require '[pod.babashka.postgresql :as sql])

(def post-data (-> (System/getenv "POST_DATA")
                   (cheshire/parse-string
                    true)))

(def session-id (System/getenv "SESSION_ID"))

(def posted-before (-> (sql/execute-one! db ["select count(*) from guestbook where session = ?" session-id])
                       :count))

(defn process-post-data []
  (cond (and posted-before
             (pos? posted-before))
        {:status :success
         :html [:div.alert.alert-info "You greeted before, thank you!"]}
        (not-empty post-data)
        (let [{:keys [name message op n1 n2 sum session_id]} post-data
              _ (when (not= session-id session_id)
                  (System/exit 0))
              op (case op "+" + "*" *)
              n1 (parse-long n1)
              n2 (parse-long n2)
              sum (parse-long sum)
              expected-sum (op n1 n2)]
          (if (not= expected-sum sum)
            {:html [:div.alert.alert-danger "Sum incorrect!"]
             :status :error}
            (when (and name (not (str/blank? name))
                       message (not (str/blank? message)))
              (try (let [res (sql/execute-one! db ["insert into guestbook (name, message, _created, session) values (?,?, now(), ?)
                                                    on conflict do nothing"
                                                   name message session_id])
                         updated (-> res :next.jdbc/update-count)]
                     {:html (if (pos? updated)
                              [:div.alert.alert-info "Your message was saved!"]
                              [:div.alert.alert-info "You greeted before, thank you!"])
                      :status :success})
                   (catch Exception e
                     (prn e))))))))

(defn entries [] (sql/execute! db ["select * from guestbook order by _created desc limit 10"]))

(defn render-messages []
  [:table.table
   [:thead
    [:tr
     [:th "Name"]
     [:th "Greeting"]]]
   [:tbody
    (for [{:guestbook/keys [name message]} (entries)]
      [:tr
       [:td name]
       [:td message]])]])

(defn render-query-params []
  (hiccup/html {:escape-strings? false}
               (cheshire/parse-string (System/getenv "QUERY_PARAMS")
                                      true)))

(defn render-form []
  (let [n1 (str (rand-int 100))
        n2 (str (rand-int 100))
        op (rand-nth ["+" "*"])]
    [:div.mb-3
     [:form {:method "POST"}
      [:label.form-label {:for "name"} "Name:"]
      [:input.form-control {:type "text"
                            :name "name"
                            :id "name"}]
      [:label.form-label {:for "message"} "Message:"]
      [:input.form-control {:type "text"
                            :name "message"
                            :id "message"}]
      [:input {:type "hidden"
               :name "session_id"
               :id "session_id"
               :value session-id}]
      [:input {:type "hidden"
               :name "op"
               :id "op"
               :value op}]
      [:input {:type "hidden"
               :name "n1"
               :id "n1"
               :value n1}]
      [:input {:type "hidden"
               :name "n2"
               :id "n2"
               :value n2}]
      [:label.form-label {:for "message"} "The outcome of " [:code (format "%s %s %s" n1 op n2)] ":"]
      [:input.form-control {:type "text"
                            :name "sum"
                            :id "sum"}]
      [:input.btn.btn-primary
       {:type "submit"
        :value "Submit"}]]]))

(println
 (try (str "<!doctype html>"
           (hiccup/html
            [:html {:lang "en"}
             [:head
              [:link {:rel "stylesheet"
                      :href "https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css"
                      :integrity "sha384-1BmE4kWBq78iYhFldvKuhfTAU6auU8tT94WrHftjDbrCEXSU1oBoqyl2QvZ6jIW3"
                      :crossorigin "anonymous"}]]
             [:body.container
              [:div "Welcome to my guestbook!"]
              (let [{:keys [status html]} (process-post-data)]
                [:div
                 html
                 (when (or (not= :success status)
                           (not posted-before))
                   [:div (render-form)])])
              [:div "Last 10 entries:"
               (render-messages)]]]))
      (catch Exception _e "ERROR" #_(str e))))
