(ns dda.c4k-common.predicate)

(defn bash-env-string?
  [input]
  (and (string? input)
       (not (re-matches #".*['\"\$]+.*" input))))

(defn fqdn-string?
  [input]
  (and (string? input)
       (some? (re-matches #"(?=^.{4,253}$)(^((?!-)[a-zA-Z0-9-]{0,62}[a-zA-Z0-9]\.)+[a-zA-Z]{2,63}$)" input))))

(defn letsencrypt-issuer?
  [input]
  (contains? #{:prod :staging} input))

(defn map-or-seq?
  [input]
  (or (map? input) (seq? input)))

(defn pvc-storage-class-name?
  [input]
  (contains? #{:manual :local-path} input))