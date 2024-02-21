# convention 4 kubernetes: c4k-common
[![Clojars Project](https://img.shields.io/clojars/v/org.domaindrivenarchitecture/c4k-common-clj.svg)](https://clojars.org/org.domaindrivenarchitecture/c4k-common-clj) [![Clojars Project](https://img.shields.io/clojars/v/org.domaindrivenarchitecture/c4k-common-cljs.svg)](https://clojars.org/org.domaindrivenarchitecture/c4k-common-cljs) [![pipeline status](https://gitlab.com/domaindrivenarchitecture/c4k-common/badges/master/pipeline.svg)](https://gitlab.com/domaindrivenarchitecture/c4k-common/-/commits/master) 

[<img src="https://domaindrivenarchitecture.org/img/delta-chat.svg" width=20 alt="DeltaChat"> chat over e-mail](mailto:buero@meissa-gmbh.de?subject=community-chat) | [<img src="https://meissa-gmbh.de/img/community/Mastodon_Logotype.svg" width=20 alt="team@social.meissa-gmbh.de"> team@social.meissa-gmbh.de](https://social.meissa-gmbh.de/@team) | [Website & Blog](https://domaindrivenarchitecture.org)

## Rationale

There are many comparable solutions for creating c4k deployments like `helm` or `kustomize`. 
`kustomize` is great to manage your k8s manifests by splitting huge files into handy parts.
`helm` is great because of its large community.

Why do we need another one? Why do you continue the reading here?

We combine the simplicity of `kustomize` with the ability for doing real programming as software developers would do.

Following the principle

    "Use programming language for programming" 

we are clearly enjoy writing kubernetes manifests with clojure. In comparison with helms templating, things such as business logic, conventions, input validation, versions, dependencies and reuse are much easier and much more reliable to implement.

By the way, c4k means "convention for kubernetes".

### Features

c4k-common supports the following use cases:

- [convention 4 kubernetes: c4k-common](#convention-4-kubernetes-c4k-common)
  - [Rationale](#rationale)
    - [Features](#features)
      - [Target Cli and Web Frontend](#target-cli-and-web-frontend)
      - [Separate Configuration from Credentials](#separate-configuration-from-credentials)
      - [Input as EDN or Yaml](#input-as-edn-or-yaml)
      - [Inline k8s resources for versioning \& dependencies](#inline-k8s-resources-for-versioning--dependencies)
      - [Work on structured Data instead flat Templating](#work-on-structured-data-instead-flat-templating)
      - [Validate your inputs](#validate-your-inputs)
      - [Namespaces](#namespaces)
      - [Ingress](#ingress)
      - [Postgres Database](#postgres-database)
      - [Monitoring with Grafana Cloud](#monitoring-with-grafana-cloud)
  - [Refactoring \& Module Overview](#refactoring--module-overview)
  - [Development \& mirrors](#development--mirrors)
  - [License](#license)

#### Target Cli and Web Frontend

Set up your cli as follows

```clojure
(defn -main [& cmd-args]
  (uberjar/main-common 
   "c4k-forgejo"              ;; name of your app
   core/config?               ;; schema for config validation
   core/auth?                 ;; schema for credential validation
   core/config-defaults       ;; want to set default values?
   core/k8s-objects           ;; the function generate the k8s manifest
   cmd-args                   ;; command line arguments given
   ))                 
```

The full example can be found here: https://repo.prod.meissa.de/meissa/c4k-forgejo/src/branch/main/src/main/clj/dda/c4k_forgejo/uberjar.clj


You can create your manifest as web-application also (using page local js without server interaction)

```html
html>

<head>
  <meta charset="utf-8" />
  <title>c4k-forgejo</title>
  <link href="https://domaindrivenarchitecture.org/css/bootstrap.min.css" rel="stylesheet" type="text/css" />
  <link href="https://domaindrivenarchitecture.org/css/fonts/fontawesome/fontawesome.css" rel="stylesheet"
    type="text/css" />
  <link href="https://domaindrivenarchitecture.org/css/custom.css" rel="stylesheet" type="text/css" />
</head>

<body>
  <div id="c4k-content"></div>
  <script src="js/main.js"></script>
</body>

</html>
```

[![Try it out](doc/tryItOut.png "Try out yourself")](https://domaindrivenarchitecture.org/pages/dda-provision/c4k-forgejo/)

See: https://repo.prod.meissa.de/meissa/c4k-forgejo/src/branch/main/public/index.html

and: https://repo.prod.meissa.de/meissa/c4k-forgejo/src/branch/main/src/main/cljs/dda/c4k_forgejo/browser.cljs

#### Separate Configuration from Credentials

We think it is an good idea to have credentials separated from configuration. All our functions, cli and frontend are following this principle.

```bash
c4k-common config.edn auth.edn > k8s-manifest.yaml
```

#### Input as EDN or Yaml

c4k-common supports all its resources, input and output as yaml and as edn.
The following command line will work also:

```bash
c4k-common config.yaml auth.yaml > k8s-manifest.yaml
```

#### Inline k8s resources for versioning & dependencies

We inline all resources used in our libraries & applications. You can generate k8s manifests everywhere without additional external dependencies.

In case of
* java: Resources are included in the jar-file out of the box (see https://repo.prod.meissa.de/meissa/c4k-forgejo/src/branch/main/project.clj#L13).
* js: With a slim macro call we inline resources to the resulting js file (see https://repo.prod.meissa.de/meissa/c4k-forgejo/src/branch/main/src/main/cljc/dda/c4k_forgejo/forgejo.cljc#L72-L74)
* native: On native builds we inline resources also (see https://repo.prod.meissa.de/meissa/c4k-forgejo/src/branch/main/build.py#L126)

#### Work on structured Data instead flat Templating

To keep things simple, we do also templating. But we convert given k8s resources to structured data.
This allows us to have more control and do unit tests:

k8s-resource:

```yaml
apiVersion: traefik.containo.us/v1alpha1
kind: Middleware
metadata:
  name: ratelimit
spec:
  rateLimit:
    average: AVG
    burst: BRS
```

Replace values:

```clojure
(defn-spec generate-rate-limit-middleware pred/map-or-seq?
  [config rate-limit-config?]
  (let [{:keys [max-rate max-concurrent-requests]} config]
  (->
   (yaml/load-as-edn "forgejo/middleware-ratelimit.yaml")
   (cm/replace-key-value :average max-rate)
   (cm/replace-key-value :burst max-concurrent-requests))))                
```

Have a unit-test:

```clojure
(deftest should-generate-middleware-ratelimit
  (is (= {:apiVersion "traefik.containo.us/v1alpha1",
          :kind "Middleware",
          :metadata {:name "ratelimit"},
          :spec {:rateLimit {:average 10, :burst 5}}}
         (cut/generate-rate-limit-middleware {:max-rate 10, :max-concurrent-requests 5}))))
```

#### Validate your inputs

Have you recognized the `defn-spec` marco? We use allover validation, e.g.

```clojure
(def rate-limit-config? (s/keys :req-un [::max-rate
                                         ::max-concurrent-requests]))

(defn-spec generate-rate-limit-middleware pred/map-or-seq?
    [config rate-limit-config?]
    ...)
```

#### Namespaces

We support namespaces for ingress & postgres (monitoring lives in it's own namespace `monitoring`).

```clojure
(deftest should-generate-simple-ingress
  (is (= [{:apiVersion "v1"
           :kind "Namespace"
           :metadata {:name "myapp"}}]
         (cut/generate {:namespace "myapp"}))))
```

#### Ingress

In most cases we use 'generate-ingress-and-cert' which generates an ingres in combination with letsencrypt cert for a named service.

```clojure
(deftest should-generate-ingress-and-cert
  (is (= [{:apiVersion "cert-manager.io/v1",
          ...}
          {:apiVersion "networking.k8s.io/v1",
           :kind "Ingress",
           ...
           :spec
           {:tls [{:hosts ["test.jit.si"], :secretName "web"}],
            :rules
            [{:host "test.jit.si",
              :http {:paths [{:path "/",
                              :pathType "Prefix",
                              :backend
                              {:service {:name "web",
                                         :port {:number 80}}}}]}}]}}]
         (cut/generate-ingress-and-cert {:fqdns ["test.jit.si"]
                                          :service-name "web"
                                          :service-port 80}))))
```

#### Postgres Database

If your application needs a database, we often use postgres:

```clojure
(deftest should-generate-deployment
  (is (= [{:image "postgres:16"
           :name "postgresql"
           :env
           [{:name "POSTGRES_USER" ...}
            {:name "POSTGRES_PASSWORD" ...}
            {:name "POSTGRES_DB" ...}]
           :volumeMounts [{:name "postgre-data-volume" ...}]}]
         (get-in (cut/generate-deployment 
                    {:postgres-image "postgres:16"})
                    [:spec :template :spec :containers]))))
```

We optimized our db installation to run between 2Gb anf 16Gb Ram usage.

#### Monitoring with Grafana Cloud

With minimal config of

```clojure
(def conf 
    {:k3s-cluster-name "your-cluster-name"
     :k3s-cluster-stage :prod
     :grafana-cloud-url "your-url"})

(def auth 
    {:grafana-cloud-user "user"
     :grafana-cloud-password "password"})

 (monitoring/generate conf auth)
```

You can attach your application to grafana cloud.

## Refactoring & Module Overview

| Module        | Version | [common load-as-edn][edn1] | [groups for webview][bgrp1] | [use common ingress][ing1] | [use common monitoring][mon1] | [validate examples][val1] | [ci with pyb][cipyb] | [inline-macro to load resources][macro] | [native build][native] | namespaces |
| ------------- |---------| :------------------------: | :-------------------------: | :------------------------: | :---------------------------: | :-----------------------: |:--------------------:|:---------------------------------------:|:----------------------:|:----------:|
| c4k-keycloak  | 0.2     |             x              |              x              |             x              |               x               |             x             |                      |                                         |                        |            |
| c4k-taiga     | 0.1     |                            |                             |                            |                               |                           |                      |                                         |                        |            |
| c4k-nextcloud | 4.0     |             x              |              x              |             x              |               x               |             x             |                      |                                         |                        |            |
| c4k-jitsi     | 1.6     |             x              |              x              |             x              |               x               |             x             |          x           |                    x                    |                        |            |
| c4k-forgejo   | 3.0     |             x              |              x              |             x              |               x               |             x             |          x           |                    x                    |           x            |            |
| c4k-shynet    | 1.0     |                            |                             |                            |                               |                           |                      |                                         |                        |            |
| c4k-website   | 1.1     |             x              |              x              |             x              |               x               |             x             |          x           |                    x                    |           x            |            |

[edn1]: https://gitlab.com/domaindrivenarchitecture/c4k-website/-/merge_requests/1
[ing1]:  https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/214aa41c28662fbf7a49998e17404e7ac9216430
[bgrp1]: https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/7ea442adaef727d5b48b242fd0baaaf51902d06e
[mon1]:  https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/19e580188ea56ea26ff3a0bfb08ca428b881ad9a
[val1]:  https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/5f08a108072569473463fb8f19150a12e564e54f
[repo1]: https://repo.prod.meissa.de/meissa/c4k-forgejo/commit/e9ee6136f3347d5fccefa6b5b4a02d30c4dc42e1
[cipyb]: https://gitlab.com/domaindrivenarchitecture/c4k-jitsi/-/merge_requests/1
[macro]: https://repo.prod.meissa.de/meissa/c4k-jitsi/commit/61d05ceedb6dcbc6bb96b96fe6f03598e2878195
[native]: https://repo.prod.meissa.de/meissa/c4k-forgejo/pulls/4/files

## Development & mirrors

Development happens at: https://repo.prod.meissa.de/meissa/c4k-common

Mirrors are:

* https://gitlab.com/domaindrivenarchitecture/c4k-common (issues and PR)
* https://github.com/DomainDrivenArchitecture/c4k-common

For more details about our repository model see: https://repo.prod.meissa.de/meissa/federate-your-repos

## License

Copyright © 2022, 2023, 2024 meissa GmbH
Licensed under the [Apache License, Version 2.0](LICENSE) (the "License")
Pls. find licenses of our subcomponents [here](doc/SUBCOMPONENT_LICENSE)
