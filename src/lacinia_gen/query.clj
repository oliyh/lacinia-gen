(ns lacinia-gen.query
  (:require [lacinia-gen.core :as lgen]
            [lacinia-gen.def :as def]
            [com.walmartlabs.lacinia :as lacinia]
            [com.walmartlabs.lacinia.schema :as schema]
            [com.walmartlabs.lacinia.util :refer [attach-resolvers attach-streamers]]
            [clojure.test.check.generators :as g]
            [clojure.string :as string]
            [cljs.analyzer :as cljs]))

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
        {:keys [field field-definition leaf?]} (::lacinia/selection context)
        many? (= :list (get-in field-definition [:type :kind]))]
    (cond
      leaf? (get parent field)
      many? (g/sample (generator (get-in field-definition [:type :type :type])) 5)
      :else (g/generate (generator field)))))

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

(defn generate-fn
  "Call with an (uncompiled) lacinia schema to return a function which
  can generate data for an arbitrary query

  Opts are as per lacinia-gen.core/generator

  e.g. (let [f (generate-fn {:objects {:team ... }})]
         (f \"query { teams { teamName } }\" {}))"
  [schema opts]
  (let [s (compile-generating-schema schema)
        gen (lgen/generator schema opts)]
    (fn [query variables]
      (lacinia/execute s query variables {:gen gen}))))

(defmacro generate-query
  "For use with literal schema, query and variables arguments

  Opts are as per lacinia-gen.core/generator

  e.g. (generate-query {:objects {:team ... }}
                       \"query { teams { teamName } }\"
                       {}
                       {})"
  [schema query variables opts]
  `'~((generate-fn schema opts)
      (->> (string/replace query #"^query|subscription" "")
           (str "query "))
      variables))

(defn- maybe-resolve [cljs-env s]
  (if (symbol? s)
    (if-let [v (resolve s)]
      @v
      (when cljs-env
        (let [result (-> (cljs/resolve-var cljs-env s (cljs/confirm-var-exists-throw))
                         :meta
                         ::def/source)]
          (if (and (list? result) (= 'quote (first result)))
            (second result)
            result))))
    s))

(defmacro generate-query*
  "For use from Clojurescript where arguments are symbols that need resolving.

   N.B. you must use defquery to define queries in Clojurescript as the value
   cannot be resolved from a normal var.

  Opts are as per lacinia-gen.core/generator

  e.g.
  (require '[my.project.graphql :refer [schema]])
  (defquery team-query \"query { teams { teamName } }\")

  (generate-query* schema team-query {} {})"
  [schema query variables opts]
  (let [cljs-env &env
        maybe-resolve (partial maybe-resolve cljs-env)]
    `(generate-query ~(maybe-resolve schema)
                     ~(maybe-resolve query)
                     ~(maybe-resolve variables)
                     ~(maybe-resolve opts))))
