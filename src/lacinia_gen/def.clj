(ns lacinia-gen.def)

(defmacro defquery [name query]
  `(def ~(vary-meta name assoc ::value query) ~query))

(defmacro defschema [name schema]
  `(def ~(vary-meta name assoc ::value schema) ~schema))
