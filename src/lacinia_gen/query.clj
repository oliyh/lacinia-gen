(ns lacinia-gen.query
  (:require [lacinia-gen.core :as lgen]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers attach-streamers]]
            [clojure.test.check.generators :as g]
            [clojure.string :as stringB]))

(defn- resolver-keys [objects]
  (->> objects
       vals
       (map :fields)
       (mapcat vals)
       (keep :resolve)
       set))

(defn- root-resolver-keys [root]
  (->> root vals (keep :resolve)))

(defn- generating-resolver [context arguments parent]
  (let [generator (:gen context)
        {:keys [field field-definition]} (::lacinia/selection context)
        many? (= :list (get-in field-definition [:type :kind]))]
    (if many?
      (g/sample (generator (get-in field-definition [:type :type :type])) 5)
      (g/generate (generator :field)))))

(defn- resolver-map [{:keys [objects queries subscriptions]}]
  (let [resolver-keys (into (resolver-keys objects)
                            (mapcat root-resolver-keys [queries subscriptions]))]
    (not-empty (zipmap resolver-keys (repeat generating-resolver)))))

(defn- streamer-map [schema]
  (let [streamer-keys (->> schema :subscriptions vals (keep :stream))]
    (not-empty (zipmap streamer-keys (repeat (fn [& args]))))))

(defn- compile-generating-schema [schema]
  (let [resolvers (resolver-map schema)
        streamers (streamer-map schema)]
    (cond-> schema
      resolvers (attach-resolvers resolvers)
      streamers (attach-streamers streamers)
      :always schema/compile)))

(defn generate-fn [schema]
  (let [s (compile-generating-schema schema)
        gen (lgen/generator schema)]
    (fn [query variables]
      (lacinia/execute s query variables {:gen gen}))))

(defmacro generate-query
  "For use with literal schema, query and variables arguments

  e.g. (generate-query {:objects {:team ... }}
                       \"query { teams { teamName } }\"
                       {})"
  [schema query variables]
  `'~((generate-fn schema)
      (->> (string/replace query #"^query|subscription" "")
           (str "query "))
      variables))

(defn- maybe-resolve [s]
  (if (symbol? s) @(resolve s) s))

(defmacro generate-query*
  "For use from Clojurescript where arguments are symbols that need resolving.

   N.B. you must use defquery to define queries in Clojurescript as the value
   cannot be resolved from a normal var.

  e.g.
  (require '[my.project.graphql :refer [schema]])
  (defquery team-query \"query { teams { teamName } }\")

  (generate-query* schema team-query {})"
  [schema query variables]
  `(generate-query ~(maybe-resolve schema)
                   ~(maybe-resolve query)
                   ~(maybe-resolve variables)))
