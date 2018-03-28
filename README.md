# lacinia-gen

`lacinia-gen` lets you generate graphs of data using your [lacinia](https://github.com/walmartlabs/lacinia) schema,
so you can make your tests more rigorous.

## Usage

```clojure
(require '[lacinia-gen.core :as lgen])
(require '[clojure.test.check.generators :as g])

(let [schema {:enums {:position {:values [:goalkeeper :defence :attack]}}
              :objects {:team {:fields {:wins {:type 'Int}
                                        :losses {:type 'Int}
                                        :players {:type '(list :player)}}}
                        :player {:fields {:name {:type 'String}
                                          :age {:type 'Int}
                                          :position {:type :position}}}}}]

  (let [gen (lgen/generator schema)]

    (g/sample (gen :position) 5)
    ;; => (:defence :goalkeeper :defence :goalkeeper :goalkeeper)

    (g/sample (gen :player) 5)
    ;; => ({:name "", :age 0, :position :attack}
           {:name "", :age 0, :position :attack}
           {:name "", :age 1, :position :attack}
           {:name "m", :age -2, :position :defence}
           {:name "¤", :age 4, :position :defence})

    (g/sample (gen :team) 5)
    ;; => ({:wins 0, :losses 0, :players ()}
           {:wins 1,
            :losses 1,
            :players ({:name "", :age -1, :position :defence})}
           {:wins 1,
            :losses 2,
            :players
            ({:name "", :age 1, :position :attack}
             {:name "q", :age -2, :position :attack})}
           {:wins -3, :losses 1, :players ()}
           {:wins 0,
            :losses -3,
            :players
            ({:name "ßéÅ", :age 3, :position :attack}
             {:name "", :age -4, :position :defence})})))
```

It supports recursive graphs, so the following works too:

```clojure
(let [schema {:objects {:team {:fields {:name {:type 'String}
                                        :players {:type '(list :player)}}}
                          :player {:fields {:name {:type 'String}
                                            :team {:type :team}}}}}]

    (let [gen (lgen/generator schema)]
      (g/sample (gen :team) 5)))

;; => {:name "ö",
       :players
       ({:name "³2",
         :team
         {:name "ºN",
          :players
          ({:name "Ïâ¦", :team {:name "¤¼J"}}
           {:name "o", :team {:name "æ8"}}
           {:name "/ç", :team {:name "ãL"}}
           {:name "éíª6", :team {:name "v"}})}}
```

If you want to limit the depth to which certain objects recurse, you can do so with the following option:

```clojure
(lgen/generator schema {:depth {:team 0}})
```

This will ensure that `:team` only appears once in the graph; the team will have players, but it prevents the players from having a team.
The default value for all objects is 1, meaning each will recur once (a team will have players which have a team which has players, but no further).
You can set any integer value you wish.

### Custom scalars

If your schema contains [custom scalars](http://lacinia.readthedocs.io/en/latest/custom-scalars.html) you will need to
provide generators for them. You can do so in the following way:

```clojure
(let [schema {:scalars {:Custom {:parse :parse-custom
                                 :serialize :serialize-custom}}
                :objects {:obj-with-custom
                          {:fields
                           {:custom {:type :Custom}
                            :custom-list {:type '(list :Custom)}}}}}]

  (let [gen (lgen/generator schema {:scalars {:Custom gen/int}})]
    (g/sample (gen :obj-with-custom) 10)))

;; => {:custom 23
       :custom-list (-1 4 16)}
```

## License

Copyright © 2018 oliyh

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
