(ns dda.c4k-common.prefixes)

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