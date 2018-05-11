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
      (println (is (->> data :teams (keep (comp not-empty :players)))))
      (is (->> data :teams (keep (comp not-empty :players)) first count pos?))
      (is (->> data :teams (keep (comp not-empty :players)) ffirst :name string?)))))
