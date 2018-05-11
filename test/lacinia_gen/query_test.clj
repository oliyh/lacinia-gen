(ns lacinia-gen.query-test
  (:require [lacinia-gen.query :as query]
            [clojure.test :refer [deftest testing is]]))

(def schema '{:enums {:position {:values [:goalkeeper :defence :attack]}}
              :objects {:team {:fields {:wins {:type Int}
                                        :losses {:type Int}
                                        :players {:type (list :player)}}}
                        :player {:fields {:name {:type String}
                                          :age {:type Int}
                                          :position {:type :position}}}}
              :queries {:teams {:type (list :team)
                                :resolve :resolve-teams}}})

(deftest generate-fn-test
  (let [f (query/generate-fn schema)]

    (let [data (:data (f "query { teams { wins players { name } } }" {}))]
      (is (:teams data))
      (is (-> data :teams count pos?))
      (is (-> data :teams first :wins int?))
      (is (->> data :teams (keep (comp not-empty :players)) first count pos?))
      (is (->> data :teams (keep (comp not-empty :players)) ffirst :name string?)))))

(def literal-macro-data
  (query/generate-query {:objects {:team {:fields {:wins {:type Int}}}}
                         :queries {:teams {:type (list :team)
                                           :resolve :resolve-teams}}}
                        "query { teams { wins } }"
                        {}))

(deftest generate-query-test
  (is literal-macro-data)
  (let [data (:data literal-macro-data)]
      (is (:teams data))
      (is (-> data :teams count pos?))
      (is (-> data :teams first :wins int?))))

(def query "query { teams { wins players { name } } }")

(def resolving-macro-data
  (query/generate-query* schema query {}))

(deftest generate-query*-test
  (is resolving-macro-data)
  (let [data (:data resolving-macro-data)]
      (is (:teams data))
      (is (-> data :teams count pos?))
      (is (-> data :teams first :wins int?))))
