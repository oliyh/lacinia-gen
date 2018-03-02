(ns lacinia-gen.core
  (:require [clojure.test.check.generators :as gen]))

(defn- primitive [type]
  (get {'String gen/string
        'Float gen/double
        'Int gen/int
        'Boolean gen/boolean}
       type))

(defn- enum [values]
  (let [g (gen/elements values)]
    (constantly g)))

(defn- field [depth all-gens type]
  (cond
    ;; sub object
    (and (keyword? type)
         (contains? all-gens type))
    ((get all-gens type) depth all-gens)

    ;; list of type
    (and (list? type)
         (= 'list (first type)))
    (gen/list (field depth all-gens (second type)))

    ;; non-nullable
    (and (list? type)
         (= 'non-null (first type)))
    (gen/such-that (complement nil?) (field depth all-gens (second type)))

    ;; primitive
    :else
    (primitive type)))

(defn- object [fields]
  (fn [depth all-gens]
    (let [fields (keep (fn [[k {:keys [type]}]]
                         (when (pos? (get depth k 1))
                           (gen/fmap (fn [v]
                                       {k v})
                                     (field (update depth k (fnil dec 1)) all-gens type))))
                       fields)]
      (gen/fmap
       (fn [kvs]
         (apply merge kvs))
       (apply gen/tuple fields)))))

(defn- make-gens [enums objects]
  (merge (reduce-kv (fn [all-gens k {:keys [fields]}]
                      (assoc all-gens k (object fields)))
                    {}
                    objects)
         (reduce-kv (fn [all-gens k {:keys [values]}]
                      (assoc all-gens k (enum values)))
                    {}
                    enums)))

(defn generator
  "Given a lacinia schema returns a function which takes an object key as an argument
  and returns a generator for that object, e.g.

  (let [g (generator my-lacinia-schema)]
    (gen/sample (g :my-object) 10))

  Options:
   - depth
     A map of object keys to the depth they should recurse to in cyclical graphs
     e.g. {:parent 1
           :child 2}"
  [schema & [opts]]
  (let [{:keys [depth]
         :or {depth {}}} opts
        all-gens (make-gens (:enums schema) (:objects schema))]
    (fn [type]
      ((get all-gens type) depth all-gens))))
