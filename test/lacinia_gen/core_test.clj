(ns lacinia-gen.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [lacinia-gen.core :as lgen]
            [clojure.test.check.generators :as g]))

(defn- position? [position]
  (contains? #{:goalkeeper :defence :attack} position))

(defn- player? [player]
  (and (string? (:name player))
       (integer? (:age player))
       (position? (:position player))))

(defn team? [team]
  (and (integer? (:wins team))
       (integer? (:losses team))
       (every? player? (:players team))))

(deftest basic-test
  (let [schema {:enums {:position {:values [:goalkeeper :defence :attack]}}
                :objects {:team {:fields {:wins {:type 'Int}
                                          :losses {:type 'Int}
                                          :players {:type '(list :player)}}}
                          :player {:fields {:name {:type 'String}
                                            :age {:type 'Int}
                                            :position {:type :position}}}}}]

    (let [gen (lgen/generator schema)]

      (testing "can generate enums"
        (let [v (g/sample (gen :position) 10)]
          (is (every? position? v))))

      (testing "can generate objects"
        (let [v (g/sample (gen :player) 10)]
          (is (every? player? v))))

      (testing "can generate composite objects"
        (let [v (g/sample (gen :team) 10)]
          (is (every? team? v)))))))

(deftest recursion-test
  (let [schema {:objects {:team {:fields {:name {:type 'String}
                                          :players {:type '(list :player)}}}
                          :player {:fields {:name {:type 'String}
                                            :team {:type :team}}}}}]

    (testing "defaults to one level"
      (let [gen (lgen/generator schema)]

        (let [v (g/sample (gen :team) 10)]
          (is (every? (fn [team]
                        (and (:players team)
                             (or (empty? (:players team))
                                 (:team (first (:players team))))
                             ;; team has players and those players have a team, but no deeper
                             (not (-> team :players first :team :players))))
                      v)))

        (let [v (g/sample (gen :player) 10)]
          (is (every? (fn [player]
                        (and (:team player)
                             (or (empty? (:players (:team player)))
                                 (:players (:team player)))
                             (not (-> player :team :players first :team))))
                      v)))))

    (testing "can override recursion depth"

      (testing "no recursion"
        (let [gen (lgen/generator schema {:depth {:team 0}})]

          (let [v (g/sample (gen :team) 10)]
            (is (every? (fn [team]
                          (and (:players team)
                               (or (empty? (:players team))
                                   (not (:team (first (:players team)))))))
                        v)))))

      (testing "more recursion"
        (let [gen (lgen/generator schema {:depth {:team 2
                                                  :players 2}})]

          (let [v (last (g/sample (gen :team) 10))]
            (is (-> v :players first :team :players first :team))))))))
